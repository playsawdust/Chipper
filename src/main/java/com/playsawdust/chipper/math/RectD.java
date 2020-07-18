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
 * A continuous rectangular solid in 2D space, with a left edge, top edge, right
 * edge, and bottom edge. Uses doubles.
 */
public final class RectD implements PooledObject {
	private static final ObjectPool<RectD> pool = new ObjectPool<>(RectD::new);

	private boolean recycled;

	// if true, the last operation that mutated this rect was from the edge API.
	// if false, it was from the size API.
	// used as a hint in toString to decide what format to use.
	private boolean edgy = false;

	private double left;
	private double top;
	private double right;
	private double bottom;

	private RectD() {}

	/*
	 * Ensure that the left is the minimum X, the right is the maximum X, the
	 * top is the minimum Y, and the bottom is the maximum Y.
	 */
	private void correctEdges() {
		double x1 = left;
		double x2 = right;
		double y1 = top;
		double y2 = bottom;
		left = Math.min(x1, x2);
		right = Math.max(x1, x2);
		top = Math.min(y1, y2);
		bottom = Math.max(y1, y2);
	}

	// by edges
	/**
	 * @return the minimum X of this rect
	 */
	public double getLeft() {
		if (recycled) throw new RecycledObjectException(this);
		return left;
	}
	/**
	 * @return the minimum Y of this rect
	 */
	public double getTop() {
		if (recycled) throw new RecycledObjectException(this);
		return top;
	}
	/**
	 * @return the maximum X of this rect
	 */
	public double getRight() {
		return right;
	}
	/**
	 * @return the maximum Y of this rect
	 */
	public double getBottom() {
		if (recycled) throw new RecycledObjectException(this);
		return bottom;
	}

	/**
	 * <b>Note</b>: A rectangle with a negative width or height is invalid. As
	 * such, this method <i>will swap the left and right edges</i> if the left
	 * is not the minimum X.
	 * @param left the new minimum X for this rect
	 */
	public void setLeft(double left) {
		if (recycled) throw new RecycledObjectException(this);
		this.left = left;
		correctEdges();
	}

	/**
	 * <b>Note</b>: A rectangle with a negative width or height is invalid. As
	 * such, this method <i>will swap the top and bottom edges</i> if the top
	 * is not the minimum Y.
	 * @param top the new minimum Y for this rect
	 */
	public void setTop(double top) {
		if (recycled) throw new RecycledObjectException(this);
		edgy = true;
		this.top = top;
		correctEdges();
	}

	/**
	 * <b>Note</b>: A rectangle with a negative width or height is invalid. As
	 * such, this method <i>will swap the left and right edges</i> if the right
	 * is not the maximum X.
	 * @param right the new maximum X for this rect
	 */
	public void setRight(double right) {
		if (recycled) throw new RecycledObjectException(this);
		edgy = true;
		this.right = right;
		correctEdges();
	}

	/**
	 * <b>Note</b>: A rectangle with a negative width or height is invalid. As
	 * such, this method <i>will swap the top and bottom edges</i> if the bottom
	 * is not the maximum Y.
	 * @param bottom the new maximum Y for this rect
	 */
	public void setBottom(double bottom) {
		if (recycled) throw new RecycledObjectException(this);
		edgy = true;
		this.bottom = bottom;
		correctEdges();
	}

	/**
	 * @param left the new minimum X for this rect
	 * @param top the new minimum Y for this rect
	 * @param right the new maximum X for this rect
	 * @param bottom the new maximum Y for this rect
	 */
	public void setEdges(double left, double top, double right, double bottom) {
		if (recycled) throw new RecycledObjectException(this);
		edgy = true;
		this.left = left;
		this.top = top;
		this.right = right;
		this.bottom = bottom;
		correctEdges();
	}

	/**
	 * @param rect the rect to copy
	 */
	public void set(RectD rect) {
		setEdges(rect.getLeft(), rect.getTop(), rect.getRight(), rect.getBottom());
	}

