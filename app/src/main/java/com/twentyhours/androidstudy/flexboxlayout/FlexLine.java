package com.twentyhours.androidstudy.flexboxlayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by soonhyung-imac on 3/7/17.
 */

public class FlexLine {
  FlexLine() {
  }

  /** @see {@link #getLeft()} */
  int mLeft = Integer.MAX_VALUE;

  /** @see {@link #getTop()} */
  int mTop = Integer.MAX_VALUE;

  /** @see {@link #getRight()} */
  int mRight = Integer.MIN_VALUE;

  /** @see {@link #getBottom()} */
  int mBottom = Integer.MIN_VALUE;

  /** @see {@link #getMainSize()} */
  int mMainSize;

  /** @see {@link #getCrossSize()} */
  int mCrossSize;

  /** @see {@link #getItemCount()} */
  int mItemCount;

  /** Holds the count of the views whose visibilities are gone */
  int mGoneItemCount;

  /**
   * @return the distance in pixels from the top edge of this view's parent
   * to the top edge of this FlexLine.
   */
  public int getLeft() {
    return mLeft;
  }

  /**
   * @return the distance in pixels from the top edge of this view's parent
   * to the top edge of this FlexLine.
   */
  public int getTop() {
    return mTop;
  }

  /**
   * @return the distance in pixels from the right edge of this view's parent
   * to the right edge of this FlexLine.
   */
  public int getRight() {
    return mRight;
  }

  /**
   * @return the distance in pixels from the bottom edge of this view's parent
   * to the bottom edge of this FlexLine.
   */
  public int getBottom() {
    return mBottom;
  }

  /**
   * @return the size of the flex line in pixels along the main axis of the flex container.
   */
  public int getMainSize() {
    return mMainSize;
  }

  /**
   * @return the size of the flex line in pixels along the cross axis of the flex container.
   */
  public int getCrossSize() {
    return mCrossSize;
  }

  /**
   * @return the count of the views contained in this flex line.
   */
  public int getItemCount() {
    return mItemCount;
  }

  /**
   * @return the count of the views whose visibilities are not gone in this flex line.
   */
  public int getItemCountNotGone() {
    return mItemCount - mGoneItemCount;
  }
}
