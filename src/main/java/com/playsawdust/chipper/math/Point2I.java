/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.math;

import com.playsawdust.chipper.exception.RecycledObjectException;
import com.playsawdust.chipper.toolbox.pool.ObjectPool;
import com.playsawdust.chipper.toolbox.pool.PooledObject;

/**
 * A point in 2D space, with an X and Y coordinate. Uses integers.
 */
public final class Point2I implements PooledObject {
	private static final ObjectPool<Point2I> pool = new ObjectPool<>(Point2I::new);

	private boolean recycled;

	private int x;
	private int y;

	private Point2I() {}

	/**
	 * @return the X of this point
	 */
	public int getX() {
		if (recycled) throw new RecycledObjectException(this);
		return x;
	}
	/**
	 * @return the Y of this point
	 */
	public int getY() {
		if (recycled) throw new RecycledObjectException(this);
		return y;
	}

	/**
	 * @param x the new X for this point
	 */
	public void setX(int x) {
		if (recycled) throw new RecycledObjectException(this);
		this.x = x;
	}

	/**
	 * @param y the new Y for this point
	 */
	public void setY(int y) {
		if (recycled) throw new RecycledObjectException(this);
		this.y = y;
	}

	/**
	 * @param x the new X for this point
	 * @param y the new Y for this point
	 */
	public void set(int x, int y) {
		if (recycled) throw new RecycledObjectException(this);
		this.x = x;
		this.y = y;
	}

	/**
	 * @param point the point to copy
	 */
	public void set(Point2I point) {
		set(point.getX(), point.getY());
	}

	/**
	 * @param x the amount to add to this point's X
	 * @param y the amount to add to this point's Y
	 */
	public void add(int x, int y) {
		if (recycled) throw new RecycledObjectException(this);
		this.x += x;
		this.y += y;
	}

	/**
	 * @param point the point to add to this one
	 */
	public void add(Point2I point) {
		add(point.getX(), point.getY());
	}

	/**
	 * @param x the amount to subtract from this point's X
	 * @param y the amount to subtract from this point's Y
	 */
	public void subtract(int x, int y) {
		if (recycled) throw new RecycledObjectException(this);
		this.x -= x;
		this.y -= y;
	}

	/**
	 * @param point the point to subtract from this one
	 */
	public void subtract(Point2I point) {
		subtract(point.getX(), point.getY());
	}

	/**
	 * Upcast this Point2I to a Point2D.
	 * @return a Point2D with the same coordinates as this Point2I
	 */
	public Point2D asPoint2D() {
		if (recycled) throw new RecycledObjectException(this);
		return Point2D.from(x, y);
	}

	@Override
	public void recycle() {
		recycled = true;
		x = y = 0;
		pool.recycle(this);
	}


	public static Point2I from(int x, int y) {
		Point2I r = pool.get();
		r.recycled = false;
		r.x = x;
		r.y = y;
		return r;
	}

	public static Point2I get() {
		return from(0, 0);
	}

}
