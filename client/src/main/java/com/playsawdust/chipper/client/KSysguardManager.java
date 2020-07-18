/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.NVXGPUMemoryInfo.*;
import static org.lwjgl.opengl.ATIMeminfo.*;
import static org.lwjgl.opengl.AMDPerformanceMonitor.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.playsawdust.chipper.client.ClientEngine;
import com.playsawdust.chipper.client.component.LayerController;
import com.playsawdust.chipper.component.Context;
import com.playsawdust.chipper.math.FastMath;

import com.playsawdust.chipper.toolbox.lipstick.MonotonicTime;

/**
 * @see <a href="https://github.com/KDE/ksysguard/blob/master/ksysguardd/Porting-HOWTO">https://github.com/KDE/ksysguard/blob/master/ksysguardd/Porting-HOWTO</a>
 */
public class KSysguardManager {
	private static abstract class Monitor {
		public final String name;
		public final long min;
		public final long max;
		public final String unit;
		public Monitor(String name, long min, long max, String unit) {
			this.name = name;
			this.min = min;
			this.max = max;
			this.unit = unit;
		}
		public abstract String get();
		public abstract String getType();
		@Override
		public String toString() {
			return name+"\t"+min+"\t"+max+"\t"+unit;
		}
	}
	private interface Updatable {
		void update();
	}
	private static class LiveMonitor extends Monitor {
		private final LongSupplier longSupplier;
		private final DoubleSupplier doubleSupplier;

		public LiveMonitor(String name, long min, long max, String unit, LongSupplier supplier) {
			super(name, min, max, unit);
			this.longSupplier = supplier;
			this.doubleSupplier = null;
		}

		public LiveMonitor(String name, long min, long max, String unit, DoubleSupplier supplier) {
			super(name, min, max, unit);
			this.longSupplier = null;
			this.doubleSupplier = supplier;
		}

		@Override
		public String get() {
			return longSupplier == null ? Double.toString(doubleSupplier.getAsDouble()) : Long.toString(longSupplier.getAsLong());
		}

		@Override
		public String getType() {
			return longSupplier == null ? "float" : "integer";
		}
	}
	private static class PollingMonitor extends Monitor implements Updatable {
		private final LongSupplier longUpdater;
		private final DoubleSupplier doubleUpdater;

		private long longValue;
		private double doubleValue;

		private long lastUpdate;
		private boolean lastHasBeenUsed = false;
		private boolean hasBeenUsed = false;

		private Runnable initializer;

		public PollingMonitor(String name, long min, long max, String unit, LongSupplier updater) {
			super(name, min, max, unit);
			this.longUpdater = updater;
			this.doubleUpdater = null;
		}

		public PollingMonitor(String name, long min, long max, String unit, DoubleSupplier updater) {
			super(name, min, max, unit);
			this.longUpdater = null;
			this.doubleUpdater = updater;
		}

		public PollingMonitor setInitializer(Runnable r) {
			this.initializer = r;
			return this;
		}

		@Override
		public String get() {
			hasBeenUsed = true;
			return longUpdater == null ? Double.toString(doubleValue) : Long.toString(longValue);
		}

		@Override
		public String getType() {
			return longUpdater == null ? "float" : "integer";
		}

		@Override
		public void update() {
			if (!hasBeenUsed) return;
			long time = MonotonicTime.millis();
			if (time-lastUpdate < 1000) return;
			if (!lastHasBeenUsed && hasBeenUsed) {
				lastHasBeenUsed = true;
				if (initializer != null) initializer.run();
			}
			lastUpdate = time;
			if (longUpdater == null) {
				doubleValue = doubleUpdater.getAsDouble();
			} else {
				longValue = longUpdater.getAsLong();
			}
		}
	}
	private static class SettableMonitor extends Monitor implements Updatable {
		private final boolean isDouble;

		private long longValue;
		private double doubleValue;

		private Runnable initializer;

		private boolean lastHasBeenUsed = false;
		private boolean hasBeenUsed = false;

		public SettableMonitor(String name, long min, long max, String unit, long initialValue) {
			super(name, min, max, unit);
			this.isDouble = false;
			this.longValue = initialValue;
		}

		public SettableMonitor(String name, long min, long max, String unit, double initialValue) {
			super(name, min, max, unit);
			this.isDouble = true;
			this.doubleValue = initialValue;
		}

