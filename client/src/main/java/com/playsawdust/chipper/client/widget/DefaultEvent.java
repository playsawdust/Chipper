/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.widget;

import java.security.BasicPermission;
import java.security.Permission;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.playsawdust.chipper.client.ClientEngine;
import com.playsawdust.chipper.client.EventProcessor;
import com.playsawdust.chipper.client.EventProcessor.Event;
import com.playsawdust.chipper.client.input.KeyModifiers;
import com.playsawdust.chipper.client.input.InputEventProcessor;
import com.playsawdust.chipper.client.input.Key;
import com.playsawdust.chipper.client.widget.container.Container;
import com.playsawdust.chipper.component.Context;
import com.playsawdust.chipper.exception.RecycledObjectException;
import com.playsawdust.chipper.math.Point2D;

import com.playsawdust.chipper.toolbox.pool.ObjectPool;
import com.playsawdust.chipper.toolbox.pool.PooledObject;

/**
 * Concrete implementation of Event for all events provided by the Widget system
 * itself. Manages every event with one class and an object pool, allowing
 * zero-allocation event processing in the majority of cases.
 */
public final class DefaultEvent implements Event, PooledObject {
	private static final Permission SET_NOT_FAKE = new BasicPermission("com.playsawdust.chipper.client.widget.DefaultEvent.setNotFake") {};

	private static final ObjectPool<DefaultEvent> pool = new ObjectPool<>(DefaultEvent::new);

	private boolean recycled = false;

	private @Nullable Context<ClientEngine> context;

	private EventType type;
	private boolean fake = true;

	/*
	 * having all these fields does waste a bit of space, but due to pooling we can get away with
	 * only ever having a handful of these objects in the entire heap, so it's worth it; it's also
	 * technically type-unsafe but as EventType is private and handles shuffling all this, and the
	 * only way to construct a DefaultEvent is through factory methods, it's /effectively/ type
	 * safe. the compiler just can't verify it for us because it's nuanced.
	 */

	private double double1;
	private double double2;
	private double double3;
	private double double4;

	private Object object1;
	private Object object2;

	private int int1;

	@Override
	public EventResponse visit(EventProcessor ep) {
		if (recycled) throw new RecycledObjectException(this);
		return type.visit(this, ep);
	}

	@Override
	public boolean isRelevant(EventProcessor ep) {
		if (recycled) throw new RecycledObjectException(this);
		return type.isRelevant(this, ep);
	}

	@Override
	public void enterContainer(Container container) {
		if (recycled) throw new RecycledObjectException(this);
		type.enterContainer(this, container);
	}

	@Override
	public void exitContainer(Container container) {
		if (recycled) throw new RecycledObjectException(this);
		type.exitContainer(this, container);
	}

	public DefaultEvent setNotFake() throws SecurityException {
		//AccessController.checkPermission(SET_NOT_FAKE); TODO
		if (recycled) throw new RecycledObjectException(this);
		fake = false;
		return this;
	}

	public boolean isFake() {
		if (recycled) throw new RecycledObjectException(this);
		return fake;
	}

	private DefaultEvent double1(double d) {
		this.double1 = d;
		return this;
	}

	private DefaultEvent double2(double d) {
		this.double2 = d;
		return this;
	}

	private DefaultEvent double3(double d) {
		this.double3 = d;
		return this;
	}

	private DefaultEvent double4(double d) {
		this.double4 = d;
		return this;
	}

	private DefaultEvent object1(Object o) {
		this.object1 = o;
		return this;
	}

	private DefaultEvent object2(Object o) {
		this.object2 = o;
		return this;
	}

	private DefaultEvent int1(int i) {
		this.int1 = i;
		return this;
	}

	@Override
	public void recycle() {
		recycled = true;
		context = null;
		double1 = double2 = double3 = double4 = 0;
		if (object1 instanceof PooledObject) {
			((PooledObject) object1).recycle();
		}
		if (object2 instanceof PooledObject) {
			((PooledObject) object2).recycle();
		}
		object1 = object2 = null;
		int1 = 0;
		type = null;
		fake = true;
		pool.recycle(this);
	}

	@Override
	public String toString() {
		if (recycled) return "DefaultEvent[RECYCLED]";
		return "DefaultEvent["+type.toString(this)+",fake="+fake+"]";
	}

	private static DefaultEvent get(Context<ClientEngine> context, EventType type) {
		DefaultEvent de = pool.get();
		de.recycled = false;
		de.context = context;
		de.fake = true;
		de.type = type;
		return de;
	}

	public static DefaultEvent mouseDown(Context<ClientEngine> context, int button, double x, double y, KeyModifiers mod) {
		return get(context, TYPE_MOUSE_DOWN).int1(button).double1(x).double2(y).object1(mod);
	}

