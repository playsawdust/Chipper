/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.rcl;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.jar.Manifest;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.playsawdust.chipper.exception.ForbiddenClassError;

import static org.objectweb.asm.Opcodes.*;

/**
 * A class loader that patches classes to throw exceptions when attempting to
 * use "forbidden" classes and logs informational messages when loading
 * "discouraged" classes. Effectively a runtime equivalent to Eclipse's access
 * rules.
 * <p>
 * <b><i>This class is not intended for use as a security measure!</i></b> It is
 * meant to be informational only.
 */
public class RuledClassLoader extends URLClassLoader {
	private enum ClassRuleType {
		DISCOURAGED,
		FORBIDDEN,
		PERMITTED,
		;
	}

	private static class ClassRule {
		public final ClassRuleType type;
		public final String reason;

		public ClassRule(ClassRuleType type, String reason) {
			this.type = type;
			this.reason = reason;
		}

		@Override
		public String toString() {
			return "ClassRule["+type+"]"+(reason.isEmpty() ? "" : " ("+reason+")");
		}
	}

	public static final class Builder {
		private final List<BiFunction<String, String, ClassRule>> rules = Lists.newArrayList();
		private final List<URL> sources = Lists.newArrayList();
		private final List<BiPredicate<String, String>> exemptions = Lists.newArrayList();
		private ClassLoader parent;

		private Builder() {}

		private static BiPredicate<String, String> packageMatches(String pkg) {
			return (c, m) -> c.startsWith(pkg);
		}

		private static BiPredicate<String, String> classMatches(String cls) {
			return (c, m) -> c.equals(cls);
		}

		private static BiPredicate<String, String> methodMatches(String cls, String method) {
			if (method.contains("(")) {
				return (c, m) -> m != null && c.equals(cls) && m.equals(method);
			} else {
				return (c, m) -> m != null && c.equals(cls) && m.startsWith(method+"(");
			}
		}

		private static BiFunction<String, String, ClassRule> predicateAsRule(ClassRuleType crt, BiPredicate<String, String> pred, String reason) {
			ClassRule cr = new ClassRule(crt, reason);
			return (cls, method) -> {
				if (pred.test(cls, method)) return cr;
				return null;
			};
		}

		public Builder addForbiddenPackage(String pkg, String reason) {
			rules.add(predicateAsRule(ClassRuleType.FORBIDDEN, packageMatches(pkg), reason));
			return this;
		}

		public Builder addForbiddenClass(String cls, String reason) {
			rules.add(predicateAsRule(ClassRuleType.FORBIDDEN, classMatches(cls), reason));
			return this;
		}

		public Builder addForbiddenMethod(String cls, String method, String reason) {
			rules.add(predicateAsRule(ClassRuleType.FORBIDDEN, methodMatches(cls, method), reason));
			return this;
		}

		public Builder addDiscouragedPackage(String pkg, String reason) {
			rules.add(predicateAsRule(ClassRuleType.DISCOURAGED, packageMatches(pkg), reason));
			return this;
		}

		public Builder addDiscouragedClass(String cls, String reason) {
			rules.add(predicateAsRule(ClassRuleType.DISCOURAGED, classMatches(cls), reason));
			return this;
		}

		public Builder addDiscouragedMethod(String cls, String method, String reason) {
			rules.add(predicateAsRule(ClassRuleType.DISCOURAGED, methodMatches(cls, method), reason));
			return this;
		}

		public Builder addPermittedPackage(String pkg) {
			rules.add(predicateAsRule(ClassRuleType.PERMITTED, packageMatches(pkg), ""));
			return this;
		}

		public Builder addPermittedClass(String cls) {
			rules.add(predicateAsRule(ClassRuleType.PERMITTED, classMatches(cls), ""));
			return this;
		}

		public Builder addPermittedMethod(String cls, String method) {
			rules.add(predicateAsRule(ClassRuleType.PERMITTED, methodMatches(cls, method), ""));
			return this;
		}

		public Builder addExemptPackage(String pkg) {
			exemptions.add(packageMatches(pkg));
			return this;
		}

		public Builder addExemptClass(String cls) {
			exemptions.add(classMatches(cls));
			return this;
		}

		public Builder addSource(URL source) {
			this.sources.add(source);
			return this;
		}

