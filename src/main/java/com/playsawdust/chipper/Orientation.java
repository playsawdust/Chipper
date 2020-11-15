/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper;

import org.checkerframework.checker.units.qual.radians;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Orientation describes the bearings of objects which primarily orient themselves against some
 * local "ground plane", such as humans walking on the surface of a planet.
 * 
 * <p>This "ground plane" is actually three vectors called a basis. This basis allows us to
 * describe the local space the euler angles are relative to. It forms a basis for
 * measurement. It can also be expressed as a quaternion, and facilities are provided to
 * use quaternion interpolation to smoothly transition between bases.
 * 
 * <p>The orientation of the object against the plane is expressed as user-friendly euler
 * angles. Yaw swivels left and right, pitch leans up and down, and roll tilts left or right.
 * Euler angles allow straightforward mouselook, as well as straightforward projection of
 * direction onto the local X/Z plane.+
 */
public class Orientation {
	private Quaterniond basis = new Quaterniond();
	private @radians double yaw   = 0.0;
	private @radians double pitch = 0.0;
	private @radians double roll  = 0.0;
	
	public Quaterniondc getBasis() {
		return basis;
	}
	
	public Quaterniond getBasis(Quaterniond dest) {
		basis.get(dest);
		return dest;
	}
	
	public Vector3d getEulerAngles() {
		return getEulerAngles(new Vector3d());
	}
	
	public Vector3d getEulerAngles(Vector3d dest) {
		dest.x = pitch;
		dest.y = yaw;
		dest.z = roll;
		return dest;
	}
	
	public void setEulerAngles(Vector3dc dest) {
		pitch = dest.x();
		yaw = dest.y();
		roll = dest.z();
	}
	
	public double getYaw() {
		return yaw;
	}
	
	public double getPitch() {
		return pitch;
	}
	
	public double getRoll() {
		return roll;
	}
	
	public void setYaw(double yaw) {
		this.yaw = yaw;
	}
	
	public void setPitch(double pitch) {
		this.pitch = pitch;
	}
	
	public void setRoll(double roll) {
		this.roll = roll;
	}
	
	public Vector3d localToWorld(Vector3dc local) {
		return localToWorld(local, new Vector3d());
	}
	
	public Vector3d localToWorld(Vector3dc local, Vector3d result) {
		Quaterniond euler = new Quaterniond().identity().rotateZYX(roll, yaw, pitch);
		result.set(local);
		euler.transform(result);
		basis.transform(result);
		return result;
	}
	
	/* TODO: Implement when we have some kind of facing enum
	public Facing getOrthogonalLook();
	*/
	
	/**
	 * Project the look vector from this orientation onto the global world X/Z plane, normalized.
	 * If the orientation is looking straight up or down, an unspecified vector of length 1 is returned.
	 */
	public Vector2d getHorizontalLook() {
		Vector3d look = getLookVector();
		Vector2d result = new Vector2d(look.x, look.z).normalize();
		if (result.lengthSquared()==0) result = new Vector2d(0, 1);
		return result;
	}
	
	public Vector3d getLookVector() {
		return localToWorld(new Vector3d(0, 0, -1));
	}
	
	public Vector3d getRightVector() {
		return localToWorld(new Vector3d(1, 0, 0));
	}
	
	/**
	 * Gets the "up" direction in worldspace.
	 */
	public Vector3d getUpVector() {
		return localToWorld(new Vector3d(0, 1, 0));
	}
	
	/**
	 * Gets the Z basis vector of this orientation's frame of reference.
	 */
	public Vector3d getBasisX() {
		return basis.transform(new Vector3d(1, 0, 0));
	}
	
	/**
	 * Gets the Y basis vector of this orientation's frame of reference. This is the "objective local up", regardless
	 * of the current state of this orientation. That is, if a creature is walking on the ceiling, its basisY will be
	 * the vector (0, -1, 0) but it will locally consider itself upright.
	 */
	public Vector3d getBasisY() {
		return basis.transform(new Vector3d(0, 1, 0));
	}
	
	/**
	 * Gets the Z basis vector of this orientation's frame of reference.
	 */
	public Vector3d getBasisZ() {
		return basis.transform(new Vector3d(0, 0, 1));
	}
}