	/**
	 * @param rect the rect to copy
	 */
	public void set(RectI rect) {
		setEdges(rect.getLeft(), rect.getTop(), rect.getRight(), rect.getBottom());
	}

	// by size
	/**
	 * @return the minimum X of this rect
	 */
	public double getX() {
		if (recycled) throw new RecycledObjectException(this);
		return left;
	}
	/**
	 * @return the minimum Y of this rect
	 */
	public double getY() {
		if (recycled) throw new RecycledObjectException(this);
		return top;
	}
	/**
	 * @return the width of this rect
	 */
	public double getWidth() {
		if (recycled) throw new RecycledObjectException(this);
		return right-left;
	}
	/**
	 * @return the height of this rect
	 */
	public double getHeight() {
		if (recycled) throw new RecycledObjectException(this);
		return bottom-top;
	}

	/**
	 * Change the minimum X of this rect without also changing its width.
	 * @param x the new minimum X for this rect
	 */
	public void setX(double x) {
		if (recycled) throw new RecycledObjectException(this);
		edgy = false;
		double width = getWidth();
		this.left = x;
		this.right = x+width;
		correctEdges();
	}

	/**
	 * Change the minimum Y of this rect without also changing its height.
	 * @param y the new minimum Y for this rect
	 */
	public void setY(double y) {
		if (recycled) throw new RecycledObjectException(this);
		edgy = false;
		double height = getHeight();
		this.top = y;
		this.bottom = y+height;
		correctEdges();
	}

	/**
	 * Change the width of this rect without changing its minimum X. (i.e.
	 * shrink this rectangle, moving it toward the left.) This is probably how
	 * you expect setWidth to act, and is what you want for a unit rectangle
	 * with an X and Y of 0. Behaves like {@link #setWidthTowardRight} if width
	 * is negative, due to edge swapping. See {@link #setLeft}.
	 * @param width the new width for this rect
	 */
	public void setWidthTowardLeft(double width) {
		if (recycled) throw new RecycledObjectException(this);
		edgy = false;
		this.right = this.left+width;
		correctEdges();
	}

	/**
	 * Change the width of this rect without changing its maximum X. (i.e.
	 * shrink this rectangle, moving it toward the right.) Behaves like
	 * {@link #setWidthTowardLeft} if width is negative, due to edge swapping.
	 * See {@link #setRight}.
	 * @param width the new width for this rect
	 */
	public void setWidthTowardRight(double width) {
		if (recycled) throw new RecycledObjectException(this);
		edgy = false;
		this.left = this.right-width;
		correctEdges();
	}

	/**
	 * Change the width of this rect by changing its maximum and minimum X.
	 * (i.e. shrink this rectangle inward.) Negative widths act identically to
	 * positive widths of the same magnitude.
	 * @param width the new width for this rect
	 */
	public void setWidthCentered(double width) {
		if (recycled) throw new RecycledObjectException(this);
		edgy = false;
		double center = this.left + (this.right - this.left)/2;
		double halfWidth = width/2;
		this.left = center-halfWidth;
		this.right = center+halfWidth;
		correctEdges();
	}

	/**
	 * Change the height of this rect without changing its minimum Y. (i.e.
	 * shrink this rectangle, moving it toward the top.) This is probably how
	 * you expect setHeight to act, and is what you want for a unit rectangle
	 * with an X and Y of 0. Behaves like {@link #setHeightTowardBottom} if
	 * height is negative, due to edge swapping. See {@link #setTop}.
	 * @param height the new height for this rect
	 */
	public void setHeightTowardTop(double height) {
		if (recycled) throw new RecycledObjectException(this);
		edgy = false;
		this.bottom = this.top+height;
		correctEdges();
	}