	public static DefaultEvent mouseUp(Context<ClientEngine> context, int button, double x, double y, KeyModifiers mod) {
		return get(context, TYPE_MOUSE_UP).int1(button).double1(x).double2(y).object1(mod);
	}

	public static DefaultEvent click(Context<ClientEngine> context, double x, double y, KeyModifiers mod) {
		return get(context, TYPE_CLICK).double1(x).double2(y).object1(mod);
	}

	public static DefaultEvent alternateClick(Context<ClientEngine> context, double x, double y, KeyModifiers mod) {
		return get(context, TYPE_ALTERNATE_CLICK).double1(x).double2(y).object1(mod);
	}

	public static DefaultEvent scroll(Context<ClientEngine> context, double x, double y, double xscroll, double yscroll) {
		return get(context, TYPE_SCROLL).double1(x).double2(y).double3(xscroll).double4(yscroll);
	}

	public static DefaultEvent back(Context<ClientEngine> context, double x, double y, KeyModifiers mod) {
		return get(context, TYPE_BACK).double1(x).double2(y).object1(mod);
	}

	public static DefaultEvent forward(Context<ClientEngine> context, double x, double y, KeyModifiers mod) {
		return get(context, TYPE_FORWARD).double1(x).double2(y).object1(mod);
	}

	public static DefaultEvent enter(Context<ClientEngine> context, double x, double y) {
		return get(context, TYPE_ENTER).double1(x).double2(y);
	}

	public static DefaultEvent move(Context<ClientEngine> context, double x, double y) {
		return get(context, TYPE_MOVE).double1(x).double2(y);
	}

	public static DefaultEvent leave(Context<ClientEngine> context, double x, double y) {
		return get(context, TYPE_LEAVE).double1(x).double2(y);
	}

	public static DefaultEvent keyDown(Context<ClientEngine> context, Key key, int scancode, KeyModifiers mod) {
		return get(context, TYPE_KEY_DOWN).object1(key).int1(scancode).object2(mod);
	}

	public static DefaultEvent keyUp(Context<ClientEngine> context, Key key, int scancode, KeyModifiers mod) {
		return get(context, TYPE_KEY_UP).object1(key).int1(scancode).object2(mod);
	}

	public static DefaultEvent keyRepeat(Context<ClientEngine> context, Key key, int scancode, KeyModifiers mod) {
		return get(context, TYPE_KEY_REPEAT).object1(key).int1(scancode).object2(mod);
	}

	public static DefaultEvent textEntered(Context<ClientEngine> context, int codepoint) {
		return get(context, TYPE_TEXT_ENTERED).int1(codepoint);
	}

	public static DefaultEvent focusGained() {
		return get(null, TYPE_FOCUS_GAINED);
	}

	public static DefaultEvent focusLost() {
		return get(null, TYPE_FOCUS_LOST);
	}

	private static final EventType TYPE_MOUSE_DOWN = new ClickIshEventType("mouse_down", InputEventProcessor::onMouseDown);
	private static final EventType TYPE_MOUSE_UP = new ClickIshEventType("mouse_up", InputEventProcessor::onMouseUp);

	private static final EventType TYPE_CLICK = new ClickIshEventType("click", InputEventProcessor::onClick);
	private static final EventType TYPE_ALTERNATE_CLICK = new ClickIshEventType("alternate_click", InputEventProcessor::onAlternateClick);

	private static final EventType TYPE_SCROLL = new ClickIshEventType("scroll", InputEventProcessor::onScroll);

	private static final EventType TYPE_BACK = new ClickIshEventType("back", InputEventProcessor::onBack);
	private static final EventType TYPE_FORWARD = new ClickIshEventType("forward", InputEventProcessor::onForward);

	private static final EventType TYPE_ENTER = new EnterEventType("enter", InputEventProcessor::onEnter);
	private static final EventType TYPE_MOVE = new MoveEventType("move", InputEventProcessor::onMove);
	private static final EventType TYPE_LEAVE = new ClickIshEventType("leave", InputEventProcessor::onLeave);

	private static final EventType TYPE_KEY_DOWN = new KeyEventType("key_down", InputEventProcessor::onKeyDown);
	private static final EventType TYPE_KEY_UP = new KeyEventType("key_up", InputEventProcessor::onKeyUp);
	private static final EventType TYPE_KEY_REPEAT = new KeyEventType("key_repeat", InputEventProcessor::onKeyRepeat);

	private static final EventType TYPE_TEXT_ENTERED = new TextEnteredEventType("text_entered", InputEventProcessor::onTextEntered);

