/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client;

import static com.playsawdust.chipper.client.BaseGL.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.rpmalloc.RPmalloc.*;
import static org.lwjgl.util.simd.SSE.*;
import static org.lwjgl.util.simd.SSE3.*;
import static org.lwjgl.openal.ALC11.*;
import static org.lwjgl.opengl.EXTFramebufferObject.*;
import static org.lwjgl.opengl.EXTFramebufferBlit.*;
import static org.lwjgl.opengl.EXTPackedDepthStencil.*;
import static org.lwjgl.stb.STBEasyFont.*;

import org.checkerframework.common.value.qual.IntRange;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.opengl.ARBMultisample;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.NativeResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.playsawdust.chipper.Addon;
import com.playsawdust.chipper.Distribution;
import com.playsawdust.chipper.Identifier;
import com.playsawdust.chipper.Parachute;
import com.playsawdust.chipper.Greeting;
import com.playsawdust.chipper.client.ClientEngine;
import com.playsawdust.chipper.client.component.Canvas;
import com.playsawdust.chipper.client.component.LayerController;
import com.playsawdust.chipper.client.component.ResourceCache;
import com.playsawdust.chipper.client.component.SoundManager;
import com.playsawdust.chipper.client.component.Canvas.BlendMode;
import com.playsawdust.chipper.client.gl.GLLinkException;
import com.playsawdust.chipper.client.gl.GLProgram;
import com.playsawdust.chipper.client.gl.GLTexture2D;
import com.playsawdust.chipper.client.gl.GLShader.ShaderType;
import com.playsawdust.chipper.client.input.KeyModifiers;
import com.playsawdust.chipper.client.input.CursorType;
import com.playsawdust.chipper.client.input.InputEventProcessor;
import com.playsawdust.chipper.client.input.Key;
import com.playsawdust.chipper.client.input.WindowInputListener;
import com.playsawdust.chipper.client.widget.DefaultEvent;
import com.playsawdust.chipper.client.widget.EventResponse;
import com.playsawdust.chipper.client.widget.Widget;
import com.playsawdust.chipper.client.widget.container.Container;
import com.playsawdust.chipper.component.Context;
import com.playsawdust.chipper.component.Engine;
import com.playsawdust.chipper.component.EngineType;
import com.playsawdust.chipper.exception.ResourceNotFoundException;
import com.playsawdust.chipper.img.BufferedImage;
import com.playsawdust.chipper.math.FastMath;
import com.playsawdust.chipper.math.Point2D;
import com.playsawdust.chipper.math.ProtoColor;
import com.playsawdust.chipper.math.RectD;
import com.playsawdust.chipper.math.RectI;

import com.playsawdust.chipper.toolbox.lipstick.MonotonicTime;

import com.sun.jna.Platform;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

public class ClientEngine implements Engine {
	private static final Logger log = LoggerFactory.getLogger(Distribution.NAME);

	private boolean ran = false;

	private long window;
	private PointerBuffer scratchPointerBuffer;

	private final List<Object> retain = Lists.newArrayList();

	private Context<ClientEngine> context;

	private boolean sizeChanged = false;
	private int windowWidth;
	private int windowHeight;
	private int framebufferWidth;
	private int framebufferHeight;
	private int canvasPixelScale = 2;
	private int canvasPixelScaleSetting = 2;
	private int lastCanvasPixelScaleSetting = 2;

	private Thread loadingThread;
	private boolean loading = true;
	private String loadingMessage = "Loading";

	private final List<Widget> widgetsToRender = Lists.newArrayList();
	private final List<InputEventProcessor> enteredThings = Lists.newArrayList();
	private final List<Widget> layersCopy = Lists.newArrayList();

	// TODO replace with an addon loader framework
	// especially important now that Sawdust is split off and we can't hardcode its Addon class...
	private Addon defaultAddon = null;

	private boolean mousePosChanged = false;
	private boolean mouseGrabbed = false;

	private boolean mouseInWindow = true;
	private double lastMouseX = 0;
	private double lastMouseY = 0;


	private int mousePixelScale = 0;

	private long lastCursor = NULL;

	private final List<DefaultEvent> pendingEvents = Lists.newArrayList();

	private final List<RectD> frostRegions = Lists.newArrayList();
	private ProtoColor glassColor = ProtoColor.BLACK;
	private double glassOpacity = 0.25;

	private GLProgram kawaseDownsample;
	private GLProgram kawaseUpsample;

	private int blurTex1 = -1;
	private int blurTex2 = -1;

	private int blurFbo1 = -1;
	private int blurFbo2 = -1;

	private int limitFbo = -1;
	private int limitFboColor = -1;
	private int limitFboDepthStencil = -1;
	private int limitFboScratchTex = -1;

	private int blurStrength = 50;

	private boolean neverRenderedAnything = true;

	private boolean limitResolution = false;
	private boolean lastLimitResolution = false;

	private boolean limitResolutionLinear = false;
	private boolean drawMouseOnCanvas = false;

	private GameState currentState;