	/**
	 * Change the height of this rect without changing its maximum Y. (i.e.
	 * shrink this rectangle, moving it toward the bottom.) Behaves like
	 * {@link #setHeightTowardTop} if height is negative, due to edge swapping.
	 * See {@link #setBottom}.
	 * @param height the new height for this rect
	 */
	public void setHeightTowardBottom(double height) {
		if (recycled) throw new RecycledObjectException(this);
		edgy = false;
		this.top = this.bottom-height;
		correctEdges();
	}

	/**
	 * Change the height of this rect by changing its maximum and minimum Y.
	 * (i.e. shrink this rectangle inward.) Negative heights act identically to
	 * positive heights of the same magnitude.
	 * @param height the new height for this rect
	 */
	public void setHeightCentered(double height) {
		if (recycled) throw new RecycledObjectException(this);
		edgy = false;
		double center = this.top + (this.bottom - this.top)/2;
		double halfHeight = height/2;
		this.top = center-halfHeight;
		this.bottom = center+halfHeight;
		correctEdges();
	}

	/**
	 * @param x the new minimum X for this rect
	 * @param y the new minimum Y for this rect
	 * @param width the new width for this rect
	 * @param height the new height for this rect
	 * @see #fromSize
	 */
	public void setSize(double x, double y, double width, double height) {
		if (recycled) throw new RecycledObjectException(this);
		edgy = false;
		this.left = x;
		this.top = y;
		this.right = x+width;
		this.bottom = y+height;
		correctEdges();
	}

	// misc

	/**
	 * Move this RectD by the given amount.
	 * @param x the amount to move horizontally
	 * @param y the amount to move vertically
	 */
	public void translate(double x, double y) {
		if (recycled) throw new RecycledObjectException(this);
		this.left += x;
		this.right += x;
		this.top += y;
		this.bottom += y;
	}

	/**
	 * @return {@code true} if the given point lies within this rectangle
	 */
	public boolean intersects(double x, double y) {
		if (recycled) throw new RecycledObjectException(this);
		return x >= left && y >= top && x <= right && y <= bottom;
	}

	/**
	 * @return {@code true} if the given rectangle intersects this one
	 */
	public boolean intersects(double left, double top, double right, double bottom) {
		if (recycled) throw new RecycledObjectException(this);
		return this.left < right && left < this.right &&
				this.top < bottom && top < this.bottom;
	}

	/**
	 * @return {@code true} if this rectangle wholly contains the given rectangle
	 */
	public boolean contains(double left, double top, double right, double bottom) {
		if (recycled) throw new RecycledObjectException(this);
		return left >= this.left && top >= this.top && right <= this.right && bottom <= this.bottom;
	}

	/**
	 * @return {@code true} if the given point lies within this rectangle
	 */
	public boolean intersects(Point2D point) {
		if (recycled) throw new RecycledObjectException(this);
		return intersects(point.getX(), point.getY());
	}

	/**
	 * @return {@code true} if the given rectangle intersects this one
	 */
	public boolean intersects(RectD other) {
		return intersects(other.getLeft(), other.getTop(), other.getRight(), other.getBottom());
	}

	/**
	 * @return {@code true} if this rectangle wholly contains the given rectangle
	 */
	public boolean contains(RectD other) {
		return contains(other.getLeft(), other.getTop(), other.getRight(), other.getBottom());
	}

	/**
	 * @return {@code true} if the given point lies within this rectangle
	 */
	public boolean intersects(Point2I point) {
		return intersects(point.getX(), point.getY());
	}

	/**
	 * @return {@code true} if the given rectangle intersects this one
	 */
	public boolean intersects(RectI other) {
		return intersects(other.getLeft(), other.getTop(), other.getRight(), other.getBottom());
	}

	/**
	 * @return {@code true} if this rectangle wholly contains the given rectangle
	 */
	public boolean contains(RectI other) {
		return contains(other.getLeft(), other.getTop(), other.getRight(), other.getBottom());
	}

