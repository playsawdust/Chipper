/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client;

import com.playsawdust.chipper.client.widget.DefaultEvent;
import com.playsawdust.chipper.client.widget.EventResponse;
import com.playsawdust.chipper.client.widget.container.Container;

/**
 * Represents something that can process events.
 */
public interface EventProcessor {

	/**
	 * Represents an event that can be sent to a processor. Generally these are
	 * provided by the base engine itself, and the event system is not
	 * especially designed for custom events.
	 */
	interface Event {
		/**
		 * Implements the visitor pattern. "Visit" this EventProcessor, calling the
		 * on* method relevant to this concrete Event class.
		 * @param ep the processor to visit
		 * @return that processor's response to this event
		 */
		EventResponse visit(EventProcessor ep);
		/**
		 * Returns {@code true} if this Event is "relevant" for the given
		 * EventProcessor, and should be propagated to it. For example, a click event
		 * would return {@code true} here if the processor is a widget and its area
		 * contains the event's coordinates.
		 * @param ep the processor to check for relevance
		 * @return {@code true} if this event is relevant for the given processor
		 */
		boolean isRelevant(EventProcessor ep);
		/**
		 * Called before the given container begins dispatching this event to
		 * its children.
		 * @param container the relevant container
		 */
		void enterContainer(Container container);
		/**
		 * Called after the given container finishes dispatching this event to
		 * its children.
		 * @param container the relevant container
		 */
		void exitContainer(Container container);
		/**
		 * <b>Events must provide a useful toString implementation.</b>
		 * <p>
		 * {@inheritDoc}
		 */
		@Override
		String toString();

		/**
		 * @return {@code true} if the given event was created by unprivileged
		 * 		code
		 */
		static boolean isFake(Event event) {
			if (event instanceof DefaultEvent) {
				return ((DefaultEvent)event).isFake();
			}
			return true;
		}

	}

	/**
	 * Skeleton implementation of Event to be used for very basic third-party events.
	 * An instance of SimpleEvent should be kept in a {@code public static final} field
	 * in the relevant class, and consumers should directly override {@code processEvent}
	 * and check reference equality with {@code ==}.
	 */
	class SimpleEvent implements Event {
		private final String description;
		public SimpleEvent(String description) {
			this.description = description;
		}
		@Override public EventResponse visit(EventProcessor ep) { return EventResponse.PASS; }
		@Override public boolean isRelevant(EventProcessor ep) { return true; }
		@Override public void exitContainer(Container container) {}
		@Override public void enterContainer(Container container) {}
		@Override public String toString() { return "SimpleEvent["+description+"]"; }
	}

	/**
	 * Higher level APIs than EventProcessor should provide on* methods for all the different kinds
	 * of events they're expected to receive, and those should be called by the Event's visit
	 * method.
	 * @param event the event to process
	 * @return this processor's response to this event
	 */
	default EventResponse processEvent(Event event) {
		return event.visit(this);
	}

}