		public SettableMonitor setInitializer(Runnable r) {
			this.initializer = r;
			return this;
		}

		public void set(long value) {
			if (isDouble) throw new IllegalStateException();
			longValue = value;
		}

		public void set(double value) {
			if (!isDouble) throw new IllegalStateException();
			doubleValue = value;
		}

		@Override
		public String get() {
			hasBeenUsed = true;
			return isDouble ? Double.toString(doubleValue) : Long.toString(longValue);
		}

		@Override
		public String getType() {
			return isDouble ? "float" : "integer";
		}

		@Override
		public void update() {
			if (!lastHasBeenUsed && hasBeenUsed) {
				lastHasBeenUsed = true;
				initializer.run();
			}
		}
	}

	private static final Logger log = LoggerFactory.getLogger(KSysguardManager.class);

	private static Thread thread;
	private static ServerSocket serverSock;

	private static final Map<String, Monitor> monitors = Maps.newLinkedHashMap();
	private static final Table<Integer, Integer, SettableMonitor> monitorsByPerfmonId = HashBasedTable.create();
	private static final Table<Integer, Integer, Integer> counterTypes = HashBasedTable.create();

	private static int monitor = -1;
	private static boolean anyPerfmonMonitorsEnabled = false;


	public static void start(Context<ClientEngine> ctx) {
		if (thread != null) return;
		if (System.getenv("CHIPPER_START_KSYSGUARD") != null) {
			anyPerfmonMonitorsEnabled = false;
			synchronized (monitors) {
				monitors.clear();
				monitors.put("engine/uptime", new LiveMonitor("Uptime", 0, 0, "s",
						() -> (long)MonotonicTime.seconds()));
				monitors.put("engine/fps", new LiveMonitor("Frames per second", 0, 60, "",
						() -> ctx.getEngine().getFramesPerSecond()));
				monitors.put("engine/mspf", new LiveMonitor("Time per frame", 0, 33, "ms",
						() -> ctx.getEngine().getMillisPerFrame()));
				monitors.put("engine/layer_count", new LiveMonitor("Layer count", 0, 0, "",
						() -> LayerController.obtain(ctx).getLayers().size()));
				ClassLoadingMXBean classLoading = ManagementFactory.getClassLoadingMXBean();
				monitors.put("vm/class_loading/loaded", new LiveMonitor("Loaded classes", 0, 0, "",
						() -> classLoading.getLoadedClassCount()));
				monitors.put("vm/class_loading/total_loaded", new LiveMonitor("Total loaded classes", 0, 0, "",
						() -> classLoading.getTotalLoadedClassCount()));
				monitors.put("vm/class_loading/unloaded", new LiveMonitor("Unloaded classes", 0, 0, "",
						() -> classLoading.getUnloadedClassCount()));
				CompilationMXBean compilation = ManagementFactory.getCompilationMXBean();
				monitors.put("vm/compilation/time", new LiveMonitor("Time spent compiling", 0, 0, "ms",
						() -> compilation.getTotalCompilationTime()));
				List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
				for (GarbageCollectorMXBean bean : gcBeans) {
					String name = bean.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-zA-Z0-9]", "_");
					monitors.put("vm/gc/"+name+"/collection_count", new LiveMonitor("Collection count", 0, 0, "",
							() -> bean.getCollectionCount()));
					monitors.put("vm/gc/"+name+"/collection_time", new LiveMonitor("Collection time", 0, 0, "ms",
							() -> bean.getCollectionTime()));
				}
				MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
				monitors.put("vm/memory/heap/used", new LiveMonitor("Used heap memory", 0, mem.getHeapMemoryUsage().getMax()/1024, "KB",
						() -> mem.getHeapMemoryUsage().getUsed()/1024));
				monitors.put("vm/memory/heap/committed", new LiveMonitor("Committed heap memory", 0, mem.getHeapMemoryUsage().getMax()/1024, "KB",
						() -> mem.getHeapMemoryUsage().getCommitted()/1024));
				monitors.put("vm/memory/offheap/used", new LiveMonitor("Used offheap memory", 0, mem.getNonHeapMemoryUsage().getMax()/1024, "KB",
						() -> mem.getNonHeapMemoryUsage().getUsed()/1024));
				monitors.put("vm/memory/offheap/committed", new LiveMonitor("Committed offheap memory", 0, mem.getNonHeapMemoryUsage().getMax()/1024, "KB",
						() -> mem.getNonHeapMemoryUsage().getCommitted()/1024));
				monitors.put("vm/memory/pending_finalization", new LiveMonitor("Objects pending finalization", 0, 0, "",
						() -> mem.getObjectPendingFinalizationCount()));
				// doesn't work without ENABLE_STATISTICS
				/*
				RPmallocGlobalStatistics globalStats = RPmallocGlobalStatistics.create();
				RPmallocThreadStatistics threadStats = RPmallocThreadStatistics.create();
				monitors.put("rpmalloc/global/cached", new LiveMonitor("Global cached memory", 0, 0, "KB",
						() -> {
							RPmalloc.rpmalloc_global_statistics(globalStats);
							return globalStats.cached()/1024;
						}));
				monitors.put("rpmalloc/global/mapped", new LiveMonitor("Global mapped memory", 0, 0, "KB",
						() -> {
							RPmalloc.rpmalloc_global_statistics(globalStats);
							return globalStats.mapped()/1024;
						}));
				monitors.put("rpmalloc/global/mapped_total", new LiveMonitor("Global total mapped memory", 0, 0, "KB",
						() -> {
							RPmalloc.rpmalloc_global_statistics(globalStats);
							return globalStats.mapped_total()/1024;
						}));
				monitors.put("rpmalloc/global/unmapped_total", new LiveMonitor("Global total unmapped memory", 0, 0, "KB",
						() -> {
							RPmalloc.rpmalloc_global_statistics(globalStats);
							return globalStats.unmapped_total()/1024;
						}));

				monitors.put("rpmalloc/thread/active", new LiveMonitor("Engine thread active memory", 0, 0, "KB",
						() -> {
							RPmalloc.rpmalloc_thread_statistics(threadStats);
							return threadStats.active()/1024;
						}));
				monitors.put("rpmalloc/thread/deferred", new LiveMonitor("Engine thread deferred memory", 0, 0, "KB",
						() -> {
							RPmalloc.rpmalloc_thread_statistics(threadStats);
							return threadStats.deferred()/1024;
						}));
				monitors.put("rpmalloc/thread/sizecache", new LiveMonitor("Engine thread size cache memory", 0, 0, "KB",
						() -> {
							RPmalloc.rpmalloc_thread_statistics(threadStats);
							return threadStats.sizecache()/1024;
						}));
				monitors.put("rpmalloc/thread/spancache", new LiveMonitor("Engine thread span cache memory", 0, 0, "KB",
						() -> {
							RPmalloc.rpmalloc_thread_statistics(threadStats);
							return threadStats.spancache()/1024;
						}));
				monitors.put("rpmalloc/thread/thread_to_global", new LiveMonitor("Engine thread surrendered-to-global memory", 0, 0, "KB",
						() -> {
							RPmalloc.rpmalloc_thread_statistics(threadStats);
							return threadStats.thread_to_global()/1024;
						}));
				monitors.put("rpmalloc/thread/global_to_thread", new LiveMonitor("Engine thread taken-from-global memory", 0, 0, "KB",
						() -> {
							RPmalloc.rpmalloc_thread_statistics(threadStats);
							return threadStats.global_to_thread()/1024;
						}));
				*/
				long gpuMax = 0;
				if (GL.getCapabilities().GL_NVX_gpu_memory_info) {
					gpuMax = glGetInteger(GL_GPU_MEMORY_INFO_TOTAL_AVAILABLE_MEMORY_NVX);
					monitors.put("gpu/memory/nvx/dedicated", new PollingMonitor("Dedicated memory", 0, gpuMax, "KB",
							() -> glGetInteger(GL_GPU_MEMORY_INFO_DEDICATED_VIDMEM_NVX)));
					monitors.put("gpu/memory/nvx/available", new PollingMonitor("Available memory", 0, gpuMax, "KB",
							() -> glGetInteger(GL_GPU_MEMORY_INFO_CURRENT_AVAILABLE_VIDMEM_NVX)));
					monitors.put("gpu/memory/nvx/eviction_count", new PollingMonitor("Eviction count", 0, 0, "",
							() -> glGetInteger(GL_GPU_MEMORY_INFO_EVICTION_COUNT_NVX)));
					monitors.put("gpu/memory/nvx/evicted_memory", new PollingMonitor("Evicted memory", 0, 0, "KB",
							() -> glGetInteger(GL_GPU_MEMORY_INFO_EVICTED_MEMORY_NVX)));
				}
				if (GL.getCapabilities().GL_ATI_meminfo) {
					IntBuffer rtrn = BufferUtils.createIntBuffer(4);
					monitors.put("gpu/memory/ati/free", new PollingMonitor("Free memory", 0, gpuMax, "KB",
							() -> {
								glGetIntegerv(GL_TEXTURE_FREE_MEMORY_ATI, rtrn);
								return rtrn.get(0);
							}));
					monitors.put("gpu/memory/ati/used", new PollingMonitor("Used memory", 0, gpuMax, "KB",
							() -> {
								glGetIntegerv(GL_TEXTURE_FREE_MEMORY_ATI, rtrn);
								return rtrn.get(2);
							}));
				}
				if (GL.getCapabilities().GL_AMD_performance_monitor) {
					monitor = glGenPerfMonitorsAMD();
					try (MemoryStack stack = MemoryStack.stackPush()) {
						IntBuffer a = stack.mallocInt(1);
						glGetPerfMonitorGroupsAMD(a, null);
						int numGroups = a.get(0);
						IntBuffer groups = BufferUtils.createIntBuffer(numGroups);
						glGetPerfMonitorGroupsAMD(null, groups);
						IntBuffer b = stack.mallocInt(1);
						ByteBuffer scratch = BufferUtils.createByteBuffer(512);
						IntBuffer counterList = BufferUtils.createIntBuffer(512);
						ByteBuffer d = stack.malloc(16);
						for (int i = 0; i < numGroups; i++) {
							int group = groups.get(i);
							glGetPerfMonitorGroupStringAMD(group, a, scratch);
							int groupNameLength = a.get(0);
							String groupName = MemoryUtil.memUTF8(scratch, groupNameLength);
							glGetPerfMonitorCountersAMD(group, a, b, counterList);
							int numCounters = a.get(0);
							int maxActiveCounters = b.get(0);
							for (int j = 0; j < numCounters; j++) {
								int counter = counterList.get(j);
								glGetPerfMonitorCounterStringAMD(group, counter, a, scratch);
								int counterNameLength = a.get(0);
								String counterName = MemoryUtil.memUTF8(scratch, counterNameLength);
								glGetPerfMonitorCounterInfoAMD(group, counter, GL_COUNTER_TYPE_AMD, d);
								int type = d.getInt(0);
								long min = 0;
								long max = 0;
								String unit = "";
								String key = "gpu/perf_mon/"+groupName.replace("/", "\\/")+"/"+counterName.replace("/", "\\/");
								String name = groupName+": "+counterName;
								boolean flt = false;
								if (type == GL_UNSIGNED_INT) {
									glGetPerfMonitorCounterInfoAMD(group, counter, GL_COUNTER_RANGE_AMD, d);
									min = d.getInt(0)&0xFFFFFFFFL;
									max = d.getInt(4)&0xFFFFFFFFL;
								} else if (type == GL_UNSIGNED_INT64_AMD) {
									glGetPerfMonitorCounterInfoAMD(group, counter, GL_COUNTER_RANGE_AMD, d);
									min = d.getLong(0);
									max = d.getLong(8);
								} else if (type == GL_PERCENTAGE_AMD) {
									min = 0;
									max = 100;
									unit = "%";
								} else if (type == GL_FLOAT) {
									glGetPerfMonitorCounterInfoAMD(group, counter, GL_COUNTER_RANGE_AMD, d);
									min = FastMath.round(d.getFloat(0));
									max = FastMath.round(d.getFloat(4));
									flt = true;
								} else {
									break;
								}
								SettableMonitor m;
								if (flt) {
									m = new SettableMonitor(name, min, max, unit, 0D);
								} else {
									m = new SettableMonitor(name, min, max, unit, 0L);
								}
								counterTypes.put(group, counter, type);
								monitorsByPerfmonId.put(group, counter, m);
								m.setInitializer(() -> {
									anyPerfmonMonitorsEnabled = true;
									glSelectPerfMonitorCountersAMD(monitor, true, group, new int[] {counter});
								});
								monitors.put(key, m);
							}
						}
					}
				}
			}
			thread = new Thread(() -> {
				Process ksysguard = null;
				try (ServerSocket server = new ServerSocket(51281, 50, InetAddress.getLoopbackAddress())) {
					serverSock = server;
					ksysguard = Runtime.getRuntime().exec("ksysguard");
					log.info("KSysguard daemon emulation listening on localhost:{}", server.getLocalPort());
					if (ksysguard.isAlive()) {
						log.info("KSysguard forked. Be sure to save your sheet before closing the game.");
					}
					while (true) {
						Socket sock = server.accept();
						Thread t = new Thread(() -> {
							try (OutputStreamWriter osw = new OutputStreamWriter(sock.getOutputStream());
									Scanner scanner = new Scanner(sock.getInputStream(), "UTF-8")) {
								osw.write("ksysguardd 4\n");
								osw.write("(actually it's Chipper but don't worry about it)\n");
								osw.write("(c) 2019 Una Thompson");
								osw.flush();
								try {
									while (true) {
										osw.write("\nksysguardd> ");
										osw.flush();
										String line = scanner.nextLine();
										if ("quit".equals(line)) break;
										synchronized (monitors) {
											if (line.equals("monitors")) {
												for (Map.Entry<String, Monitor> en : monitors.entrySet()) {
													osw.write(en.getKey()+"\t"+en.getValue().getType()+"\n");
												}
											} else if (line.endsWith("?")) {
												String monitorName = line.substring(0, line.length()-1);
												if (monitors.containsKey(monitorName)) {
													osw.write(monitors.get(monitorName).toString());
												} else {
													osw.write("UNKNOWN COMMAND");
												}
											} else {
												if (monitors.containsKey(line)) {
													osw.write(monitors.get(line).get());
												} else {
													osw.write("UNKNOWN COMMAND");
												}
											}
										}
										osw.flush();
									}
								} catch (NoSuchElementException e) {}
							} catch (IOException e) {
								log.error("Error in ksysguard io thread", e);
							}
						}, "ksysguard io thread");
						t.setDaemon(true);
						t.start();
					}
				} catch (Exception e) {
					if (!(e instanceof SocketException && "Socket closed".equals(e.getMessage()))) {
						log.error("Error in ksysguard acceptor thread", e);
					}
				} finally {
					if (ksysguard != null) {
						ksysguard.destroy();
					}
					serverSock = null;
				}
			}, "ksysguard acceptor thread");
			thread.setDaemon(false);
			thread.start();
		}
	}

	public static void frameStart() {
		if (monitor != -1 && anyPerfmonMonitorsEnabled) {
			glBeginPerfMonitorAMD(monitor);
		}
	}

	public static void frameEnd() {
		if (monitor != -1 && anyPerfmonMonitorsEnabled) {
			glEndPerfMonitorAMD(monitor);

			try (MemoryStack stack = MemoryStack.stackPush()) {
				IntBuffer a = stack.mallocInt(1);
				glGetPerfMonitorCounterDataAMD(monitor, GL_PERFMON_RESULT_SIZE_AMD, a, null);
				int resultSize = a.get(0);
				ByteBuffer data = MemoryUtil.memAlloc(resultSize);
				try {
					synchronized (monitors) {
						while (data.remaining() > 0) {
							int group = data.getInt();
							int counter = data.getInt();
							if (!counterTypes.contains(group, counter)) continue;
							SettableMonitor monitor = monitorsByPerfmonId.get(group, counter);
							int type = counterTypes.get(group, counter);
							if (type == GL_UNSIGNED_INT) {
								monitor.set(data.getInt()&0xFFFFFFFFL);
							} else if (type == GL_UNSIGNED_INT64_AMD) {
								monitor.set(data.getLong());
							} else if (type == GL_PERCENTAGE_AMD) {
								monitor.set(data.getFloat());
							} else if (type == GL_FLOAT) {
								monitor.set(data.getFloat());
							}
						}
					}
				} finally {
					MemoryUtil.memFree(data);
				}
			}
		}
		if (thread != null) {
			synchronized (monitors) {
				for (Monitor m : monitors.values()) {
					if (m instanceof Updatable) {
						((Updatable) m).update();
					}
				}
			}
		}
	}

	public static void stop() {
		if (thread != null) {
			if (serverSock != null) {
				try {
					serverSock.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				serverSock = null;
			}
			thread = null;
		}
		synchronized (monitors) {
			monitors.clear();
		}
	}

}

