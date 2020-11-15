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
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class SceneObject implements Orientable {
	private Vector3d position = new Vector3d(0,0,0);
	private Orientation orientation = new Orientation();

	//private @radians double yaw = 0;
	//private @radians double pitch = 0;
	//private @radians double roll = 0;

	public Vector3d getPosition(Vector3d dest) {
		return dest.set(position);
	}
	
	public Vector3dc getPosition() {
		return position;
	}

	//@Override
	//public Quaterniond getDirection(Quaterniond dest) {
	//	return dest.identity().rotateZYX(roll, yaw, pitch);
	//}
	
	/**
	 * Gets the Orientation for this object. Changes to the provided Orientation will be reflected in this object.
	 */
	public Orientation getOrientation() {
		return this.orientation;
	}
	
	@Deprecated
	public Vector3d getDirectionEuler() {
		return getDirectionEuler(new Vector3d());
	}
	
	@Deprecated
	public Vector3d getDirectionEuler(Vector3d dest) {
		return orientation.getEulerAngles(dest);
		//return dest.set(pitch, yaw, roll);
	}
	
	public void setPosition(Vector3dc position) {
		this.position.x = position.x();
		this.position.y = position.y();
		this.position.z = position.z();
	}
	
	public void setPositionRelative(Vector3dc relative) {
		this.position.add(relative, this.position);
	}
	
	public void setPositionRelative(double x, double y, double z) {
		this.position.add(x, y, z, this.position);
	}

	//@Override
	//public void setDirection(Quaterniondc direction) {
	//	Vector3d lookVec = direction.transform(new Vector3d(0,0,-1));
	//	yaw = Math.atan2(lookVec.z, lookVec.x);
	//	double dxz = Math.sqrt(lookVec.x*lookVec.x + lookVec.z*lookVec.z);
	//	Vector2d lookUpVec = new Vector2d(dxz, lookVec.y).normalize();
	//	pitch = Math.atan2(lookUpVec.y, lookUpVec.x);
		
	//	roll = 0.0; //for now. We'd need to transform an up vector and check its rotation around the look vec to find this, which is somewhat expensive
		
		/*
		Vector3d euler = direction.getEulerAnglesXYZ(new Vector3d()); //TODO: This is wrong. We need ZYX
		yaw = euler.y;
		pitch = euler.x;
		roll = euler.z;*/
	//}
	
	@Deprecated
	public void setDirectionEuler(Vector3dc eulerAngles) {
		this.orientation.setEulerAngles(eulerAngles);
		//this.pitch = eulerAngles.x();
		//this.yaw = eulerAngles.y();
		//this.roll = eulerAngles.z();
	}
	
	@Deprecated
	public void setDirectionEuler(double pitch, double yaw, double roll) {
		this.orientation.setPitch(pitch);
		this.orientation.setYaw(yaw);
		this.orientation.setRoll(roll);
	}
	
	@Deprecated
	public @radians double getYaw() {
		return this.orientation.getYaw();
	}
	
	@Deprecated
	public @radians double getPitch() {
		return this.orientation.getPitch();
	}
	
	@Deprecated
	public @radians double getRoll() {
		return this.orientation.getRoll();
	}
	
	/**
	 * Rotates this object left-to-right (around the Y axis) relative to its current yaw.
	 */
	@Deprecated
	public void yaw(@radians double yaw) {
		this.orientation.setYaw(this.orientation.getYaw()+yaw);
		//this.yaw += yaw;
	}
	
	@Deprecated
	public void pitch(@radians double pitch) {
		this.orientation.setPitch(this.orientation.getPitch()+pitch);
		//this.pitch += pitch;
	}
	
	@Deprecated
	public void roll(@radians double roll) {
		this.orientation.setRoll(this.orientation.getRoll()+roll);
		//this.roll += roll;
	}
	
	@Deprecated
	public void setYaw(@radians double yaw) {
		this.orientation.setYaw(yaw);
	}
	
	@Deprecated
	public void setPitch(@radians double pitch) {
		this.orientation.setPitch(pitch);
	}
	
	@Deprecated
	public void setRoll(@radians double roll) {
		this.orientation.setRoll(roll);
	}
	
	
}