		public Builder addSources(URL... sources) {
			for (URL u : sources) {
				this.sources.add(u);
			}
			return this;
		}

		public Builder addSources(Iterable<URL> sources) {
			for (URL u : sources) {
				this.sources.add(u);
			}
			return this;
		}

		public Builder addRules(Consumer<Builder> rulesAdder) {
			rulesAdder.accept(this);
			return this;
		}

		public Builder parent(ClassLoader parent) {
			this.parent = parent;
			return this;
		}

		public RuledClassLoader build() {
			return new RuledClassLoader(sources.toArray(new URL[sources.size()]), parent, ImmutableList.copyOf(rules), ImmutableList.copyOf(exemptions));
		}
	}

	public static Builder builder() { return new Builder(); }

	static {
		registerAsParallelCapable();
	}

	private final ImmutableList<BiPredicate<String, String>> exemptions;
	private final ImmutableList<BiFunction<String, String, ClassRule>> rules;

	private final Object ucp;

	private final MethodHandle getResource;

	private final MethodHandle getBytes;
	private final MethodHandle getManifest;
	private final MethodHandle getCodeSourceURL;
	private final MethodHandle getCodeSigners;

	private final MethodHandle definePackageInternal;
	private final MethodHandle findBootstrapClassOrNullHS;
	private final MethodHandle findLoadedClassJ9;
	private final MethodHandle bootstrapClassLoaderJ9;

