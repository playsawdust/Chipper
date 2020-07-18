/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.gl;

import org.lwjgl.opengl.ARBComputeShader;

import com.google.common.base.Preconditions;
import com.playsawdust.chipper.client.BaseGL;
import com.playsawdust.chipper.client.gl.GLShader.ShaderType;

public class GLComputeProgram extends GLProgram {
	protected int workGroupSize = -1;
	
	private GLComputeProgram(int name) {
		super(name);
	}
	
	protected void checkShaderArgument(GLShader shader) {
		Preconditions.checkArgument(shader.getType()==ShaderType.COMPUTE, "can only attach compute shaders to a compute program");
		Preconditions.checkArgument(this.getShaderCount()==0, "cannot attach more than one shader to a compute program");
	}
	
	@Override
	public void link() throws GLLinkException {
		super.link();
		
		workGroupSize = BaseGL.glGetInteger(ARBComputeShader.GL_COMPUTE_WORK_GROUP_SIZE);
	}
	
	@Override
	public void use() {
	}
	
	public int getWorkGroupSize() {
		checkFreed();
		return workGroupSize;
	}
	
	/**
	 * Dispatch {@code items} elements of work
	 * @param items
	 */
	protected void dispatch(int items) {
		checkFreed();
		Preconditions.checkState(this.isLinked());
		ARBComputeShader.glDispatchCompute(items/workGroupSize, 1, 1); //truncated div because we might overrun our buffer otherwise
	}
}