	private static final EventType TYPE_FOCUS_GAINED = new FocusEventType("focus_gained", InputEventProcessor::onFocusGained);
	private static final EventType TYPE_FOCUS_LOST = new FocusEventType("focus_lost", InputEventProcessor::onFocusLost);

	private interface EventType {
		EventResponse visit(DefaultEvent event, EventProcessor ep);
		boolean isRelevant(DefaultEvent event, EventProcessor ep);
		void enterContainer(DefaultEvent event, Container container);
		void exitContainer(DefaultEvent event, Container container);
		String toString(DefaultEvent event);
	}

	private interface ClickEventMethod {
		EventResponse on(InputEventProcessor w, double x, double y);
	}

	private interface KeyModifiersClickEventMethod {
		EventResponse on(InputEventProcessor w, double x, double y, KeyModifiers mod);
	}

	private interface KeyModifiersClickButtonEventMethod {
		EventResponse on(InputEventProcessor w, int button, double x, double y, KeyModifiers mod);
	}

	private interface ScrollEventMethod {
		EventResponse on(InputEventProcessor w, double x, double y, double xscroll, double yscroll);
	}

	private static class ClickIshEventType implements EventType {
		// TODO this is a disaster
		private final String type;
		private ClickEventMethod clickMethod;
		private ScrollEventMethod scrollMethod;
		private KeyModifiersClickEventMethod modMethod;
		private KeyModifiersClickButtonEventMethod modButtonMethod;
		private ClickIshEventType(String type, ClickEventMethod method) {
			this.type = type;
			this.clickMethod = method;
			this.scrollMethod = null;
			this.modMethod = null;
			this.modButtonMethod = null;
		}
		private ClickIshEventType(String type, ScrollEventMethod method) {
			this.type = type;
			this.clickMethod = null;
			this.scrollMethod = method;
			this.modMethod = null;
			this.modButtonMethod = null;
		}
		private ClickIshEventType(String type, KeyModifiersClickEventMethod method) {
			this.type = type;
			this.clickMethod = null;
			this.scrollMethod = null;
			this.modMethod = method;
			this.modButtonMethod = null;
		}
		private ClickIshEventType(String type, KeyModifiersClickButtonEventMethod method) {
			this.type = type;
			this.clickMethod = null;
			this.scrollMethod = null;
			this.modMethod = null;
			this.modButtonMethod = method;
		}
		@Override
		public EventResponse visit(DefaultEvent event, EventProcessor ep) {
			if (!(ep instanceof InputEventProcessor)) return EventResponse.PASS;
			InputEventProcessor iep = (InputEventProcessor)ep;
			try (Point2D point = Point2D.from(event.double1, event.double2)) {
				if (ep instanceof Widget) {
					Widget widget = (Widget)ep;
					if (widget.getParent() != null) {
						widget.getParent().adjust(widget, point);
					}
				}
				if (clickMethod != null) {
					return clickMethod.on(iep, point.getX(), point.getY());
				} else if (scrollMethod != null) {
					return scrollMethod.on(iep, point.getX(), point.getY(), event.double3, event.double4);
				} else if (modMethod != null) {
					return modMethod.on(iep, point.getX(), point.getY(), (KeyModifiers)event.object1);
				} else if (modButtonMethod != null) {
					return modButtonMethod.on(iep, event.int1, point.getX(), point.getY(), (KeyModifiers)event.object1);
				} else {
					throw new AssertionError();
				}
			}
		}
		@Override
		public boolean isRelevant(DefaultEvent event, EventProcessor ep) {
			if (ep instanceof Widget) {
				Widget widget = (Widget)ep;
				if (widget.getParent() != null) {
					return widget.getParent().hit(event.double1, event.double2).contains(widget);
				}
				return true;
			}
			return ep instanceof InputEventProcessor;
		}
		@Override
		public void enterContainer(DefaultEvent event, Container container) {
			try (Point2D point = Point2D.from(event.double1, event.double2)) {
				if (container.getParent() != null) {
					container.getParent().adjust(container, point);
				}
				event.double1 = point.getX();
				event.double2 = point.getY();
			}
		}
		@Override
		public void exitContainer(DefaultEvent event, Container container) {
			try (Point2D point = Point2D.from(event.double1, event.double2)) {
				if (container.getParent() != null) {
					container.getParent().unadjust(container, point);
				}
				event.double1 = point.getX();
				event.double2 = point.getY();
			}
		}
		@Override
		public String toString(DefaultEvent de) {
			if (clickMethod != null) {
				return "type="+type+",x="+de.double1+",y="+de.double2;
			} else if (scrollMethod != null) {
				return "type="+type+",x="+de.double1+",y="+de.double2+",xscroll="+de.double3+",yscroll="+de.double4;
			} else if (modMethod != null) {
				return "type="+type+",x="+de.double1+",y="+de.double2+",mod="+de.object1;
			} else if (modButtonMethod != null) {
				return "type="+type+",button="+de.int1+",x="+de.double1+",y="+de.double2+",mod="+de.object1;
			} else {
				throw new AssertionError();
			}
		}
	}