	private RuledClassLoader(URL[] urls, ClassLoader parent, ImmutableList<BiFunction<String, String, ClassRule>> rules, ImmutableList<BiPredicate<String, String>> exemptions) {
		super(urls, parent);
		this.rules = rules;
		this.exemptions = exemptions;
		try {
			Field ucpF = URLClassLoader.class.getDeclaredField("ucp");
			ucpF.setAccessible(true);
			this.ucp = ucpF.get(this);

			this.definePackageInternal = unreflect(URLClassLoader.class.getDeclaredMethod("getAndVerifyPackage", String.class, Manifest.class, URL.class));

			Class<?> ucpClass = Class.forName("jdk.internal.loader.URLClassPath");
			this.getResource = unreflect(ucpClass.getDeclaredMethod("getResource", String.class, boolean.class));

			Class<?> resource = Class.forName("jdk.internal.loader.Resource");
			this.getBytes = unreflect(resource.getDeclaredMethod("getBytes"));
			this.getManifest = unreflect(resource.getDeclaredMethod("getManifest"));
			this.getCodeSourceURL = unreflect(resource.getDeclaredMethod("getCodeSourceURL"));
			this.getCodeSigners = unreflect(resource.getDeclaredMethod("getCodeSigners"));

			Method fbcon = null;
			try {
				// HotSpot
				fbcon = ClassLoader.class.getDeclaredMethod("findBootstrapClassOrNull", String.class);
				fbcon.setAccessible(true);
			} catch (NoSuchMethodException e) {
				// OpenJ9
				// done this way to avoid double-defining or not defining the final fields
			}
			if (fbcon != null) {
				this.findBootstrapClassOrNullHS = MethodHandles.lookup().unreflect(fbcon);
				this.findLoadedClassJ9 = null;
				this.bootstrapClassLoaderJ9 = null;
			} else {
				this.findBootstrapClassOrNullHS = null;
				this.findLoadedClassJ9 = unreflect(ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class));
				this.bootstrapClassLoaderJ9 = unreflectGetter(ClassLoader.class.getDeclaredField("bootstrapClassLoader"));
			}
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	private static MethodHandle unreflect(Method m) throws IllegalAccessException {
		m.setAccessible(true);
		return MethodHandles.lookup().unreflect(m);
	}

	private static MethodHandle unreflectGetter(Field f) throws IllegalAccessException {
		f.setAccessible(true);
		return MethodHandles.lookup().unreflectGetter(f);
	}

	private static StackTraceElement getCaller(boolean skipImmediateCaller) {
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		int i = -1;
		String immediateCaller = null;
		for (StackTraceElement ste : stack) {
			i++;
			if (i == 0) continue;
			if (!isNoise(ste.getClassName())) {
				if (skipImmediateCaller) {
					if (immediateCaller == null) {
						immediateCaller = ste.getClassName();
						continue;
					} else if (immediateCaller.equals(ste.getClassName())) {
						continue;
					}
				}
				return ste;
			}
		}
		throw new AssertionError();
	}

	private static boolean isNoise(String className) {
		return className.equals("com.playsawdust.chipper.rcl.RuledClassLoader")
				|| className.startsWith("com.playsawdust.chipper.rcl.RuledClassLoader$")
				|| className.equals("java.lang.ClassLoader")
				|| className.startsWith("java.lang.ClassLoader$")
				|| className.equals("java.lang.Class")
				|| className.startsWith("java.lang.Class$")
				|| className.equals("java.security.SecureClassLoader")
				|| className.startsWith("java.security.SecureClassLoader$")
				|| className.equals("java.net.URLClassLoader")
				|| className.startsWith("java.net.URLClassLoader$")
				|| className.equals("java.security.AccessController")
				|| className.startsWith("java.security.AccessController$")
				|| className.startsWith("java.lang.reflect.");
	}

	private ClassRule findClassRule(String name, String method) {
		for (BiFunction<String, String, ClassRule> rule : rules) {
			ClassRule cr = rule.apply(name, method);
			if (cr != null) {
				if (cr.type == ClassRuleType.PERMITTED) return null;
				return cr;
			}
		}
		return null;
	}

	private boolean isExempt(String name, String method) {
		for (BiPredicate<String, String> pred : exemptions) {
			if (pred.test(name, method)) return true;
		}
		return false;
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		try {
			Class<?> boot;
			if (findBootstrapClassOrNullHS != null) {
				boot = (Class<?>)findBootstrapClassOrNullHS.invokeExact((ClassLoader)this, name);
			} else {
				ClassLoader bootstrap = (ClassLoader)bootstrapClassLoaderJ9.invokeExact();
				boot = (Class<?>)findLoadedClassJ9.invokeExact(bootstrap, name);
				if (boot == null) {
					boot = bootstrap.loadClass(name);
				}
			}
			if (boot != null) return boot;
		} catch (ClassNotFoundException e) {
			// continue to try to find it on our classpath
		} catch (Throwable t) {
			throw new InternalError(t);
		}
		Class<?> clazz = findClass(name);
		if (resolve) {
			resolveClass(clazz);
		}
		return clazz;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		Class<?> result;
		try {
			result = AccessController.doPrivileged((PrivilegedExceptionAction<Class<?>>)() -> {
				String path = name.replace('.', '/')+".class";
				try {
					Object res = getResource.invoke(ucp, path, false);
					if (res != null) {
						return defineClass(name, res);
					} else {
						return null;
					}
				} catch (Throwable t) {
					throw new ClassNotFoundException(name, t);
				}
			});
		} catch (PrivilegedActionException pae) {
			throw (ClassNotFoundException) pae.getException();
		}
		if (result == null) {
			throw new ClassNotFoundException(name);
		}
		return result;
	}

	private Class<?> defineClass(String name, Object res) throws IOException {
		try {
			byte[] data = (byte[]) getBytes.invoke(res);
			int lastDot = name.lastIndexOf('.');
			if (lastDot != -1) {
				String pkgname = name.substring(0, lastDot);
				Manifest man = (Manifest)getManifest.invoke(res);
				try {
					definePackageInternal.invoke((URLClassLoader)this, pkgname, man, (URL)getCodeSourceURL.invoke(res));
				} catch (Throwable e) {
					if (e instanceof RuntimeException) throw (RuntimeException)e;
					if (e instanceof Error) throw (Error)e;
					throw new RuntimeException(e);
				}
			}
			if (!isExempt(name, null)) {
				ClassReader reader = new ClassReader(data);
				ClassNode node = new ClassNode(ASM7);
				reader.accept(node, 0);
				int nextIdentifierLiteralId = 1;
				boolean changed = false;
				InsnList pendingClinitInjections = new InsnList();
				InsnList clinitInstructions = null;
				for (MethodNode mn : node.methods) {
					if (mn.name.equals("<clinit>")) {
						clinitInstructions = mn.instructions;
						clinitInstructions.insert(pendingClinitInjections);
					}
					if (isExempt(name, mn.name)) continue;
					/*
					 * to optimize `new Identifier` calls with two literals, we need to detect the
					 * following bytecode:
					 *
					 * new Lcom/playsawdust/chipper/Identifier;
					 * dup
					 * ldc <namespace>
					 * ldc <path>
					 * invokespecial com/playsawdust/chipper/Identifier <init> (Ljava/lang/String;Ljava/lang/String)V
					 *
					 * we then synthesize a private static final field named CHIPPER$$GENLITID$<NAMESPACE>$<PATH>$<ID>
					 * and move the initialization code into clinit, add a putstatic, and replace what
					 * we found with a getstatic and a bunch of NOPs
					 *
					 * we keep track of our stage as a number; the "stage" is how many of the parts of
					 * bytecode we've found thus far; if we find anything unrecognized, we reset the
					 * stage to 0
					 */
					int newIdentifierStage = 0;
					String newIdentifierLiteralNamespace = null;
					String newIdentifierLiteralPath = null;
					boolean squelchNext = false;
					for (int i = 0; i < mn.instructions.size(); i++) {
						AbstractInsnNode insn = mn.instructions.get(i);
						if (!mn.name.equals("<clinit>")) {
							switch (newIdentifierStage) {
								case 0: {
									if (insn.getOpcode() == NEW && (((TypeInsnNode)insn).desc).equals("com/playsawdust/chipper/Identifier")) {
										newIdentifierStage = 1;
									} else {
										newIdentifierStage = 0;
									}
									break;
								}
								case 1: {
									if (insn.getOpcode() == DUP) {
										newIdentifierStage = 2;
									} else {
										newIdentifierStage = 0;
									}
									break;
								}
								case 2: {
									if (insn.getOpcode() == LDC && insn instanceof LdcInsnNode) {
										LdcInsnNode ldc = (LdcInsnNode)insn;
										if (ldc.cst instanceof String) {
											newIdentifierLiteralNamespace = (String)ldc.cst;
											newIdentifierStage = 3;
										} else {
											newIdentifierStage = 0;
										}
									} else {
										newIdentifierStage = 0;
									}
									break;
								}
								case 3: {
									if (insn.getOpcode() == LDC && insn instanceof LdcInsnNode) {
										LdcInsnNode ldc = (LdcInsnNode)insn;
										if (ldc.cst instanceof String) {
											newIdentifierLiteralPath = (String)ldc.cst;
											newIdentifierStage = 4;
										} else {
											newIdentifierStage = 0;
											newIdentifierLiteralNamespace = null;
										}
									} else {
										newIdentifierStage = 0;
										newIdentifierLiteralNamespace = null;
									}
									break;
								}
								case 4: {
									if (insn.getOpcode() == INVOKESPECIAL && insn instanceof MethodInsnNode && newIdentifierLiteralNamespace != null && newIdentifierLiteralPath != null) {
										MethodInsnNode m = (MethodInsnNode)insn;
										if (m.owner.equals("com/playsawdust/chipper/Identifier")
												&& m.name.equals("<init>")
												&& m.desc.equals("(Ljava/lang/String;Ljava/lang/String;)V")
												&& !m.itf) {
											// we found it all; do it
											String cleanPath = newIdentifierLiteralPath.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
											String fieldName = "CHIPPER$$GENLITID$"+newIdentifierLiteralNamespace.toUpperCase(Locale.ROOT)+"$"+cleanPath+"$"+nextIdentifierLiteralId;
											nextIdentifierLiteralId++;
											FieldNode field = new FieldNode(ACC_PRIVATE | ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC,
													fieldName, "Lcom/playsawdust/chipper/Identifier;",
													null, null);
											node.fields.add(field);
											InsnNode nop = new InsnNode(NOP);
											mn.instructions.set(insn.getPrevious().getPrevious().getPrevious().getPrevious(), nop);
											mn.instructions.set(insn.getPrevious().getPrevious().getPrevious(), nop);
											mn.instructions.set(insn.getPrevious().getPrevious(), nop);
											mn.instructions.set(insn.getPrevious(), nop);
											mn.instructions.set(insn, new FieldInsnNode(GETSTATIC, node.name, fieldName, "Lcom/playsawdust/chipper/Identifier;"));
											InsnList li = new InsnList();
											li.add(new TypeInsnNode(NEW, "com/playsawdust/chipper/Identifier"));
											li.add(new InsnNode(DUP));
											li.add(new LdcInsnNode(newIdentifierLiteralNamespace));
											li.add(new LdcInsnNode(newIdentifierLiteralPath));
											li.add(new MethodInsnNode(INVOKESPECIAL, "com/playsawdust/chipper/Identifier", "<init>", "(Ljava/lang/String;Ljava/lang/String;)V", false));
											li.add(new FieldInsnNode(PUTSTATIC, node.name, fieldName, "Lcom/playsawdust/chipper/Identifier;"));
											if (clinitInstructions == null) {
												pendingClinitInjections.add(li);
											} else {
												clinitInstructions.insert(li);
											}
											changed = true;
										}
									}
									newIdentifierStage = 0;
									newIdentifierLiteralNamespace = null;
									newIdentifierLiteralPath = null;
									break;
								}
							}
						} else {
							if (insn instanceof MethodInsnNode) {
								MethodInsnNode m = (MethodInsnNode)insn;
								if (m.owner.equals("com/playsawdust/chipper/rcl/AccessRules") && m.name.equals("squelchNextWarning")) {
									squelchNext = true;
								}
								if (m.getOpcode() == INVOKESTATIC || m.name.equals("<init>")) {
									String clazz = m.owner.replace('/', '.');
									ClassRule rule = findClassRule(clazz, m.name+m.desc);
									if (rule != null) {
										if (squelchNext) {
											squelchNext = false;
										} else {
											InsnList li = getRuleEnactment(clazz, m.name, rule, true);
											// insert these instructions *after* the node we found
											// putting them before would be more "correct", but would be a pain to do right
											mn.instructions.insert(insn, li);
											changed = true;
										}
									}
								}
							}
						}
					}
				}
				if (clinitInstructions == null && pendingClinitInjections.size() > 0) {
					MethodNode clinit = new MethodNode(ACC_STATIC | ACC_SYNTHETIC, "<clinit>", "()V", null, null);
					pendingClinitInjections.add(new InsnNode(RETURN));
					clinit.instructions = pendingClinitInjections;
					node.methods.add(clinit);
				}
				if (changed) {
					ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
					node.accept(writer);
	//				try {
	//					Files.write(data, new File("/tmp/"+node.name.replace('/', '_')+"__before.class"));
	//				} catch (IOException e) {
	//					e.printStackTrace();
	//				}
					data = writer.toByteArray();
	//				try {
	//					Files.write(data, new File("/tmp/"+node.name.replace('/', '_')+"__after.class"));
	//				} catch (IOException e) {
	//					e.printStackTrace();
	//				}
				}
			}
			return defineClass(name, data, 0, data.length, new CodeSource((URL)getCodeSourceURL.invoke(res), (CodeSigner[])getCodeSigners.invoke(res)));
		} catch (IOException e) {
			throw e;
		} catch (Throwable t) {
			throw new IOException(t);
		}
	}

	private InsnList getRuleEnactment(String clazz, String method, ClassRule cr, boolean foreign) {
		InsnList li = new InsnList();
		li.add(new LdcInsnNode(clazz));
		if (method != null) {
			li.add(new LdcInsnNode(method));
		} else {
			li.add(new InsnNode(ACONST_NULL));
		}
		li.add(new LdcInsnNode(cr.reason));
		li.add(new LdcInsnNode(foreign));
		li.add(new MethodInsnNode(INVOKESTATIC, "com/playsawdust/chipper/rcl/RuledClassLoader", "$$enactClassRule$"+cr.type, "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)V", false));
		return li;
	}

	private static String substitute(String str, String clazz, String method) {
		String shortName = clazz.substring(clazz.lastIndexOf('.')+1);
		return str.replace("$$$", shortName+"."+method).replace("$$", shortName);
	}

	public static void $$enactClassRule$FORBIDDEN(String clazz, String method, String reason, boolean foreign) {
		StackTraceElement caller = getCaller(!foreign);
		AccessRules.log.error("{}.{}({}:{}) makes a forbidden reference.\n{}",
				caller.getClassName(), caller.getMethodName(), caller.getFileName(), caller.getLineNumber(), substitute(reason, clazz, method));
		throw new ForbiddenClassError();
	}

	public static void $$enactClassRule$DISCOURAGED(String clazz, String method, String reason, boolean foreign) {
		StackTraceElement caller = getCaller(!foreign);
		AccessRules.log.warn("{}.{}({}:{}) makes a discouraged reference.\n{}",
				caller.getClassName(), caller.getMethodName(), caller.getFileName(), caller.getLineNumber(), substitute(reason, clazz, method));
	}

}
