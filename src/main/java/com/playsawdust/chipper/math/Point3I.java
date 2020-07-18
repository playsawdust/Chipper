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
 * A point in 3D space, with a X, Y, and Z coordinates. Uses ints.
 */
public final class Point3I implements PooledObject {
	private static final ObjectPool<Point3I> pool = new ObjectPool<>(Point3I::new);

	private boolean recycled;

	private int x;
	private int y;
	private int z;

	private Point3I() {}

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
	 * @return the Y of this point
	 */
	public int getZ() {
		if (recycled) throw new RecycledObjectException(this);
		return z;
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
	 * @param z the new Z for this point
	 */
	public void setZ(int z) {
		if (recycled) throw new RecycledObjectException(this);
		this.z = z;
	}

	/**
	 * @param x the new X for this point
	 * @param y the new Y for this point
	 * @param z the new Z for this point
	 */
	public void set(int x, int y, int z) {
		if (recycled) throw new RecycledObjectException(this);
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * @param point the point to copy
	 */
	public void set(Point3I point) {
		set(point.getX(), point.getY(), point.getZ());
	}

	/**
	 * @param x the amount to add to this point's X
	 * @param y the amount to add to this point's Y
	 * @param z the amount to add to this point's Z
	 */
	public void add(int x, int y, int z) {
		if (recycled) throw new RecycledObjectException(this);
		this.x += x;
		this.y += y;
		this.z += z;
	}

	/**
	 * @param point the point to add to this one
	 */
	public void add(Point3I point) {
		add(point.getX(), point.getY(), point.getZ());
	}

	/**
	 * @param x the amount to subtract from this point's X
	 * @param y the amount to subtract from this point's Y
	 */
	public void subtract(int x, int y, int z) {
		if (recycled) throw new RecycledObjectException(this);
		this.x -= x;
		this.y -= y;
		this.z -= z;
	}

	/**
	 * @param point the point to subtract from this one
	 */
	public void subtract(Point3I point) {
		subtract(point.getX(), point.getY(), point.getZ());
	}

	@Override
	public void recycle() {
		recycled = true;
		x = y = z = 0;
		pool.recycle(this);
	}


	public static Point3I from(int x, int y, int z) {
		Point3I r = pool.get();
		r.recycled = false;
		r.x = x;
		r.y = y;
		r.z = z;
		return r;
	}

	public static Point3I get() {
		return from(0, 0, 0);
	}

}