	/**
	 * @return a point consisting of the minimum X and minimum Y of this rect
	 * 		(i.e. this rect's top-left corner)
	 */
	public Point2D getTopLeft() {
		return Point2D.from(left, top);
	}

	/**
	 * @return a point consisting of the maximum X and minimum Y of this rect
	 * 		(i.e. this rect's top-right corner)
	 */
	public Point2D getTopRight() {
		return Point2D.from(right, top);
	}

	/**
	 * @return a point consisting of the maximum X and maximum Y of this rect
	 * 		(i.e. this rect's bottom-right corner)
	 */
	public Point2D getBottomRight() {
		return Point2D.from(right, bottom);
	}

	/**
	 * @return a point consisting of the minimum X and maximum Y of this rect
	 * 		(i.e. this rect's bottom-left corner)
	 */
	public Point2D getBottomLeft() {
		return Point2D.from(left, bottom);
	}

	/**
	 * Returns a string in an unspecified format, for debugging purposes, that
	 * uses a heuristic to decide whether to delegate to {@link #toEdgeString}
	 * or {@link #toSizeString}. <b>Do not attempt to parse the returned
	 * string</b>. If you want a particular format, call a particlar method
	 * rather than trying to manipulate the heuristic.
	 * @return a string representation of this rect
	 */
	@Override
	public String toString() {
		return edgy ? toEdgeString() : toSizeString();
	}

	/**
	 * Returns a string in an unspecified format, for debugging purposes, that
	 * includes information useful to those using the edge methods like
	 * {@link #getRight}. <b>Do not attempt to parse the returned string</b>.
	 * @return a string representation of this rect, using edge information
	 */
	public String toEdgeString() {
		return "RectD{left="+left+", top="+top+", right="+right+", bottom="+bottom+"}";
	}

	/**
	 * Returns a string in an unspecified format, for debugging purposes, that
	 * includes information useful to those using the size methods like
	 * {@link #getWidth}. <b>Do not attempt to parse the returned string</b>.
	 * @return a string representation of this rect, using size information
	 */
	public String toSizeString() {
		return "RectD{x="+left+", y="+top+", width="+(right-left)+", height="+(bottom-top)+"}";
	}

	@Override
	public void recycle() {
		recycled = true;
		left = top = right = bottom = 0;
		edgy = false;
		pool.recycle(this);
	}

	/**
	 * Construct a RectD from edges. <b>Note</b>: A rectangle with a negative
	 * width or height is invalid. As such, this method <i>will swap pairs of
	 * edges</i> if they do not match their contracts.
	 * @param left the minimum X for the rect
	 * @param top the minimum Y for the rect
	 * @param right the maximum X for the rect
	 * @param bottom the maximum Y for the rect
	 * @return a RectD taken from the pool, initialized with the given values
	 */
	public static RectD fromEdges(double left, double top, double right, double bottom) {
		RectD r = pool.get();
		r.recycled = false;
		r.left = left;
		r.top = top;
		r.right = right;
		r.bottom = bottom;
		r.edgy = true;
		r.correctEdges();
		return r;
	}

	/**
	 * Construct a RectD from its top-left corner and a width and height.
	 * Negative width will result in the x being interpreted as the <i>right</i>
	 * edge. Negative height will result in the y being interpreted as the
	 * <i>bottom</i> edge.
	 * @param x the left edge of the rect
	 * @param y the top edge of the rect
	 * @param width the width of the rect
	 * @param height the height of the rect
	 */
	public static RectD fromSize(double x, double y, double width, double height) {
		RectD r = fromEdges(x, y, x+width, y+height);
		r.edgy = false;
		return r;
	}

	/**
	 * Get a RectD with all edges set to zero. Useful when you don't care
	 * what the initial values of the rect are.
	 */
	public static RectD get() {
		return fromEdges(0, 0, 0, 0);
	}

}
