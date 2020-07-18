/*
 * Chipper - an open polyglot game engine
 * Copyright (C) 2019-2020 the Chipper developers
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.playsawdust.chipper.client.gl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import com.google.common.collect.ImmutableBiMap;

public class GLProperties {
	public static final ImmutableBiMap<String, Integer> INTEGER_NAMES = ImmutableBiMap.<String, Integer>builder()
			//BLEND - boolean
			//BLEND_COLOR - vec4
			//CULL_FACE - boolean
			//DEPTH_RANGE - int[2]
			//depth_test - boolean
			//depth_writemask - boolean
			//dither - boolean
			//doublebuffer - boolean
			
			//COLOR_CLEAR_VALUE - vec4
			//.put("gl_color_logic_op",    GL11.GL_COLOR_LOGIC_OP) //boolean
			//COLOR_WRITEMASK - 4 booleans
			//COMPRESSED_TEXTURE_FORMATS - int[]
			
			
			.put("COLOR_MATERIAL", GL11.GL_COLOR_MATERIAL)
			.put("TEXTURE_2D", GL11.GL_TEXTURE_2D)
			
			.put("CURRENT_COLOR", GL11.GL_CURRENT_COLOR)
			.put("CURRENT_INDEX", GL11.GL_CURRENT_INDEX)
			/*
		        GL_CURRENT_NORMAL                = 0xB02,
		        GL_CURRENT_TEXTURE_COORDS        = 0xB03,
		        GL_CURRENT_RASTER_COLOR          = 0xB04,
		        GL_CURRENT_RASTER_INDEX          = 0xB05,
		        GL_CURRENT_RASTER_TEXTURE_COORDS = 0xB06,
		        GL_CURRENT_RASTER_POSITION       = 0xB07,
		        GL_CURRENT_RASTER_POSITION_VALID = 0xB08,
		        GL_CURRENT_RASTER_DISTANCE       = 0xB09,
		  	*/
			.put("POINT_SMOOTH", GL11.GL_POINT_SMOOTH)
			.put("POINT_SIZE", GL11.GL_POINT_SIZE)
			.put("POINT_SIZE_RANGE", GL11.GL_POINT_SIZE_RANGE)
			.put("POINT_SIZE_GRANULARITY", GL11.GL_POINT_SIZE_GRANULARITY)
			.put("LINE_SMOOTH", GL11.GL_LINE_SMOOTH)
			.put("LINE_WIDTH", GL11.GL_LINE_WIDTH)
			.put("LINE_WIDTH_RANGE", GL11.GL_LINE_WIDTH_RANGE)
			/*
		        GL_LINE_WIDTH_GRANULARITY        = 0xB23,
		        GL_LINE_STIPPLE                  = 0xB24,
		        GL_LINE_STIPPLE_PATTERN          = 0xB25,
		        GL_LINE_STIPPLE_REPEAT           = 0xB26,
		        GL_LIST_MODE                     = 0xB30,
		        GL_MAX_LIST_NESTING              = 0xB31,
		        GL_LIST_BASE                     = 0xB32,
		        GL_LIST_INDEX                    = 0xB33,
		        GL_POLYGON_MODE                  = 0xB40,
		        GL_POLYGON_SMOOTH                = 0xB41,
		        GL_POLYGON_STIPPLE               = 0xB42,
		        GL_EDGE_FLAG                     = 0xB43,
		        GL_CULL_FACE                     = 0xB44,
		        GL_CULL_FACE_MODE                = 0xB45,
		        GL_FRONT_FACE                    = 0xB46,
		        GL_LIGHTING                      = 0xB50,
		        GL_LIGHT_MODEL_LOCAL_VIEWER      = 0xB51,
		        GL_LIGHT_MODEL_TWO_SIDE          = 0xB52,
		        GL_LIGHT_MODEL_AMBIENT           = 0xB53,
		        GL_SHADE_MODEL                   = 0xB54,
		        GL_COLOR_MATERIAL_FACE           = 0xB55,
		        GL_COLOR_MATERIAL_PARAMETER      = 0xB56,
		        GL_COLOR_MATERIAL                = 0xB57,
		        GL_FOG                           = 0xB60,
		        GL_FOG_INDEX                     = 0xB61,
		        GL_FOG_DENSITY                   = 0xB62,
		        GL_FOG_START                     = 0xB63,
		        GL_FOG_END                       = 0xB64,
		        GL_FOG_MODE                      = 0xB65,
		        GL_FOG_COLOR                     = 0xB66,
		        GL_DEPTH_RANGE                   = 0xB70,
		        GL_DEPTH_TEST                    = 0xB71,
		        GL_DEPTH_WRITEMASK               = 0xB72,
		        GL_DEPTH_CLEAR_VALUE             = 0xB73,
		        GL_DEPTH_FUNC                    = 0xB74,
		        GL_ACCUM_CLEAR_VALUE             = 0xB80,
		        GL_STENCIL_TEST                  = 0xB90,
		        GL_STENCIL_CLEAR_VALUE           = 0xB91,
		        GL_STENCIL_FUNC                  = 0xB92,
		        GL_STENCIL_VALUE_MASK            = 0xB93,
		        GL_STENCIL_FAIL                  = 0xB94,
		        GL_STENCIL_PASS_DEPTH_FAIL       = 0xB95,
		        GL_STENCIL_PASS_DEPTH_PASS       = 0xB96,
		        GL_STENCIL_REF                   = 0xB97,
		        GL_STENCIL_WRITEMASK             = 0xB98,
		        GL_MATRIX_MODE                   = 0xBA0,
		        GL_NORMALIZE                     = 0xBA1,
		        GL_VIEWPORT                      = 0xBA2,
		        GL_MODELVIEW_STACK_DEPTH         = 0xBA3,
		        GL_PROJECTION_STACK_DEPTH        = 0xBA4,
		        GL_TEXTURE_STACK_DEPTH           = 0xBA5,
		        GL_MODELVIEW_MATRIX              = 0xBA6,
		        GL_PROJECTION_MATRIX             = 0xBA7,
		        GL_TEXTURE_MATRIX                = 0xBA8,
		        GL_ATTRIB_STACK_DEPTH            = 0xBB0,
		        GL_CLIENT_ATTRIB_STACK_DEPTH     = 0xBB1,
		        GL_ALPHA_TEST                    = 0xBC0,
		        GL_ALPHA_TEST_FUNC               = 0xBC1,
		        GL_ALPHA_TEST_REF                = 0xBC2,
		        GL_DITHER                        = 0xBD0,
		        GL_BLEND_DST                     = 0xBE0,
		        GL_BLEND_SRC                     = 0xBE1,
		        GL_BLEND                         = 0xBE2,
		        GL_LOGIC_OP_MODE                 = 0xBF0,
		        GL_INDEX_LOGIC_OP                = 0xBF1,
		        GL_LOGIC_OP                      = 0xBF1,
		        GL_COLOR_LOGIC_OP                = 0xBF2,
		        GL_AUX_BUFFERS                   = 0xC00,
		        GL_DRAW_BUFFER                   = 0xC01,
		        GL_READ_BUFFER                   = 0xC02,
		        GL_SCISSOR_BOX                   = 0xC10,
		        GL_SCISSOR_TEST                  = 0xC11,
		        GL_INDEX_CLEAR_VALUE             = 0xC20,
		        GL_INDEX_WRITEMASK               = 0xC21,
		        GL_COLOR_CLEAR_VALUE             = 0xC22,
		        GL_COLOR_WRITEMASK               = 0xC23,
		        GL_INDEX_MODE                    = 0xC30,
		        GL_RGBA_MODE                     = 0xC31,
		        GL_DOUBLEBUFFER                  = 0xC32,
		        GL_STEREO                        = 0xC33,
		        GL_RENDER_MODE                   = 0xC40,
		        GL_PERSPECTIVE_CORRECTION_HINT   = 0xC50,
		        GL_POINT_SMOOTH_HINT             = 0xC51,
		        GL_LINE_SMOOTH_HINT              = 0xC52,
		        GL_POLYGON_SMOOTH_HINT           = 0xC53,
		        GL_FOG_HINT                      = 0xC54,
		        GL_TEXTURE_GEN_S                 = 0xC60,
		        GL_TEXTURE_GEN_T                 = 0xC61,
		        GL_TEXTURE_GEN_R                 = 0xC62,
		        GL_TEXTURE_GEN_Q                 = 0xC63,
		        GL_PIXEL_MAP_I_TO_I              = 0xC70,
		        GL_PIXEL_MAP_S_TO_S              = 0xC71,
		        GL_PIXEL_MAP_I_TO_R              = 0xC72,
		        GL_PIXEL_MAP_I_TO_G              = 0xC73,
		        GL_PIXEL_MAP_I_TO_B              = 0xC74,
		        GL_PIXEL_MAP_I_TO_A              = 0xC75,
		        GL_PIXEL_MAP_R_TO_R              = 0xC76,
		        GL_PIXEL_MAP_G_TO_G              = 0xC77,
		        GL_PIXEL_MAP_B_TO_B              = 0xC78,
		        GL_PIXEL_MAP_A_TO_A              = 0xC79,
		        GL_PIXEL_MAP_I_TO_I_SIZE         = 0xCB0,
		        GL_PIXEL_MAP_S_TO_S_SIZE         = 0xCB1,
		        GL_PIXEL_MAP_I_TO_R_SIZE         = 0xCB2,
		        GL_PIXEL_MAP_I_TO_G_SIZE         = 0xCB3,
		        GL_PIXEL_MAP_I_TO_B_SIZE         = 0xCB4,
		        GL_PIXEL_MAP_I_TO_A_SIZE         = 0xCB5,
		        GL_PIXEL_MAP_R_TO_R_SIZE         = 0xCB6,
		        GL_PIXEL_MAP_G_TO_G_SIZE         = 0xCB7,
		        GL_PIXEL_MAP_B_TO_B_SIZE         = 0xCB8,
		        GL_PIXEL_MAP_A_TO_A_SIZE         = 0xCB9,
		        GL_UNPACK_SWAP_BYTES             = 0xCF0,
		        GL_UNPACK_LSB_FIRST              = 0xCF1,
		        GL_UNPACK_ROW_LENGTH             = 0xCF2,
		        GL_UNPACK_SKIP_ROWS              = 0xCF3,
		        GL_UNPACK_SKIP_PIXELS            = 0xCF4,
		        GL_UNPACK_ALIGNMENT              = 0xCF5,
		        GL_PACK_SWAP_BYTES               = 0xD00,
		        GL_PACK_LSB_FIRST                = 0xD01,
		        GL_PACK_ROW_LENGTH               = 0xD02,
		        GL_PACK_SKIP_ROWS                = 0xD03,
		        GL_PACK_SKIP_PIXELS              = 0xD04,
		        GL_PACK_ALIGNMENT                = 0xD05,
		        GL_MAP_COLOR                     = 0xD10,
		        GL_MAP_STENCIL                   = 0xD11,
		        GL_INDEX_SHIFT                   = 0xD12,
		        GL_INDEX_OFFSET                  = 0xD13,
		        GL_RED_SCALE                     = 0xD14,
		        GL_RED_BIAS                      = 0xD15,
		        GL_ZOOM_X                        = 0xD16,
		        GL_ZOOM_Y                        = 0xD17,
		        GL_GREEN_SCALE                   = 0xD18,
		        GL_GREEN_BIAS                    = 0xD19,
		        GL_BLUE_SCALE                    = 0xD1A,
		        GL_BLUE_BIAS                     = 0xD1B,
		        GL_ALPHA_SCALE                   = 0xD1C,
		        GL_ALPHA_BIAS                    = 0xD1D,
		        GL_DEPTH_SCALE                   = 0xD1E,
		        GL_DEPTH_BIAS                    = 0xD1F,
		        GL_MAX_EVAL_ORDER                = 0xD30,
		        GL_MAX_LIGHTS                    = 0xD31,
		        GL_MAX_CLIP_PLANES               = 0xD32,
		        GL_MAX_TEXTURE_SIZE              = 0xD33,
		        GL_MAX_PIXEL_MAP_TABLE           = 0xD34,
		        GL_MAX_ATTRIB_STACK_DEPTH        = 0xD35,
		        GL_MAX_MODELVIEW_STACK_DEPTH     = 0xD36,
		        GL_MAX_NAME_STACK_DEPTH          = 0xD37,
		        GL_MAX_PROJECTION_STACK_DEPTH    = 0xD38,
		        GL_MAX_TEXTURE_STACK_DEPTH       = 0xD39,
		        GL_MAX_VIEWPORT_DIMS             = 0xD3A,
		        GL_MAX_CLIENT_ATTRIB_STACK_DEPTH = 0xD3B,
		        GL_SUBPIXEL_BITS                 = 0xD50,
		        GL_INDEX_BITS                    = 0xD51,
		        GL_RED_BITS                      = 0xD52,
		        GL_GREEN_BITS                    = 0xD53,
		        GL_BLUE_BITS                     = 0xD54,
		        GL_ALPHA_BITS                    = 0xD55,
		        GL_DEPTH_BITS                    = 0xD56,
		        GL_STENCIL_BITS                  = 0xD57,
		        GL_ACCUM_RED_BITS                = 0xD58,
		        GL_ACCUM_GREEN_BITS              = 0xD59,
		        GL_ACCUM_BLUE_BITS               = 0xD5A,
		        GL_ACCUM_ALPHA_BITS              = 0xD5B,
		        GL_NAME_STACK_DEPTH              = 0xD70,
		        GL_AUTO_NORMAL                   = 0xD80,
		        GL_MAP1_COLOR_4                  = 0xD90,
		        GL_MAP1_INDEX                    = 0xD91,
		        GL_MAP1_NORMAL                   = 0xD92,
		        GL_MAP1_TEXTURE_COORD_1          = 0xD93,
		        GL_MAP1_TEXTURE_COORD_2          = 0xD94,
		        GL_MAP1_TEXTURE_COORD_3          = 0xD95,
		        GL_MAP1_TEXTURE_COORD_4          = 0xD96,
		        GL_MAP1_VERTEX_3                 = 0xD97,
		        GL_MAP1_VERTEX_4                 = 0xD98,
		        GL_MAP2_COLOR_4                  = 0xDB0,
		        GL_MAP2_INDEX                    = 0xDB1,
		        GL_MAP2_NORMAL                   = 0xDB2,
		        GL_MAP2_TEXTURE_COORD_1          = 0xDB3,
		        GL_MAP2_TEXTURE_COORD_2          = 0xDB4,
		        GL_MAP2_TEXTURE_COORD_3          = 0xDB5,
		        GL_MAP2_TEXTURE_COORD_4          = 0xDB6,
		        GL_MAP2_VERTEX_3                 = 0xDB7,
		        GL_MAP2_VERTEX_4                 = 0xDB8,
		        GL_MAP1_GRID_DOMAIN              = 0xDD0,
		        GL_MAP1_GRID_SEGMENTS            = 0xDD1,
		        GL_MAP2_GRID_DOMAIN              = 0xDD2,
		        GL_MAP2_GRID_SEGMENTS            = 0xDD3,
		        GL_TEXTURE_1D                    = 0xDE0,
		        GL_TEXTURE_2D                    = 0xDE1,
		        GL_FEEDBACK_BUFFER_POINTER       = 0xDF0,
		        GL_FEEDBACK_BUFFER_SIZE          = 0xDF1,
		        GL_FEEDBACK_BUFFER_TYPE          = 0xDF2,
		        GL_SELECTION_BUFFER_POINTER      = 0xDF3,
		        GL_SELECTION_BUFFER_SIZE         = 0xDF4;*/
			.build();
}
