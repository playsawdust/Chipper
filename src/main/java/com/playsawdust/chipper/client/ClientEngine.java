package com.playsawdust.chipper.client;

import com.playsawdust.chipper.component.Engine;
import com.playsawdust.chipper.math.ProtoColor;

public interface ClientEngine extends Engine {

	void setLoadingMessage(String loadingMessage);

	void quit();

	/**
	 * Switch to the given GameState, performing proper cleanup of the current GameState, if any.
	 */
	void switchToState(GameState state);

	// TODO all of these need to get moved into some kind of settings manager
	void setBlurStrength(int blurStrength);

	int getBlurStrength();

	void setGlassColor(ProtoColor color);

	void setGlassOpacity(double opacity);

	ProtoColor getGlassColor();

	double getGlassOpacity();

	void setCanvasPixelScaleSetting(int canvasPixelScaleSetting);

	boolean getLimitResolution();

	void setLimitResolution(boolean limitResolution);

	boolean getLimitResolutionLinear();

	boolean getDrawMouseOnCanvas();

	void setLimitResolutionLinear(boolean limitResolutionLinear);

	void setDrawMouseOnCanvas(boolean drawMouseOnCanvas);

	int getCanvasPixelScaleSetting();

	int getCanvasPixelScale();
	
	int getWindowWidth();

	int getWindowHeight();

	int getFramesPerSecond();

	double getMillisPerFrame();

	boolean isKeyDown(int key);

	boolean isFocused();

	void grabCursor();

	void releaseCursor();

	boolean isCursorGrabbed();

	int getMouseClick();

	boolean isMouseDown(int button);

	double getCursorX();

	double getCursorY();

}