/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.component;

import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.collect.Lists;
import com.playsawdust.chipper.client.ClientEngine;
import com.playsawdust.chipper.client.Renderable;
import com.playsawdust.chipper.client.input.WindowInputListener;
import com.playsawdust.chipper.client.widget.EventResponse;
import com.playsawdust.chipper.client.widget.Widget;
import com.playsawdust.chipper.client.widget.container.Container;
import com.playsawdust.chipper.collect.unmodifiable.Unmodifiable;
import com.playsawdust.chipper.collect.unmodifiable.UnmodifiableList;
import com.playsawdust.chipper.component.Component;
import com.playsawdust.chipper.component.Context;
import com.playsawdust.chipper.component.Engine;
import com.playsawdust.chipper.component.Context.WhiteLotus;

/**
 * Manages the list of <i>layers</i> - the core components of the widget system.
 * Layers are drawn in order - this order is reflected in method names in this
 * class. Layers are just Widgets - as such, to implement a screen, you simply
 * extend your desired Container implementation, or you can use a completely
 * custom Widget if you want.
 */
public final class LayerController implements Component {

	private Renderable root = null;
	private final List<Widget> layers = Lists.newArrayList();
	private final UnmodifiableList<Widget> layersUnmodifiable = Unmodifiable.list(layers);
	private final UnmodifiableList<Widget> layersUnmodifiableReverse = Unmodifiable.list(Lists.reverse(layers));

	private LayerController(WhiteLotus lotus) {
		WhiteLotus.verify(lotus);
	}

	/**
	 * @return an unmodifiable view of the layer list, with the bottom-most
	 * 		layer first
	 */
	public UnmodifiableList<Widget> getLayers() {
		return layersUnmodifiable;
	}

	/**
	 * @return an unmodifiable view of the layer list, with the top-most
	 * 		layer first
	 */
	public UnmodifiableList<Widget> getLayersReverse() {
		return layersUnmodifiableReverse;
	}

	/**
	 * Returns the root renderable, which draws before any layers. This is good
	 * for menu backgrounds, the game world, etc. Additionally, it will be
	 * blurred if any layer reports a {@link Widget#getFrostRegions frost region}
	 * and the frosted glass effect is enabled.
	 * @return the root renderable
	 */
	public @Nullable Renderable getRoot() {
		return root;
	}

	/**
	 * Set the root renderable, which draws before (and therefore below) any layers.
	 * <p>
	 * If the given Renderable implements {@link WindowInputListener}, it will receive any input events
	 * that are not {@link EventResponse#ACCEPT caught} by any layers.
	 * @see #getRoot
	 * @param root the new root renderable
	 */
	public void setRoot(@Nullable Renderable root) {
		this.root = root;
	}

	/**
	 * Add the given widget to the top of the layer list, displaying over all
	 * current layers.
	 * @param widget the widget to add
	 */
	public void addToTop(Widget widget) {
		layers.add(widget);
	}

	/**
	 * Add the given widget to the bottom of the layer list, displaying under
	 * all current layers, but over the root renderable.
	 * @param widget the widget to add
	 */
	public void addToBottom(Widget widget) {
		layers.add(0, widget);
	}

	/**
	 * Clear the layer list and add the given widget to the layer list.
	 * @param widget the widget to display
	 */
	public void swap(Widget widget) {
		layers.clear();
		layers.add(widget);
	}

	/**
	 * Clear the layer list and add the given widget to the layer list.
	 * <p>
	 * This overload exists to resolve an ambiguity with the {@link #swap(Iterable)}
	 * method, which has undesirable behavior.
	 * @param widget the widget to display
	 */
	public void swap(Container widget) {
		swap((Widget)widget);
	}

	/**
	 * Clear the layer list and add the given widgets to the layer list, with
	 * the first widget being the bottom-most.
	 * @param widgets the widgets to display
	 */
	public void swap(Widget... widgets) {
		layers.clear();
		for (Widget w : widgets) {
			layers.add(w);
		}
	}

	/**
	 * Clear the layer list and add the given widgets to the layer list, with
	 * the first widget being the bottom-most.
	 * @param widgets the widgets to display
	 */
	public void swap(Iterable<Widget> widgets) {
		layers.clear();
		for (Widget w : widgets) {
			layers.add(w);
		}
	}

	/**
	 * Remove the given widget from the layer list if it is present.
	 * @param w the widget to remove
	 */
	public void remove(Widget w) {
		layers.remove(w);
	}

	/**
	 * Clear the layer list.
	 */
	public void clear() {
		layers.clear();
	}

	public static LayerController obtain(Context<? extends ClientEngine> ctx) {
		return ctx.getComponent(LayerController.class);
	}

	@Override
	public boolean compatibleWith(Engine engine) {
		return engine instanceof ClientEngine;
	}

}