	@Override
	@SuppressWarnings("deprecation")
	public int run(String... args) {
		if (ran) {
			throw new IllegalStateException("ClientEngine cannot be started more than once, even after being stopped");
		}
		ran = true;
		Thread.currentThread().setName("Engine thread");
		Greeting.print(this, log);

		Configuration.MEMORY_ALLOCATOR.set("rpmalloc");
		_MM_SET_FLUSH_ZERO_MODE(_MM_FLUSH_ZERO_ON);
		_MM_SET_DENORMALS_ZERO_MODE(_MM_DENORMALS_ZERO_ON);

		rpmalloc_initialize();
		rpmalloc_thread_initialize();

		scratchPointerBuffer = memAllocPointer(1);

		Parachute.allocate();
		
		if (System.getenv("WAYLAND_DISPLAY") != null) {
			log.debug("Enabling LWJGL Wayland support");
			Configuration.GLFW_LIBRARY_NAME.set("glfw_wayland");
		}

		// for now
		glfwInitHint(GLFW_COCOA_MENUBAR, GLFW_FALSE);
		glfwInitHint(GLFW_COCOA_CHDIR_RESOURCES, GLFW_FALSE);

		if (!glfwInit()) {
			log.error("GLFW initialization failed: {}", getGlfwErrorString());
			return 1;
		}

		glfwSetErrorCallback(retain((err, desc) -> {
			log.warn("GLFW error: {} (0x{})", memUTF8(desc), Integer.toHexString(err));
		}));

		glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
		glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);
		glfwWindowHint(GLFW_DEPTH_BITS, 24);
		glfwWindowHint(GLFW_STENCIL_BITS, 8);
		glfwWindowHint(GLFW_SAMPLES, 8);
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_FOCUS_ON_SHOW, GLFW_FALSE);

		glfwWindowHint(GLFW_COCOA_RETINA_FRAMEBUFFER, GLFW_TRUE);

		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_ANY_PROFILE);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 1);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0);

		glfwWindowHintString(GLFW_COCOA_FRAME_NAME, Distribution.ID);
		glfwWindowHintString(GLFW_X11_CLASS_NAME, Distribution.ID);
		glfwWindowHintString(GLFW_X11_INSTANCE_NAME, Distribution.ID);

		if (!tryCreateWindow()) {
			log.error("GLFW window creation failed: {}", getGlfwErrorString());
			return 2;
		}

		glfwSetWindowSizeLimits(window, 480, 360, GLFW_DONT_CARE, GLFW_DONT_CARE);

		glfwMakeContextCurrent(window);
		GL.createCapabilities();
		OpenGLDebug.install();

		GLCapabilities glcap = GL.getCapabilities();
		if (!glcap.OpenGL12) {
			log.error("Chipper requires at least OpenGL 1.2. Exiting.");
			return 3;
		}

		// Drawing directly to the front buffer allows us to dodge a glfwSwapBuffers call
		// We do this at all to make the window open as soon as possible, which improves perceived speed
		glDrawBuffer(GL_FRONT);
		glClearColor((float)Distribution.LOADING_BACKGROUND_COLOR.getRed(), (float)Distribution.LOADING_BACKGROUND_COLOR.getGreen(), (float)Distribution.LOADING_BACKGROUND_COLOR.getBlue(), 1);
		glClear(GL_COLOR_BUFFER_BIT);
		glDrawBuffer(GL_BACK);
		glfwShowWindow(window);

		try {
			glfwSetWindowSizeCallback(window, retain((win, w, h) -> {
				windowWidth = w;
				windowHeight = h;
				sizeChanged = true;
			}));

			glfwSetFramebufferSizeCallback(window, retain((win, w, h) -> {
				framebufferWidth = w;
				framebufferHeight = h;
			}));

			glfwSetWindowTitle(window, Distribution.NAME+" v"+Distribution.VERSION);
			if (glfwExtensionSupported("WGL_EXT_swap_control_tear")
					|| glfwExtensionSupported("GLX_EXT_swap_control_tear")) {
				log.info("Tear control is supported. Enabling adaptive vsync.");
				glfwSwapInterval(-1);
			} else {
				glfwSwapInterval(1);
			}

			context = Context.createNew(this);

			NativeResource[] garbage = new NativeResource[5];
			try (GLFWImage.Buffer images = GLFWImage.malloc(5)) {
				try {
					garbage[0] = loadGlfwImage(images, "images/icon-48.png");
					garbage[1] = loadGlfwImage(images, "images/icon-32.png");
					garbage[2] = loadGlfwImage(images, "images/icon-24.png");
					garbage[3] = loadGlfwImage(images, "images/icon-16.png");
				} catch (IOException e) {
					log.error("Failed to load all favicons", e);
				}
				images.flip();
//				glfwSetWindowIcon(window, images);
			}
			for (NativeResource nr : garbage) {
				if (nr != null) nr.free();
			}

			log.debug("OpenGL {}", glGetString(GL_VERSION));
			log.debug("bits({}r {}g {}b {}a {}d {}s) samples({})",
					glGetInteger(GL_RED_BITS), glGetInteger(GL_GREEN_BITS), glGetInteger(GL_BLUE_BITS), glGetInteger(GL_ALPHA_BITS),
					glGetInteger(GL_DEPTH_BITS), glGetInteger(GL_STENCIL_BITS), glcap.GL_ARB_multisample ? glGetInteger(ARBMultisample.GL_SAMPLES_ARB) : "unsupported");
			log.debug("{}", glGetString(GL_RENDERER));

			glDisable(GL_LIGHTING);
			glDisable(GL_BLEND);
			glEnable(GL_ALPHA_TEST);
			glAlphaFunc(GL_GREATER, 0.15f);
			glDisable(GL_CULL_FACE);

			// ResourceCache's GL-touching methods (e.g. getFont, getTexture) aren't
			// thread-safe, so we have to collect the resources on this thread then
			// pass them over to our loader render thread

			// We use a loader render thread at all because lots of important things
			// can only be called on the main thread

			// (We could also use the newly created Context in the other thread to
			//  load the resources, but then we'd double-load at least the font)

			Font _loaderFont = null;
			GLTexture2D _loaderBrand = null;
			try {
				if (!"$none".equals(Distribution.LOADING_FONT)) {
					_loaderFont = ResourceCache.obtain(context).getFont(new Identifier(Distribution.ID, Distribution.LOADING_FONT));
				}
				_loaderBrand = ResourceCache.obtain(context).getTexture(new Identifier(Distribution.ID, "images/brand.png"));
			} catch (ResourceNotFoundException e1) {
				log.error("Failed to load resources for loader", e1);
			}
			final Font loaderFont = _loaderFont;
			final GLTexture2D loaderBrand = _loaderBrand;

			glfwMakeContextCurrent(NULL);

			loadingThread = new Thread(() -> {
				rpmalloc_thread_initialize();
				glfwMakeContextCurrent(window);
				GL.createCapabilities();
				Context<ClientEngine> context = Context.createNew(this);
				drawLoaderBare(context, loaderBrand, loaderFont);
				while (loading) {
					glViewport(0, 0, framebufferWidth, framebufferHeight);
					if (glfwWindowShouldClose(window)) {
						System.exit(0);
					}
					drawLoaderBare(context, loaderBrand, loaderFont);
					glfwSwapBuffers(window);
				}
				glfwMakeContextCurrent(NULL);
				rpmalloc_thread_finalize();
			}, "Loading screen thread");
			loadingThread.start();

			loadingMessage = "";

			loadingMessage = "Initializing audio...";
			((SoundManagerInternalAccess)SoundManager.obtain(context)).init();

			if (defaultAddon != null) {
				loadingMessage = "Initializing addons...";
				defaultAddon.load(context, this::setLoadingMessage);
			}

			loadingMessage = "Done";

			loading = false;
			try {
				while (loadingThread.isAlive()) {
					loadingThread.join();
				}
			} catch (InterruptedException e) {
				throw new AssertionError(e);
			}
			loadingThread = null;

			glfwMakeContextCurrent(window);

			KSysguardManager.start(context);

			glfwSetCursorEnterCallback(window, retain((w, entered) -> {
				mouseInWindow = entered;
			}));

			glfwSetCursorPosCallback(window, retain((w, x, y) -> {
				mousePosChanged = true;
				lastMouseX = x;
				lastMouseY = y;
				mousePixelScale = canvasPixelScale;
			}));
			glfwSetMouseButtonCallback(window, retain((w, button, action, mods) -> {
				double mx = lastMouseX/canvasPixelScale;
				double my = lastMouseY/canvasPixelScale;
				if (action == GLFW_RELEASE) {
					pendingEvents.add(DefaultEvent.mouseUp(context, button, mx, my, KeyModifiers.fromGlfw(mods)).setNotFake());
					if (button == 0) {
						pendingEvents.add(DefaultEvent.click(context, mx, my, KeyModifiers.fromGlfw(mods)).setNotFake());
					} else if (button == 1 || (button == 0 && Platform.isMac() && KeyModifiers.fromGlfw(mods).isControlHeld())) {
						pendingEvents.add(DefaultEvent.alternateClick(context, mx, my, KeyModifiers.fromGlfw(mods)).setNotFake());
					} else if (button == 3) {
						pendingEvents.add(DefaultEvent.back(context, mx, my, KeyModifiers.fromGlfw(mods)).setNotFake());
					} else if (button == 4) {
						pendingEvents.add(DefaultEvent.forward(context, mx, my, KeyModifiers.fromGlfw(mods)).setNotFake());
					}
				} else if (action == GLFW_PRESS) {
					pendingEvents.add(DefaultEvent.mouseDown(context, button, mx, my, KeyModifiers.fromGlfw(mods)).setNotFake());
				}
			}));

			glfwSetKeyCallback(window, retain((window, key, scancode, action, mods) -> {
				if (key == GLFW_KEY_Q) {
					LayerController lc = LayerController.obtain(context);
					if (lc.getLayers().isEmpty() && lc.getRoot() == null) {
						glfwSetWindowShouldClose(window, true);
						return;
					}
				}
				// recycling the KeyModifiers is handled by DefaultEvent's recycle method
				KeyModifiers b = KeyModifiers.fromGlfw(mods);
				Key ck = Key.fromGlfwKeyCode(key);
				if (action == GLFW_PRESS) {
					pendingEvents.add(DefaultEvent.keyDown(context, ck, scancode, b).setNotFake());
				} else if (action == GLFW_RELEASE) {
					pendingEvents.add(DefaultEvent.keyUp(context, ck, scancode, b).setNotFake());
				} else if (action == GLFW_REPEAT) {
					pendingEvents.add(DefaultEvent.keyRepeat(context, ck, scancode, b).setNotFake());
				}
			}));

			glfwSetCharCallback(window, retain((window, codepoint) ->  {
				pendingEvents.add(DefaultEvent.textEntered(context, codepoint).setNotFake());
			}));

			glfwSetScrollCallback(window, retain((window, xoffset, yoffset) -> {
				double mx = lastMouseX/canvasPixelScale;
				double my = lastMouseY/canvasPixelScale;
				pendingEvents.add(DefaultEvent.scroll(context, mx, my, xoffset, yoffset).setNotFake());
			}));

			glfwSetWindowFocusCallback(window, retain((window, focused) -> {
				if (focused) {
					pendingEvents.add(DefaultEvent.focusGained());
				} else {
					pendingEvents.add(DefaultEvent.focusLost());
				}
			}));

			glfwSetInputMode(window, GLFW_LOCK_KEY_MODS, GLFW_TRUE);

			if (defaultAddon != null)
				defaultAddon.init(context);

			if (glcap.GL_EXT_framebuffer_object && glcap.GL_EXT_framebuffer_blit && glcap.GL_ARB_shader_objects && glcap.GL_ARB_fragment_program && glcap.GL_EXT_packed_depth_stencil) {
				setupBlur();
			} else {
				log.info("One or more of EXT_framebuffer_object, EXT_framebuffer_blit, EXT_packed_depth_stencil, ARB_shader_objects, or ARB_fragment_program is unsupported. Blur will be disabled.");
			}

			while (!glfwWindowShouldClose(window)) {
				glfwMakeContextCurrent(window);
				mainLoop();
			}

			log.info("Shutting down");

			if (defaultAddon != null)
				defaultAddon.exit(context);

			glfwDestroyWindow(window);
			window = -1;

			if (alcGetCurrentContext() != NULL) {
				((SoundManagerInternalAccess)SoundManager.obtain(context)).stop();
			}
		} finally {
			KSysguardManager.stop();
			if (loadingThread != null) {
				loading = false;
				while (loadingThread.isAlive()) {
					try {
						loadingThread.join();
					} catch (InterruptedException e) {}
				}
			}
			((SoundManagerInternalAccess)SoundManager.obtain(context)).destroy();
			if (window != -1) {
				glfwDestroyWindow(window);
			}
			glfwTerminate();
			rpmalloc_thread_finalize();
			rpmalloc_finalize();
			log.info("Goodbye");
		}
		return 0;
	}

	@Override
	public EngineType getType() {
		return EngineType.SOLE_CLIENT;
	}

	public void setLoadingMessage(String loadingMessage) {
		this.loadingMessage = loadingMessage;
	}

	public void quit() {
		glfwSetWindowShouldClose(window, true);
	}

	private void setupBlur() {
		kawaseDownsample = GLProgram.allocate();
		try {
			kawaseDownsample.attachShader(ResourceCache.obtain(context).getShader(ShaderType.FRAGMENT, new Identifier("chipper", "shaders/kawase-downsample.fs")));
		} catch (ResourceNotFoundException e) {
			kawaseDownsample.free();
			log.warn("Failed to get downsample shader. Blur will be disabled.", e);
			return;
		}

		kawaseUpsample = GLProgram.allocate();
		try {
			kawaseUpsample.attachShader(ResourceCache.obtain(context).getShader(ShaderType.FRAGMENT, new Identifier("chipper", "shaders/kawase-upsample.fs")));
		} catch (ResourceNotFoundException e) {
			kawaseDownsample.free();
			kawaseUpsample.free();
			log.warn("Failed to get upsample shader. Blur will be disabled.", e);
			return;
		}

		try {
			kawaseDownsample.link();
		} catch (GLLinkException e) {
			log.warn("Failed to link downsample program. Blur will be disabled.", e);
			return;
		}
		try {
			kawaseUpsample.link();
		} catch (GLLinkException e) {
			log.warn("Failed to link upsample program. Blur will be disabled.", e);
			return;
		}

		blurTex1 = glGenTextures();
		blurTex2 = glGenTextures();

		glBindTexture(GL_TEXTURE_2D, blurTex1);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

		glBindTexture(GL_TEXTURE_2D, blurTex2);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

		blurFbo1 = glGenFramebuffersEXT();
		blurFbo2 = glGenFramebuffersEXT();

		glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, blurFbo1);
		glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT, GL_TEXTURE_2D, blurTex1, 0);
		glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, blurFbo2);
		glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT, GL_TEXTURE_2D, blurTex2, 0);

		glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
	}

	public void addThingToEnteredList(InputEventProcessor ep) {
		enteredThings.add(ep);
	}

	public boolean isThingInEnteredList(InputEventProcessor ep) {
		return enteredThings.contains(ep);
	}

	public void setBlurStrength(@IntRange(from=0, to=100) int blurStrength) {
		this.blurStrength = blurStrength;
	}

	public int getBlurStrength() {
		return blurStrength;
	}

	public void setGlassColor(ProtoColor color) {
		this.glassColor = color;
	}

	public void setGlassOpacity(double opacity) {
		this.glassOpacity = opacity;
	}

	public ProtoColor getGlassColor() {
		return glassColor;
	}

	public double getGlassOpacity() {
		return glassOpacity;
	}

	public void setCanvasPixelScaleSetting(int canvasPixelScaleSetting) {
		this.canvasPixelScaleSetting = canvasPixelScaleSetting;
	}

	public boolean getLimitResolution() {
		return limitResolution;
	}

	public void setLimitResolution(boolean limitResolution) {
		this.limitResolution = limitResolution;
	}

	public boolean getLimitResolutionLinear() {
		return limitResolutionLinear;
	}

	public boolean getDrawMouseOnCanvas() {
		return drawMouseOnCanvas;
	}

	public void setLimitResolutionLinear(boolean limitResolutionLinear) {
		this.limitResolutionLinear = limitResolutionLinear;
	}

	public void setDrawMouseOnCanvas(boolean drawMouseOnCanvas) {
		this.drawMouseOnCanvas = drawMouseOnCanvas;
	}

	public int getCanvasPixelScaleSetting() {
		return canvasPixelScaleSetting;
	}

	public int getCanvasPixelScale() {
		return canvasPixelScale;
	}

	@Override
	public Addon getDefaultAddon() {
		return defaultAddon;
	}

	/**
	 * Switch to the given GameState, performing proper cleanup of the current GameState, if any.
	 */
	public void switchToState(GameState state) {
		// TODO error handling
		if (currentState != null) {
			currentState.tearDown(context);
		}
		state.setUp(context);
		if (state.isReady()) {
			this.currentState = state;
		}
	}

	private void drawLoaderBare(Context<ClientEngine> ctx, GLTexture2D brand, Font font) {
		int ww = windowWidth/canvasPixelScale;
		int wh = windowHeight/canvasPixelScale;
		((CanvasInternalAccess)Canvas.obtain(ctx)).setSize(ww, wh);
		prepare2D(ww, wh);
		drawLoader(ctx, brand, font, true, 1);
		cleanup2D();
	}

	private boolean tryCreateWindow() {
		window = glfwCreateWindow(1280, 720, Distribution.ID, NULL, NULL);
		windowWidth = 1280;
		windowHeight = 720;
		if (window == NULL) {
			return false;
		}
		int[] fw = {0}, fh = {0};
		glfwGetFramebufferSize(window, fw, fh);
		framebufferWidth = fw[0];
		framebufferHeight = fh[0];
		return true;
	}

	private NativeResource loadGlfwImage(GLFWImage.Buffer buf, String path) throws IOException {
		BufferedImage img = ResourceCache.obtain(context)
				.loadImage(new Identifier(Distribution.ID, path));
		int w = img.getWidth();
		int h = img.getHeight();
		ByteBuffer rawData = img.getRawABGRData();
		try (GLFWImage gi = GLFWImage.malloc()) {
			gi.set(w, h, rawData);
			buf.put(gi);
		}
		return img;
	}

	// TODO this should probably go in some sort of Display component
	public int getWindowWidth() {
		return windowWidth;
	}

	public int getWindowHeight() {
		return windowHeight;
	}

	private int frames = 0;
	private long nsPerFrame = 0;
	private double lastFrameMetricsUpdate = 0;

	private double firstFrame = 0;

	private int fps;
	private double mspf;

	private ByteBuffer noticeBuffer;

	public int getFramesPerSecond() {
		return fps;
	}

	public double getMillisPerFrame() {
		return mspf;
	}

	private void mainLoop() {
		if (lastFrameMetricsUpdate == 0) lastFrameMetricsUpdate = MonotonicTime.seconds();
		if (firstFrame == 0) firstFrame = MonotonicTime.seconds();
		if (MonotonicTime.seconds()-lastFrameMetricsUpdate > 1) {
			fps = frames;
			mspf = frames == 0 ? 1000 : (nsPerFrame/frames)/1000000D;
			frames = 0;
			nsPerFrame = 0;
			lastFrameMetricsUpdate = MonotonicTime.seconds();
		}
		glfwPollEvents();
		KSysguardManager.frameStart();
		glViewport(0, 0, framebufferWidth, framebufferHeight);
		glClearColor(0, 0, 0, 1);
		glClearDepth(0);
		glClearStencil(0);
		glClear(GL_COLOR_BUFFER_BIT | GL_STENCIL_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		long frameStart = System.nanoTime();

		if (currentState != null) {
			if (currentState.isReady()) {
				currentState.preFrame(context);
			}
			if (currentState.isErrored()) {
				currentState = null;
			}
		}
		((SoundManagerInternalAccess)SoundManager.obtain(context)).update();
		if (defaultAddon != null)
			defaultAddon.preFrame(context);

		if (windowWidth < 480 || windowHeight < 360) {
			glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
			ProtoColor bg = ProtoColor.MaterialColors.DEEP_ORANGE_900;
			if (GL.getCapabilities().GL_ARB_vertex_array_object) {
				ARBVertexBufferObject.glBindBufferARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, 0);
			}
			glClearColor((float)bg.getRed(), (float)bg.getGreen(), (float)bg.getBlue(), 1);
			glClear(GL_COLOR_BUFFER_BIT);
			prepare2D(windowWidth, windowHeight);
			if (noticeBuffer == null) {
				noticeBuffer = memAlloc(32768);
			}
			drawStbEasyFont("Cowardly refusing to draw on a canvas", (windowWidth-211)/2, (windowHeight/2)-8);
			drawStbEasyFont("under the minimum size of 480x360", (windowWidth-188)/2, (windowHeight/2)+2);
			cleanup2D();
			glfwSwapBuffers(window);
			return;
		}

		lastLimitResolution = limitResolution;

		if (defaultAddon == null) {
			canvasPixelScaleSetting = 2;
		}
		
		if (lastCanvasPixelScaleSetting != canvasPixelScaleSetting) {
			lastCanvasPixelScaleSetting = canvasPixelScaleSetting;
			sizeChanged = true;
		}
		
		canvasPixelScale = canvasPixelScaleSetting;

		double ww = windowWidth/(double)canvasPixelScale;
		double wh = windowHeight/(double)canvasPixelScale;

		while (ww < 480 || wh < 360) {
			if (canvasPixelScale == 1) {
				// nothing more we can do
				break;
			}
			canvasPixelScale--;
			ww = windowWidth/(double)canvasPixelScale;
			wh = windowHeight/(double)canvasPixelScale;
		}

		int wwDown = (int)(ww);
		int whDown = (int)(wh);
		int wwUp = (int)(ww+0.5);
		int whUp = (int)(wh+0.5);

		if (mousePixelScale != canvasPixelScale && mouseInWindow) {
			double x = (lastMouseX/mousePixelScale)*canvasPixelScale;
			double y = (lastMouseY/mousePixelScale)*canvasPixelScale;
			if (Double.isFinite(x) && Double.isFinite(y)) {
				lastMouseX = x;
				lastMouseY = y;
				glfwSetCursorPos(window, x, y);
				mousePosChanged = true;
				mousePixelScale = canvasPixelScale;
			}
		}

		if (lastLimitResolution) {
			if (limitFbo == -1) {
				limitFbo = glGenFramebuffersEXT();
				limitFboColor = glGenRenderbuffersEXT();
				limitFboDepthStencil = glGenRenderbuffersEXT();
				limitFboScratchTex = glGenTextures();
				sizeChanged = true;
			}
			glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, limitFbo);
			if (sizeChanged) {
				glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, limitFbo);
				glBindRenderbufferEXT(GL_RENDERBUFFER_EXT, limitFboColor);
				glRenderbufferStorageEXT(GL_RENDERBUFFER_EXT, GL_RGB8, wwUp, whUp);
				glBindRenderbufferEXT(GL_RENDERBUFFER_EXT, limitFboDepthStencil);
				glRenderbufferStorageEXT(GL_RENDERBUFFER_EXT, GL_DEPTH24_STENCIL8_EXT, wwUp, whUp);

				glFramebufferRenderbufferEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT, GL_RENDERBUFFER_EXT, limitFboColor);
				glFramebufferRenderbufferEXT(GL_FRAMEBUFFER_EXT, GL_DEPTH_ATTACHMENT_EXT, GL_RENDERBUFFER_EXT, limitFboDepthStencil);
				glFramebufferRenderbufferEXT(GL_FRAMEBUFFER_EXT, GL_STENCIL_ATTACHMENT_EXT, GL_RENDERBUFFER_EXT, limitFboDepthStencil);

				glBindTexture(GL_TEXTURE_2D, limitFboScratchTex);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			}
			glViewport(0, 0, wwUp, whUp);
		} else if (limitFbo != -1) {
			glDeleteFramebuffersEXT(limitFbo);
			glDeleteRenderbuffersEXT(limitFboColor);
			glDeleteRenderbuffersEXT(limitFboDepthStencil);
			glDeleteTextures(limitFboScratchTex);
			limitFbo = limitFboColor = limitFboDepthStencil = limitFboScratchTex = -1;
		}

		LayerController lc = LayerController.obtain(context);
		// making a copy of the layers list allows event handlers to modify the
		// layer list without throwing a ConcurrentModificationException - it
		// effectively delays any changes for a frame
		layersCopy.clear();
		layersCopy.addAll(lc.getLayersReverse());
		boolean anyLayersOpaque = false;
		widgetsToRender.clear();
		frostRegions.clear();
		for (Widget w : layersCopy) {
			if (w.needsLayout() || sizeChanged) {
				w.preLayout();
				w.layout(wwDown, whDown);
			}
			widgetsToRender.add(0, w);
			if (w.isOpaque()) {
				anyLayersOpaque = true;
				break;
			}
		}
		Renderable root = lc.getRoot();
		Canvas c = Canvas.obtain(context);
		if (!anyLayersOpaque && root != null) {
			frostRegions.clear();
			boolean bottommost = true;
			for (Widget w : widgetsToRender) {
				w.getFrostRegions(ww, wh, frostRegions, bottommost);
				bottommost = false;
			}
			root.render(context);
			glDisable(GL_TEXTURE_2D);
			if (blurFbo1 != -1) {
				boolean noFrostRegions = true;
				try (RectD screenRect = RectD.fromSize(0, 0, ww, wh)) {
					for (RectD reg : frostRegions) {
						if (reg.intersects(screenRect)) {
							noFrostRegions = false;
							break;
						}
					}
				}
				if (!noFrostRegions && !root.isPlain() && blurStrength > 0 && glassOpacity < 1) {
					glEnable(GL_TEXTURE_2D);
					int str = FastMath.clamp(blurStrength, 0, 100);
					int iter;
					float ofs;
					double div = 1;
					if (str <= 15) {
						iter = 2;
						ofs = (float)FastMath.lerp(0, 5, str/15D);
					} else if (str <= 35) {
						iter = 3;
						ofs = (float)FastMath.lerp(3.5, 7, (str-15)/20D);
					} else if (str <= 45) {
						iter = 4;
						ofs = (float)FastMath.lerp(3, 5, (str-35)/10D);
						div = 2;
					} else if (str <= 55) {
						iter = 5;
						ofs = (float)FastMath.lerp(2.5, 5.5, (str-45)/10D);
						div = 3;
					} else if (str <= 75) {
						iter = 8;
						ofs = (float)FastMath.lerp(3, 7, (str-55)/20D);
						div = 4;
					} else {
						iter = 8;
						ofs = (float)FastMath.lerp(6, 7, (str-75)/25D);
						div = 5;
					}

					int width = (limitResolution ? wwUp : windowWidth);
					int height = (limitResolution ? whUp : windowHeight);

					int widthDiv = FastMath.ceil(width/div);
					int heightDiv = FastMath.ceil(height/div);

					prepare2D(1, 1);
					try {
						glBindTexture(GL_TEXTURE_2D, blurTex1);
						glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, widthDiv, heightDiv, 0, GL_RGB, GL_UNSIGNED_BYTE, NULL);

						glReadBuffer(lastLimitResolution ? GL_COLOR_ATTACHMENT0_EXT : GL_BACK);
						glBindTexture(GL_TEXTURE_2D, blurTex2);
						glCopyTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, 0, 0, width, height, 0);

						glBindFramebufferEXT(GL_READ_FRAMEBUFFER_EXT, blurFbo2);
						glBindFramebufferEXT(GL_DRAW_FRAMEBUFFER_EXT, blurFbo1);
						glBlitFramebufferEXT(0, 0, width, height, 0, 0, widthDiv, heightDiv, GL_COLOR_BUFFER_BIT, GL_LINEAR);

						glBindTexture(GL_TEXTURE_2D, blurTex2);
						glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, widthDiv, heightDiv, 0, GL_RGB, GL_UNSIGNED_BYTE, NULL);
						glBindFramebufferEXT(GL_READ_FRAMEBUFFER_EXT, blurFbo1);
						glBindFramebufferEXT(GL_DRAW_FRAMEBUFFER_EXT, blurFbo2);
						glBlitFramebufferEXT(0, 0, widthDiv, heightDiv, 0, 0, widthDiv, heightDiv, GL_COLOR_BUFFER_BIT, GL_LINEAR);

						kawaseDown(c, kawaseDownsample, iter, div, ofs, widthDiv, heightDiv);
						kawaseUp(c, kawaseUpsample, iter, div, ofs, widthDiv, heightDiv);

						if (lastLimitResolution) {
							glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, limitFbo);
						} else {
							glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
						}
						GLProgram.NONE.use();
						glBindTexture(GL_TEXTURE_2D, blurTex2);
						glDepthMask(false);
					} finally {
						cleanup2D();
					}
					prepare2D(ww, wh);
					try {
						for (RectD rd : frostRegions) {
							double minU = (rd.getLeft()/ww);
							double minV = 1-((rd.getTop()/wh));
							double maxU = (rd.getRight()/ww);
							double maxV = 1-((rd.getBottom()/wh));
							c.startShape().quads().flat().color(0xFFFFFFFF)
								.next().vertex(rd.getLeft(), rd.getTop()).tex(minU, minV)
								.next().vertex(rd.getRight(), rd.getTop()).tex(maxU, minV)
								.next().vertex(rd.getRight(), rd.getBottom()).tex(maxU, maxV)
								.next().vertex(rd.getLeft(), rd.getBottom()).tex(minU, maxV)
							.end();
						}
						glDepthMask(true);
					} finally {
						cleanup2D();
					}
				}
			}
			glEnable(GL_DEPTH_TEST);
			glDisable(GL_TEXTURE_2D);
			glDepthFunc(GL_LESS);
			prepare2D(ww, wh);
			try {
				for (RectD rd : frostRegions) {
					c.startShape().quads().flat().color(glassColor, glassOpacity)
						.next().vertex(rd.getLeft(), rd.getTop())
						.next().vertex(rd.getRight(), rd.getTop())
						.next().vertex(rd.getRight(), rd.getBottom())
						.next().vertex(rd.getLeft(), rd.getBottom())
					.end();
				}
			} finally {
				cleanup2D();
			}
			for (RectD r : frostRegions) {
				r.recycle();
			}
			frostRegions.clear();
			glDisable(GL_DEPTH_TEST);
		}

		glDisable(GL_TEXTURE_2D);
		prepare2D(ww, wh);
		try {
			((CanvasInternalAccess)c).setSize(wwDown, whDown);
			for (Widget w : widgetsToRender) {
				try (Canvas.State s = c.pushState()) {
					w.draw(context, wwDown, whDown, c);
				}
			}
			if (root == null && widgetsToRender.isEmpty()) {
				ProtoColor bg = neverRenderedAnything ? ProtoColor.MaterialColors.DEEP_PURPLE_700 : ProtoColor.MaterialColors.BLUE_900;
				if (GL.getCapabilities().GL_ARB_vertex_array_object) {
					ARBVertexBufferObject.glBindBufferARB(ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB, 0);
				}
				glClearColor((float)bg.getRed(), (float)bg.getGreen(), (float)bg.getBlue(), 1);
				glClear(GL_COLOR_BUFFER_BIT);
				if (noticeBuffer == null) {
					noticeBuffer = memAlloc(32768);
				}
				if (neverRenderedAnything) {
					drawStbEasyFont("Welcome to Chipper!", 4, 4);
					drawStbEasyFont("Chipper is just an engine, so on its own, all it can do is show this screen.", 4, 16);
					//if (addons.isEmpty()) {
					//	drawStbEasyFont("Write an addon and load it to get started!", 4, 40);
					if (defaultAddon == null) {
						drawStbEasyFont("Chipper is incomplete and currently hardcodes the client's addon.", 4, 40);
						drawStbEasyFont("Change line 112 in ClientEngine to instanciate your Addon subclass.", 4, 52);
						drawStbEasyFont("(This desperately needs improving.)", 4, 64);
					} else {
						drawStbEasyFont("Your addon needs to put a Widget on the layer stack, or set a Renderable.", 4, 40);
						drawStbEasyFont("(Probably both, they're for different purposes.) Check out LayerController for how to do that.", 4, 52);
					}
				} else {
					drawStbEasyFont("Hmmm.", 4, 4);
					drawStbEasyFont("There's nothing on the layer stack and no root renderable.", 4, 16);

					drawStbEasyFont("This probably shouldn't happen!", 4, 40);
					drawStbEasyFont("If this is your addon/game and you did this on purpose, and you", 4, 64);
					drawStbEasyFont("want a black screen, you should set an empty Renderable.", 4, 76);

					drawStbEasyFont("As you can see, no Renderable and no widgets is considered an error.", 4, 100);
				}
				drawStbEasyFont("Press Q to exit. Chipper v"+Distribution.CHIPPER_VERSION+". "+fps+" f/s, "+mspf+" ms/f.", 4, whDown-14);
			} else {
				neverRenderedAnything = false;
			}

			double mx = lastMouseX/canvasPixelScale;
			double my = lastMouseY/canvasPixelScale;

			if (mousePosChanged) {
				pendingEvents.add(DefaultEvent.move(context, mx, my).setNotFake());
			}

			for (DefaultEvent de : pendingEvents) {
				try {
					boolean consumed = false;
					for (Widget w : layersCopy) {
						if (de.isRelevant(w)) {
							if (w.processEvent(de) == EventResponse.ACCEPT) {
								consumed = true;
								break;
							}
						}
					}
					if (!consumed) {
						if (root instanceof WindowInputListener) {
							if (de.isRelevant((WindowInputListener)root)) {
								((WindowInputListener) root).processEvent(de);
							}
						}
					}
				} finally {
					de.recycle();
				}
			}
			pendingEvents.clear();

			CursorType type = CursorType.DEFAULT;

			try (RectI scratch = RectI.get()) {
				Iterator<InputEventProcessor> iter = enteredThings.iterator();
				while (iter.hasNext()) {
					InputEventProcessor ep = iter.next();
					if (ep instanceof Renderable && root != ep) {
						iter.remove();
						continue;
					}
					if (ep instanceof WindowInputListener || (ep instanceof Widget && layersCopy.contains(ep))) {
						if (!mouseInWindow) {
							ep.processEvent(DefaultEvent.leave(context, mx, my));
							iter.remove();
						}
					} else if (!mouseInWindow || (ep instanceof Widget && ((Widget)ep).getParent() == null)) {
						ep.processEvent(DefaultEvent.leave(context, mx, my));
						iter.remove();
					} else if (ep instanceof Widget) {
						Widget w = (Widget)ep;
						Container parent = w.getParent();
						parent.getSize(w, scratch);
						Widget child = w;
						Container cursor = parent;
						try (Point2D pos = Point2D.get()) {
							while (cursor != null) {
								cursor.unadjust(child, pos);
								child = cursor;
								cursor = cursor.getParent();
								/*
								 * if we reached the root of this hierarchy, but we
								 * didn't reach a root layer, then one of the widget's
								 * parents must have been removed from the widget
								 * hierarchy.
								 */
								if (cursor == null && !LayerController.obtain(context).getLayers().contains(child)) {
									ep.processEvent(DefaultEvent.leave(context, mx, my));
									iter.remove();
									return;
								}
							}
							try (RectD scratchD = scratch.asRectD()) {
								scratchD.setX(pos.getX());
								scratchD.setY(pos.getY());
								if (!scratchD.intersects(mx, my)) {
									ep.processEvent(DefaultEvent.leave(context, mx-pos.getX(), my-pos.getY()));
									iter.remove();
								} else {
									CursorType wt = w.getCursorType(mx-pos.getX(), my-pos.getY());
									if (wt != null) {
										type = wt;
									}
								}
							}
						}
					}
				}
			}
			for (Widget w : lc.getLayersReverse()) {
				CursorType override = w.getCursorType(mx, my);
				if (override != null) {
					type = override;
					break;
				}
			}
			if (drawMouseOnCanvas && type.hasBeenCustomized()) {
				if (lastCursor != -1) {
					lastCursor = -1;
					glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
				}
				if (mouseInWindow) {
					glEnable(GL_TEXTURE_2D);
					type.createTexture();
					int x1 = (int)(lastMouseX/canvasPixelScale) - (type.getHotspotX()/canvasPixelScale);
					int y1 = (int)(lastMouseY/canvasPixelScale) - (type.getHotspotY()/canvasPixelScale);
					int x2 = x1+(type.getWidth()/canvasPixelScale);
					int y2 = y1+(type.getHeight()/canvasPixelScale);
					glBindTexture(GL_TEXTURE_2D, type.getTexture());
					glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, limitResolutionLinear ? GL_LINEAR : GL_NEAREST);
					c.startShape().quads().flat().color(0xFFFFFFFF)
						.next().vertex(x1, y1).tex(0, 0)
						.next().vertex(x2, y1).tex(1, 0)
						.next().vertex(x2, y2).tex(1, 1)
						.next().vertex(x1, y2).tex(0, 1)
					.end();
					glDisable(GL_TEXTURE_2D);
				}
			} else {
				if (type.unsafeGetCursorHandle() == NULL && type.glfwConst != 0) {
					type.replaceWithStandardCursor();
					//type.cursorHandle = glfwCreateStandardCursor(type.glfwConst);
				}
				long handle = type.unsafeGetCursorHandle();
				if (type == CursorType.NONE) {
					handle = -1;
				}
				if (handle != lastCursor) {
					lastCursor = handle;
					if (handle == -1) {
						glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
					} else {
						glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
						glfwSetCursor(window, type == null ? NULL : type.unsafeGetCursorHandle());
					}
				}
			}
			mousePosChanged = false;
			sizeChanged = false;

			if (MonotonicTime.seconds()-firstFrame < 1) {
				Font loaderFont = null;
				GLTexture2D loaderBrand = null;
				try {
					if (!"$none".equals(Distribution.LOADING_FONT)) {
						loaderFont = ResourceCache.obtain(context).getFont(new Identifier(Distribution.ID, Distribution.LOADING_FONT));
					}
					loaderBrand = ResourceCache.obtain(context).getTexture(new Identifier(Distribution.ID, "images/brand.png"));
				} catch (ResourceNotFoundException e1) {
					log.error("Failed to load resources for loader", e1);
				}
				drawLoader(context, loaderBrand, loaderFont, false, FastMath.ease(1, 0, MonotonicTime.seconds()-firstFrame));
			}
			// set alpha of entire framebuffer to 100%
			// this prevents recordings being too dark in OBS, or the window being oddly translucent
			// in places on esoteric window managers
			glColorMask(false, false, false, true);
			c.drawRect(0, 0, wwUp, whUp, 0xFF000000);
			glColorMask(true, true, true, true);
		} finally {
			cleanup2D();
			glEnable(GL_DEPTH_TEST);
		}

		if (lastLimitResolution) {
			glEnable(GL_TEXTURE_2D);
			glViewport(0, 0, framebufferWidth, framebufferHeight);
			glReadBuffer(GL_COLOR_ATTACHMENT0_EXT);
			glBindTexture(GL_TEXTURE_2D, limitFboScratchTex);
			glCopyTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, 0, 0, wwUp, whUp, 0);
			glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
			prepare2D(ww, wh);
			glBindTexture(GL_TEXTURE_2D, limitFboScratchTex);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, limitResolutionLinear ? GL_LINEAR : GL_NEAREST);
			c.startShape().quads().flat().color(0xFFFFFFFF)
				.next().vertex( 0,  0).tex(0, 1)
				.next().vertex(wwUp,  0).tex(1, 1)
				.next().vertex(wwUp, whUp).tex(1, 0)
				.next().vertex( 0, whUp).tex(0, 0)
			.end();
			cleanup2D();
			glDisable(GL_TEXTURE_2D);
		}

		frames++;
		nsPerFrame += System.nanoTime()-frameStart;
		KSysguardManager.frameEnd();
		if (defaultAddon != null)
			defaultAddon.postFrame(context);
		if (currentState != null) {
			if (currentState.isReady()) {
				currentState.postFrame(context);
			}
			if (currentState.isErrored()) {
				currentState = null;
			}
		}
		glfwSwapBuffers(window);
	}

	public boolean isKeyDown(int key) {
		return glfwGetKey(this.window, key)==GLFW_PRESS;
	}

	public boolean isFocused() {
		return true;
		//The following seems to cause an intermittent freeze for some reason
		//boolean visible = glfwGetWindowAttrib(this.window, GLFW_VISIBLE)==GLFW_TRUE;
		//boolean iconified = glfwGetWindowAttrib(this.window, GLFW_ICONIFIED)==GLFW_TRUE;
		//return visible && !iconified;
	}

	public void grabCursor() {
		if (mouseGrabbed) return;
		mouseGrabbed = true;
		glfwSetInputMode(this.window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
	}

	public void releaseCursor() {
		mouseGrabbed = false;
		glfwSetInputMode(this.window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
	}

	public boolean isCursorGrabbed() {
		return mouseGrabbed;
	}

	public int getMouseClick() {
		int left = glfwGetMouseButton(this.window, GLFW_MOUSE_BUTTON_1)==GLFW_PRESS ? 1 : 0;
		int right = glfwGetMouseButton(this.window, GLFW_MOUSE_BUTTON_2)==GLFW_PRESS ? 1 : 0;
		return left | (right << 1);
	}

	public boolean isMouseDown(int button) {
		return glfwGetMouseButton(this.window, button)==GLFW_PRESS;
	}

	public double getCursorX() {
		return this.lastMouseX;
	}

	public double getCursorY() {
		return this.lastMouseY;
	}

	/**
	 * For use with the "nothing to render" notice, when we have no Font
	 * resources to load.
	 */
	private void drawStbEasyFont(String str, int x, int y) {
		int count = stb_easy_font_print(x, y, str, null, noticeBuffer);
		glColor3f(1, 1, 1);
		glDisable(GL_TEXTURE_2D);
		glDisable(GL_DEPTH_TEST);
		glEnableClientState(GL_VERTEX_ARRAY);
		glVertexPointer(3, GL_FLOAT, (4*4), noticeBuffer);
		glDrawArrays(GL_QUADS, 0, count*4);
		glDisableClientState(GL_VERTEX_ARRAY);
	}

	private void kawaseDown(Canvas c, GLProgram shdr, int iter, double div, float ofs, int w, int h) {
		shdr.use();
		shdr.getUniform("offset").setFloat(ofs);
		for (int i = 1; i <= iter; i++) {
			kawaseInner(c, shdr, div, i, w, h);
		}
	}

	private void kawaseUp(Canvas c, GLProgram shdr, int iter, double div, float ofs, int w, int h) {
		shdr.use();
		shdr.getUniform("offset").setFloat(ofs);
		for (int i = iter; i >= 1; i--) {
			kawaseInner(c, shdr, div, i, w, h);
		}
	}

	private void kawaseInner(Canvas c, GLProgram shdr, double div, int i, int w, int h) {
		glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, blurFbo1);
		glBindTexture(GL_TEXTURE_2D, blurTex2);
		shdr.getUniform("halfpixel").setFloat(0.5f / w, 0.5f / h);
		shdr.getUniform("renderTextureSize").setFloat(w, h);
		c.startShape().quads().flat().color(0xFFFFFFFF)
			.next().vertex(0, 0)
			.next().vertex(1, 0)
			.next().vertex(1, 1)
			.next().vertex(0, 1)
		.end();
		glBindFramebufferEXT(GL_READ_FRAMEBUFFER_EXT, blurFbo1);
		glBindFramebufferEXT(GL_DRAW_FRAMEBUFFER_EXT, blurFbo2);
		glBlitFramebufferEXT(0, 0, windowWidth, windowHeight, 0, 0, windowWidth, windowHeight, GL_COLOR_BUFFER_BIT, GL_LINEAR);
	}

	private void drawLoader(Context<ClientEngine> ctx, GLTexture2D brand, Font font, boolean drawAccessories, double a) {
		Canvas c = Canvas.obtain(ctx);
		c.setBlendMode(BlendMode.NORMAL);
		c.drawRect(0, 0, c.getWidth(), c.getHeight(), Distribution.LOADING_BACKGROUND_COLOR, a);
		if (brand != null) {
			c.drawImage(brand, (c.getWidth()-64)/2, (c.getHeight()-64)/2, 64, 64, 1, 1, 1, a);
		}
		if (drawAccessories) {
			int bX = (c.getWidth()-48)/2;
			int bY = ((c.getHeight()-64)/2)+72;
			c.drawRect(bX, bY, 48, 2, Distribution.LOADING_ACCESSORY_COLOR, 0.25);
			int x = (int)((MonotonicTime.seconds()*96)%80)-16;
			int w = (int)((((FastMath.sin(MonotonicTime.seconds()*3))+1)/2)*24)+8;
			c.drawRect(bX+x-(w/2), bY, w, 2, Distribution.LOADING_ACCESSORY_COLOR, 0.75);
			c.drawRect(bX-128, bY, 128, 2, Distribution.LOADING_BACKGROUND_COLOR);
			c.drawRect(bX+48, bY, 128, 2, Distribution.LOADING_BACKGROUND_COLOR);
			if (font != null) {
				String msg = loadingMessage;
				font.drawString((c.getWidth()-font.measure(msg))/2, bY+8, msg, Distribution.LOADING_ACCESSORY_COLOR, 1);
			}
		}
	}

	public void prepare2D(double ww, double wh) {
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glOrtho(0, ww, wh, 0, 1, 100);
		glPushMatrix();
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		glTranslated(0, 0, -50);
		glPushMatrix();
	}

	public void cleanup2D() {
		glMatrixMode(GL_PROJECTION);
		glPopMatrix();
		glMatrixMode(GL_MODELVIEW);
		glPopMatrix();
	}

	private <T> T retain(T obj) {
		retain.add(obj);
		return obj;
	}

	private String getGlfwErrorString() {
		int errorCode = glfwGetError(scratchPointerBuffer);
		String error = "Unknown error";
		if (errorCode != 0) {
			error = scratchPointerBuffer.getStringUTF8();
		}
		return error+" (0x"+Integer.toHexString(errorCode)+")";
	}


}
