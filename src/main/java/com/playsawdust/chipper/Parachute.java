/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.playsawdust.chipper.toolbox.lipstick.MonotonicTime;

/**
 * Large preallocated blocks of memory that can be freed to alleviate an out-of-memory condition
 * just long enough to alert the user, or to give enough time to deallocate nonessential resources
 * and recover.
 */
public class Parachute {
	private static final Logger log = LoggerFactory.getLogger(Parachute.class);

	/**
	 * The size of each parachute in bytes. (Two parachutes are allocated; one in the Java heap,
	 * and one in native memory.)
	 * <p>
	 * Must be divisible by 4.
	 */
	public static final int SIZE = 16 * 1024 * 1024;
	private static final int TOTAL_SIZE_M = (SIZE*2)/1024/1024;

	// "safe food". because hexspeak is fun
	private static final int FILL = 0x5AFEF00D;

	private static final Object MUTEX = new Object();

	private static int[] onheap;
	private static ByteBuffer offheap;

	/**
	 * Allocate the parachute, if it hasn't been allocated already.
	 */
	public static void allocate() {
		// don't use Stopwatch for parity with free
		long start = MonotonicTime.millis();
		synchronized (MUTEX) {
			if (onheap == null) {
				onheap = new int[SIZE/4];
				Arrays.fill(onheap, FILL);
				offheap = MemoryUtil.memAlloc(SIZE);
				while (offheap.hasRemaining()) {
					offheap.putInt(FILL);
				}
				log.debug("Allocated {}M of memory for parachutes in {}ms", TOTAL_SIZE_M, MonotonicTime.deltaMillis(start));
			}
		}
	}

	/**
	 * Free the parachute, making {@code SIZE} bytes of memory available in native and heap memory.
	 * @return {@code true} if the parachute was able to be freed; {@code false} if it was never
	 * 		allocated or was already freed
	 */
	public static boolean free() {
		// we can't allocate a Stopwatch because this method is likely called after an OOME is
		// thrown, so we won't have the heap space for a Stopwatch instance; however, primitive
		// locals don't go on the heap and are safe to use
		long start = MonotonicTime.millis();
		synchronized (MUTEX) {
			if (onheap != null) {
				onheap = null;
				MemoryUtil.memFree(offheap);
				offheap = null;
				System.gc();
				System.gc();
				// both of these log arguments get boxed, but by now we have free heap
				log.debug("Freed {}M of memory from parachutes in {}ms", TOTAL_SIZE_M, MonotonicTime.deltaMillis(start));
				return true;
			} else {
				log.debug("Tried to free parachutes, but there are none!");
				return false;
			}
		}
	}

}
