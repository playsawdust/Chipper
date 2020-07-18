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
 * A point in 3D space, with a X, Y, and Z coordinates. Uses doubles.
 */
public final class Point3D implements PooledObject {
	private static final ObjectPool<Point3D> pool = new ObjectPool<>(Point3D::new);

	private boolean recycled;

	private double x;
	private double y;
	private double z;

	private Point3D() {}

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
	 * @return the Y of this point
	 */
	public double getZ() {
		if (recycled) throw new RecycledObjectException(this);
		return z;
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
	 * @param z the new Z for this point
	 */
	public void setZ(double z) {
		if (recycled) throw new RecycledObjectException(this);
		this.z = z;
	}

	/**
	 * @param x the new X for this point
	 * @param y the new Y for this point
	 * @param z the new Z for this point
	 */
	public void set(double x, double y, double z) {
		if (recycled) throw new RecycledObjectException(this);
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * @param point the point to copy
	 */
	public void set(Point3D point) {
		set(point.getX(), point.getY(), point.getZ());
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
	public void add(double x, double y, double z) {
		if (recycled) throw new RecycledObjectException(this);
		this.x += x;
		this.y += y;
		this.z += z;
	}

	/**
	 * @param point the point to add to this one
	 */
	public void add(Point3D point) {
		add(point.getX(), point.getY(), point.getZ());
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
	public void subtract(double x, double y, double z) {
		if (recycled) throw new RecycledObjectException(this);
		this.x -= x;
		this.y -= y;
		this.z -= z;
	}

	/**
	 * @param point the point to subtract from this one
	 */
	public void subtract(Point3D point) {
		subtract(point.getX(), point.getY(), point.getZ());
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


	public static Point3D from(double x, double y, double z) {
		Point3D r = pool.get();
		r.recycled = false;
		r.x = x;
		r.y = y;
		r.z = z;
		return r;
	}

	public static Point3D get() {
		return from(0, 0, 0);
	}

}
