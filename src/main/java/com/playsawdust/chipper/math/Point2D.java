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
 * A point in 2D space, with an X and Y coordinate. Uses doubles.
 */
public final class Point2D implements PooledObject {
	private static final ObjectPool<Point2D> pool = new ObjectPool<>(Point2D::new);

	private boolean recycled;

	private double x;
	private double y;

	private Point2D() {}

	/**
	 * @return the X of this point
	 */
	public double getX() {
		if (recycled) throw new RecycledObjectException(this);
		return x;
	}
	/**
	 * @return the Y of this point
	 */
	public double getY() {
		if (recycled) throw new RecycledObjectException(this);
		return y;
	}

	/**
	 * @param x the new X for this point
	 */
	public void setX(double x) {
		if (recycled) throw new RecycledObjectException(this);
		this.x = x;
	}

	/**
	 * @param y the new Y for this point
	 */
	public void setY(double y) {
		if (recycled) throw new RecycledObjectException(this);
		this.y = y;
	}

	/**
	 * @param x the new X for this point
	 * @param y the new Y for this point
	 */
	public void set(double x, double y) {
		if (recycled) throw new RecycledObjectException(this);
		this.x = x;
		this.y = y;
	}

	/**
	 * @param point the point to copy
	 */
	public void set(Point2D point) {
		set(point.getX(), point.getY());
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
	public void add(double x, double y) {
		if (recycled) throw new RecycledObjectException(this);
		this.x += x;
		this.y += y;
	}

	/**
	 * @param point the point to add to this one
	 */
	public void add(Point2D point) {
		add(point.getX(), point.getY());
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
	public void subtract(double x, double y) {
		if (recycled) throw new RecycledObjectException(this);
		this.x -= x;
		this.y -= y;
	}

	/**
	 * @param point the point to subtract from this one
	 */
	public void subtract(Point2D point) {
		subtract(point.getX(), point.getY());
	}

	/**
	 * @param point the point to subtract from this one
	 */
	public void subtract(Point2I point) {
		subtract(point.getX(), point.getY());
	}

	@Override
	public void recycle() {
		recycled = true;
		x = y = 0;
		pool.recycle(this);
	}


	public static Point2D from(double x, double y) {
		Point2D r = pool.get();
		r.recycled = false;
		r.x = x;
		r.y = y;
		return r;
	}

	public static Point2D get() {
		return from(0, 0);
	}

}