	private static class EnterEventType extends ClickIshEventType {
		private EnterEventType(String type, ClickEventMethod method) {
			super(type, method);
		}

		@Override
		public EventResponse visit(DefaultEvent event, EventProcessor ep) {
			if (!(ep instanceof InputEventProcessor)) return EventResponse.PASS;
			InputEventProcessor iep = (InputEventProcessor)ep;
			EventResponse rtrn = super.visit(event, ep);
			event.context.getEngine().addThingToEnteredList(iep);
			return rtrn;
		}
	}

	private static class MoveEventType extends ClickIshEventType {
		private MoveEventType(String type, ClickEventMethod method) {
			super(type, method);
		}

		@Override
		public EventResponse visit(DefaultEvent event, EventProcessor ep) {
			if (!(ep instanceof InputEventProcessor)) return EventResponse.PASS;
			InputEventProcessor iep = (InputEventProcessor)ep;
			if (!event.context.getEngine().isThingInEnteredList(iep)) {
				TYPE_ENTER.visit(event, iep);
			}
			return super.visit(event, iep);
		}
	}

	private interface KeyEventMethod {
		EventResponse on(InputEventProcessor w, Key key, int scancode, KeyModifiers mod);
	}

	private static class KeyEventType implements EventType {
		private final String type;
		private KeyEventMethod method;
		private KeyEventType(String type, KeyEventMethod method) {
			this.type = type;
			this.method = method;
		}
		@Override
		public EventResponse visit(DefaultEvent event, EventProcessor ep) {
			if (!(ep instanceof InputEventProcessor)) return EventResponse.PASS;
			return method.on((InputEventProcessor)ep, (Key)event.object1, event.int1, (KeyModifiers)event.object2);
		}
		@Override
		public boolean isRelevant(DefaultEvent event, EventProcessor ep) {
			return focusedOrGlobal(ep);
		}
		@Override
		public void enterContainer(DefaultEvent event, Container container) {
		}
		@Override
		public void exitContainer(DefaultEvent event, Container container) {
		}
		@Override
		public String toString(DefaultEvent de) {
			return "type="+type+",key="+de.object1+",scancode="+de.int1+",mod="+de.object2;
		}
	}

	private interface IntEventMethod {
		EventResponse on(InputEventProcessor w, int i);
	}

	private static class TextEnteredEventType implements EventType {
		private final String type;
		private IntEventMethod method;
		private TextEnteredEventType(String type, IntEventMethod method) {
			this.type = type;
			this.method = method;
		}
		@Override
		public EventResponse visit(DefaultEvent event, EventProcessor ep) {
			if (!(ep instanceof InputEventProcessor)) return EventResponse.PASS;
			return method.on((InputEventProcessor)ep, event.int1);
		}
		@Override
		public boolean isRelevant(DefaultEvent event, EventProcessor ep) {
			return focusedOrGlobal(ep);
		}
		@Override
		public void enterContainer(DefaultEvent event, Container container) {
		}
		@Override
		public void exitContainer(DefaultEvent event, Container container) {
		}
		@Override
		public String toString(DefaultEvent de) {
			return "type="+type+",codepoint="+de.int1+",characters="+new String(Character.toChars(de.int1));
		}
	}

	private interface SimpleEventMethod {
		EventResponse on(InputEventProcessor w);
	}

	private static class FocusEventType implements EventType {
		private final String type;
		private SimpleEventMethod method;
		private FocusEventType(String type, SimpleEventMethod method) {
			this.type = type;
			this.method = method;
		}
		@Override
		public EventResponse visit(DefaultEvent event, EventProcessor ep) {
			if (!(ep instanceof InputEventProcessor)) return EventResponse.PASS;
			return method.on((InputEventProcessor)ep);
		}
		@Override
		public boolean isRelevant(DefaultEvent event, EventProcessor ep) {
			return focusedOrGlobal(ep);
		}
		@Override
		public void enterContainer(DefaultEvent event, Container container) {
		}
		@Override
		public void exitContainer(DefaultEvent event, Container container) {
		}
		@Override
		public String toString(DefaultEvent de) {
			return "type="+type;
		}
	}

	private static boolean focusedOrGlobal(EventProcessor ep) {
		if (ep instanceof Widget) {
			Widget widget = (Widget)ep;
			return widget.getParent() == null || widget.getParent().getFocusedWidget() == widget;
		}
		return ep instanceof InputEventProcessor;
	}

}
