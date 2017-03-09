package com.twentyhours.androidstudy.flexboxlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.twentyhours.androidstudy.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by soonhyung-imac on 3/7/17.
 */

public class FlexboxLayout extends ViewGroup {
  // its value should be one of the explicitly named constants.
  @IntDef({FLEX_DIRECTION_ROW, FLEX_DIRECTION_COLUMN})
  // Annotations are to be discarded by the compiler.
  @Retention(RetentionPolicy.SOURCE)
  public @interface FlexDirection {

  }

  public static final int FLEX_DIRECTION_ROW = 0;

  public static final int FLEX_DIRECTION_COLUMN = 2;

  /**
   * The direction children items are placed inside the Flexbox layout, it determines the
   * direction of the main axis (and the cross axis, perpendicular to the main axis).
   * <ul>
   * <li>
   * {@link #FLEX_DIRECTION_ROW}: Main axis direction -> horizontal. Main start to
   * main end -> Left to right (in LTR languages).
   * Cross start to cross end -> Top to bottom
   * </li>
   * <li>
   * {@link #FLEX_DIRECTION_COLUMN}: Main axis direction -> vertical. Main start
   * to main end -> Top to bottom. Cross start to cross end ->
   * Left to right (In LTR languages).
   * </li>
   * </ul>
   * The default value is {@link #FLEX_DIRECTION_ROW}.
   */
  private int mFlexDirection;

  @IntDef({FLEX_WRAP_NOWRAP, FLEX_WRAP_WRAP})
  @Retention(RetentionPolicy.SOURCE)
  public @interface FlexWrap {

  }

  public static final int FLEX_WRAP_NOWRAP = 0;

  public static final int FLEX_WRAP_WRAP = 1;

  /**
   * This attribute controls whether the flex container is single-line or multi-line, and the
   * direction of the cross axis.
   * <ul>
   * <li>{@link #FLEX_WRAP_NOWRAP}: The flex container is single-line.</li>
   * <li>{@link #FLEX_WRAP_WRAP}: The flex container is multi-line.</li>
   * </ul>
   * The default value is {@link #FLEX_WRAP_NOWRAP}.
   */
  private int mFlexWrap;

  private List<FlexLine> mFlexLines = new ArrayList<>();

  public FlexboxLayout(Context context) {
    this(context, null);
  }

  public FlexboxLayout(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public FlexboxLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    TypedArray a = context.obtainStyledAttributes(
        attrs, R.styleable.FlexboxLayout, defStyleAttr, 0);
    mFlexDirection = a.getInt(R.styleable.FlexboxLayout_flexDirection, FLEX_DIRECTION_ROW);
    mFlexWrap = a.getInt(R.styleable.FlexboxLayout_flexWrap, FLEX_WRAP_NOWRAP);
    a.recycle();
  }

  /**
   * onMeasure -> sets up measuredWidth/measuredHeight
   */
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    // TODO: Only calculate the children views which are affected from the last measure.

    switch (mFlexDirection) {
      case FLEX_DIRECTION_ROW:
        measureHorizontal(widthMeasureSpec, heightMeasureSpec);
        break;
      case FLEX_DIRECTION_COLUMN:
        measureVertical(widthMeasureSpec, heightMeasureSpec);
        break;
      default:
        throw new IllegalStateException(
            "Invalid value for the flex direction is set: " + mFlexDirection);
    }
  }

  @Override
  public void addView(View child, int index, ViewGroup.LayoutParams params) {
    // Create an array for the reordered indices before the View is added in the parent
    // ViewGroup since otherwise reordered indices won't be in effect before the
    // FlexboxLayout's onMeasure is called.
    // Because requestLayout is requested in the super.addView method.
    super.addView(child, index, params);
  }

  /**
   * Sub method for {@link #onMeasure(int, int)}, when the main axis direction is horizontal
   * (either left to right or right to left).
   *
   * @param widthMeasureSpec  horizontal space requirements as imposed by the parent
   * @param heightMeasureSpec vertical space requirements as imposed by the parent
   * @see #onMeasure(int, int)
   * @see #setFlexDirection(int)
   * @see #setFlexWrap(int)
   */
  private void measureHorizontal(int widthMeasureSpec, int heightMeasureSpec) {
    int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    int widthSize = MeasureSpec.getSize(widthMeasureSpec);
    int childState = 0;

    mFlexLines.clear();

    // Determine how many flex lines are needed in this layout by measuring each child.
    // (Expand or shrink the view depending on the flexGrow and flexShrink attributes in a later
    // loop)
    {
      int childCount = getChildCount();
      int paddingStart = ViewCompat.getPaddingStart(this);
      int paddingEnd = ViewCompat.getPaddingEnd(this);
      int largestHeightInRow = Integer.MIN_VALUE;
      FlexLine flexLine = new FlexLine();

      // The index of the view in a same flex line.
      int indexInFlexLine = 0;
      flexLine.mMainSize = paddingStart + paddingEnd;
      for (int i = 0; i < childCount; i++) {
        View child = getChildAt(i);
        if (child == null) {
          addFlexLineIfLastFlexItem(i, childCount, flexLine);
          continue;
        } else if (child.getVisibility() == View.GONE) {
          flexLine.mItemCount++;
          flexLine.mGoneItemCount++;
          addFlexLineIfLastFlexItem(i, childCount, flexLine);
          continue;
        }

        FlexboxLayout.LayoutParams lp = (LayoutParams) child.getLayoutParams();
        flexLine.mIndicesAlignSelfStretch.add(i);

        int childWidth = lp.width;
        int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
            getPaddingLeft() + getPaddingRight() + lp.leftMargin
                + lp.rightMargin, childWidth);
        int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
            getPaddingTop() + getPaddingBottom() + lp.topMargin
                + lp.bottomMargin, lp.height);
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);

        childState = ViewCompat
            .combineMeasuredStates(childState, ViewCompat.getMeasuredState(child));
        largestHeightInRow = Math.max(largestHeightInRow,
            child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);

        if (isWrapRequired(widthMode, widthSize, flexLine.mMainSize,
            child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin, lp,
            i, indexInFlexLine)) {
          if (flexLine.getItemCountNotGone() > 0) {
            addFlexLine(flexLine);
          }

          flexLine = new FlexLine();
          flexLine.mItemCount = 1;
          flexLine.mMainSize = paddingStart + paddingEnd;
          largestHeightInRow = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
          indexInFlexLine = 0;
        } else {
          flexLine.mItemCount++;
          indexInFlexLine++;
        }
        flexLine.mMainSize += child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
        // Temporarily set the cross axis length as the largest child in the row
        // Expand along the cross axis depending on the mAlignContent property if needed
        // later
        flexLine.mCrossSize = Math.max(flexLine.mCrossSize, largestHeightInRow);

        flexLine.mMaxBaseline = Math
            .max(flexLine.mMaxBaseline, child.getBaseline() + lp.topMargin);
        addFlexLineIfLastFlexItem(i, childCount, flexLine);
      }
    }

    determineMainSize(mFlexDirection, widthMeasureSpec, heightMeasureSpec);

    determineCrossSize(mFlexDirection, widthMeasureSpec, heightMeasureSpec,
        getPaddingTop() + getPaddingBottom());
    // Now cross size for each flex line is determined.
    // Expand the views if alignItems (or alignSelf in each child view) is set to stretch
    stretchViews(mFlexDirection);
    setMeasuredDimensionForFlex(mFlexDirection, widthMeasureSpec, heightMeasureSpec,
        childState);
  }

  /**
   * Sub method for {@link #onMeasure(int, int)} when the main axis direction is vertical
   * (either from top to bottom or bottom to top).
   *
   * @param widthMeasureSpec  horizontal space requirements as imposed by the parent
   * @param heightMeasureSpec vertical space requirements as imposed by the parent
   * @see #onMeasure(int, int)
   * @see #setFlexDirection(int)
   * @see #setFlexWrap(int)
   */
  private void measureVertical(int widthMeasureSpec, int heightMeasureSpec) {
    int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    int heightSize = MeasureSpec.getSize(heightMeasureSpec);
    int childState = 0;

    mFlexLines.clear();

    // Determine how many flex lines are needed in this layout by measuring each child.
    // (Expand or shrink the view depending on the flexGrow and flexShrink attributes in a later
    // loop)
    int childCount = getChildCount();
    int paddingTop = getPaddingTop();
    int paddingBottom = getPaddingBottom();
    int largestWidthInColumn = Integer.MIN_VALUE;
    FlexLine flexLine = new FlexLine();
    flexLine.mMainSize = paddingTop + paddingBottom;
    // The index of the view in a same flex line.
    int indexInFlexLine = 0;
    for (int i = 0; i < childCount; i++) {
      View child = getChildAt(i);
      if (child == null) {
        addFlexLineIfLastFlexItem(i, childCount, flexLine);
        continue;
      } else if (child.getVisibility() == View.GONE) {
        flexLine.mItemCount++;
        flexLine.mGoneItemCount++;
        addFlexLineIfLastFlexItem(i, childCount, flexLine);
        continue;
      }

      FlexboxLayout.LayoutParams lp = (LayoutParams) child.getLayoutParams();
      flexLine.mIndicesAlignSelfStretch.add(i);

      int childHeight = lp.height;

      int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
          getPaddingLeft() + getPaddingRight() + lp.leftMargin
              + lp.rightMargin, lp.width);
      int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
          getPaddingTop() + getPaddingBottom() + lp.topMargin
              + lp.bottomMargin, childHeight);
      child.measure(childWidthMeasureSpec, childHeightMeasureSpec);

      childState = ViewCompat
          .combineMeasuredStates(childState, ViewCompat.getMeasuredState(child));
      largestWidthInColumn = Math.max(largestWidthInColumn,
          child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);

      if (isWrapRequired(heightMode, heightSize, flexLine.mMainSize,
          child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin, lp,
          i, indexInFlexLine)) {
        if (flexLine.getItemCountNotGone() > 0) {
          addFlexLine(flexLine);
        }

        flexLine = new FlexLine();
        flexLine.mItemCount = 1;
        flexLine.mMainSize = paddingTop + paddingBottom;
        largestWidthInColumn = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
        indexInFlexLine = 0;
      } else {
        flexLine.mItemCount++;
        indexInFlexLine++;
      }
      flexLine.mMainSize += child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
      // Temporarily set the cross axis length as the largest child width in the column
      // Expand along the cross axis depending on the mAlignContent property if needed
      // later
      flexLine.mCrossSize = Math.max(flexLine.mCrossSize, largestWidthInColumn);

      addFlexLineIfLastFlexItem(i, childCount, flexLine);
    }

    determineMainSize(mFlexDirection, widthMeasureSpec, heightMeasureSpec);
    determineCrossSize(mFlexDirection, widthMeasureSpec, heightMeasureSpec,
        getPaddingLeft() + getPaddingRight());
    // Now cross size for each flex line is determined.
    // Expand the views if alignItems (or alignSelf in each child view) is set to stretch
    stretchViews(mFlexDirection);
    setMeasuredDimensionForFlex(mFlexDirection, widthMeasureSpec, heightMeasureSpec,
        childState);
  }

  private void addFlexLineIfLastFlexItem(int childIndex, int childCount, FlexLine flexLine) {
    if (childIndex == childCount - 1 && flexLine.getItemCountNotGone() != 0) {
      // Add the flex line if this item is the last item
      addFlexLine(flexLine);
    }
  }

  private void addFlexLine(FlexLine flexLine) {
    mFlexLines.add(flexLine);
  }

  /**
   * Determine the main size by expanding (shrinking if negative remaining free space is given)
   * an individual child in each flex line if any children's flexGrow (or flexShrink if remaining
   * space is negative) properties are set to non-zero.
   *
   * @param flexDirection     the value of the flex direction
   * @param widthMeasureSpec  horizontal space requirements as imposed by the parent
   * @param heightMeasureSpec vertical space requirements as imposed by the parent
   * @see #setFlexDirection(int)
   * @see #getFlexDirection()
   */
  private void determineMainSize(@FlexDirection int flexDirection, int widthMeasureSpec,
                                 int heightMeasureSpec) {
    int mainSize;
    int paddingAlongMainAxis;
    switch (flexDirection) {
      case FLEX_DIRECTION_ROW:
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        if (widthMode == MeasureSpec.EXACTLY) {
          mainSize = widthSize;
        } else {
          mainSize = getLargestMainSize();
        }
        paddingAlongMainAxis = getPaddingLeft() + getPaddingRight();
        break;
      case FLEX_DIRECTION_COLUMN:
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if (heightMode == MeasureSpec.EXACTLY) {
          mainSize = heightSize;
        } else {
          mainSize = getLargestMainSize();
        }
        paddingAlongMainAxis = getPaddingTop() + getPaddingBottom();
        break;
      default:
        throw new IllegalArgumentException("Invalid flex direction: " + flexDirection);
    }

    int childIndex = 0;
    for (FlexLine flexLine : mFlexLines) {
      if (flexLine.mMainSize < mainSize) {
        childIndex = expandFlexItems(widthMeasureSpec, heightMeasureSpec, flexLine,
            flexDirection, mainSize, paddingAlongMainAxis, childIndex);
      } else {
        childIndex = shrinkFlexItems(widthMeasureSpec, heightMeasureSpec, flexLine,
            flexDirection, mainSize, paddingAlongMainAxis, childIndex);
      }
    }
  }

  /**
   * Expand the flex items along the main axis based on the individual flexGrow attribute.
   *
   * @param widthMeasureSpec     the horizontal space requirements as imposed by the parent
   * @param heightMeasureSpec    the vertical space requirements as imposed by the parent
   * @param flexLine             the flex line to which flex items belong
   * @param flexDirection        the flexDirection value for this FlexboxLayout
   * @param maxMainSize          the maximum main size. Expanded main size will be this size
   * @param paddingAlongMainAxis the padding value along the main axis
   * @param startIndex           the start index of the children views to be expanded. This index
   *                             needs to
   *                             be an absolute index in the flex container (FlexboxLayout),
   *                             not the relative index in the flex line.
   * @return the next index, the next flex line's first flex item starts from the returned index
   * @see #getFlexDirection()
   * @see #setFlexDirection(int)
   */
  private int expandFlexItems(int widthMeasureSpec, int heightMeasureSpec, FlexLine flexLine,
                              @FlexDirection int flexDirection, int maxMainSize, int paddingAlongMainAxis,
                              int startIndex) {
    int childIndex = startIndex;
    if (flexLine.mTotalFlexGrow <= 0 || maxMainSize < flexLine.mMainSize) {
      childIndex += flexLine.mItemCount;
      return childIndex;
    }
    int sizeBeforeExpand = flexLine.mMainSize;
    boolean needsReexpand = false;
    float unitSpace = (maxMainSize - flexLine.mMainSize) / flexLine.mTotalFlexGrow;
    flexLine.mMainSize = paddingAlongMainAxis + flexLine.mDividerLengthInMainSize;

    // Setting the cross size of the flex line as the temporal value since the cross size of
    // each flex item may be changed from the initial calculation
    // (in the measureHorizontal/measureVertical method) even this method is part of the main
    // size determination.
    // E.g. If a TextView's layout_width is set to 0dp, layout_height is set to wrap_content,
    // and layout_flexGrow is set to 1, the TextView is trying to expand to the vertical
    // direction to enclose its content (in the measureHorizontal method), but
    // the width will be expanded in this method. In that case, the height needs to be measured
    // again with the expanded width.
    flexLine.mCrossSize = Integer.MIN_VALUE;
    int largestCrossSize = 0;
    float accumulatedRoundError = 0;
    for (int i = 0; i < flexLine.mItemCount; i++) {
      View child = getChildAt(childIndex);
      if (child == null) {
        continue;
      } else if (child.getVisibility() == View.GONE) {
        childIndex++;
        continue;
      }
      LayoutParams lp = (LayoutParams) child.getLayoutParams();
      if (isMainAxisDirectionHorizontal(flexDirection)) {
        // The direction of the main axis is horizontal
        float rawCalculatedWidth = child.getMeasuredWidth();
        if (i == flexLine.mItemCount - 1) {
          rawCalculatedWidth += accumulatedRoundError;
          accumulatedRoundError = 0;
        }
        int newWidth = Math.round(rawCalculatedWidth);
        accumulatedRoundError += (rawCalculatedWidth - newWidth);
        if (accumulatedRoundError > 1.0) {
          newWidth += 1;
          accumulatedRoundError -= 1.0;
        } else if (accumulatedRoundError < -1.0) {
          newWidth -= 1;
          accumulatedRoundError += 1.0;
        }
        int childHeightMeasureSpec = getChildHeightMeasureSpec(heightMeasureSpec, lp);
        child.measure(MeasureSpec.makeMeasureSpec(newWidth, MeasureSpec.EXACTLY),
            childHeightMeasureSpec);
        largestCrossSize = Math.max(largestCrossSize, child.getMeasuredHeight()
            + lp.topMargin + lp.bottomMargin);
        flexLine.mMainSize += child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
      } else {
        // The direction of the main axis is vertical
        float rawCalculatedHeight = child.getMeasuredHeight();
        if (i == flexLine.mItemCount - 1) {
          rawCalculatedHeight += accumulatedRoundError;
          accumulatedRoundError = 0;
        }
        int newHeight = Math.round(rawCalculatedHeight);
        accumulatedRoundError += (rawCalculatedHeight - newHeight);
        if (accumulatedRoundError > 1.0) {
          newHeight += 1;
          accumulatedRoundError -= 1.0;
        } else if (accumulatedRoundError < -1.0) {
          newHeight -= 1;
          accumulatedRoundError += 1.0;
        }
        int childWidthMeasureSpec = getChildWidthMeasureSpec(widthMeasureSpec, lp);
        child.measure(childWidthMeasureSpec,
            MeasureSpec.makeMeasureSpec(newHeight, MeasureSpec.EXACTLY));
        largestCrossSize = Math.max(largestCrossSize, child.getMeasuredWidth()
            + lp.leftMargin + lp.rightMargin);
        flexLine.mMainSize += child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
      }
      flexLine.mCrossSize = Math.max(flexLine.mCrossSize, largestCrossSize);
      childIndex++;
    }

    return childIndex;
  }

  /**
   * Shrink the flex items along the main axis based on the individual flexShrink attribute.
   *
   * @param widthMeasureSpec     the horizontal space requirements as imposed by the parent
   * @param heightMeasureSpec    the vertical space requirements as imposed by the parent
   * @param flexLine             the flex line to which flex items belong
   * @param flexDirection        the flexDirection value for this FlexboxLayout
   * @param maxMainSize          the maximum main size. Shrank main size will be this size
   * @param paddingAlongMainAxis the padding value along the main axis
   * @param startIndex           the start index of the children views to be shrank. This index
   *                             needs to
   *                             be an absolute index in the flex container (FlexboxLayout),
   *                             not the relative index in the flex line.
   * @return the next index, the next flex line's first flex item starts from the returned index
   * @see #getFlexDirection()
   * @see #setFlexDirection(int)
   */
  private int shrinkFlexItems(int widthMeasureSpec, int heightMeasureSpec, FlexLine flexLine,
                              @FlexDirection int flexDirection, int maxMainSize, int paddingAlongMainAxis,
                              int startIndex) {
    int childIndex = startIndex;
    int sizeBeforeShrink = flexLine.mMainSize;
    if (flexLine.mTotalFlexShrink <= 0 || maxMainSize > flexLine.mMainSize) {
      childIndex += flexLine.mItemCount;
      return childIndex;
    }
    boolean needsReshrink = false;
    float unitShrink = (flexLine.mMainSize - maxMainSize) / flexLine.mTotalFlexShrink;
    float accumulatedRoundError = 0;
    flexLine.mMainSize = paddingAlongMainAxis + flexLine.mDividerLengthInMainSize;

    // Setting the cross size of the flex line as the temporal value since the cross size of
    // each flex item may be changed from the initial calculation
    // (in the measureHorizontal/measureVertical method) even this method is part of the main
    // size determination.
    // E.g. If a TextView's layout_width is set to 0dp, layout_height is set to wrap_content,
    // and layout_flexGrow is set to 1, the TextView is trying to expand to the vertical
    // direction to enclose its content (in the measureHorizontal method), but
    // the width will be expanded in this method. In that case, the height needs to be measured
    // again with the expanded width.
    int largestCrossSize = 0;
    flexLine.mCrossSize = Integer.MIN_VALUE;
    for (int i = 0; i < flexLine.mItemCount; i++) {
      View child = getChildAt(childIndex);
      if (child == null) {
        continue;
      } else if (child.getVisibility() == View.GONE) {
        childIndex++;
        continue;
      }
      LayoutParams lp = (LayoutParams) child.getLayoutParams();
      if (isMainAxisDirectionHorizontal(flexDirection)) {
        // The direction of main axis is horizontal
        float rawCalculatedWidth = child.getMeasuredWidth();
        if (i == flexLine.mItemCount - 1) {
          rawCalculatedWidth += accumulatedRoundError;
          accumulatedRoundError = 0;
        }
        int newWidth = Math.round(rawCalculatedWidth);
        accumulatedRoundError += (rawCalculatedWidth - newWidth);
        if (accumulatedRoundError > 1.0) {
          newWidth += 1;
          accumulatedRoundError -= 1;
        } else if (accumulatedRoundError < -1.0) {
          newWidth -= 1;
          accumulatedRoundError += 1;
        }
        int childHeightMeasureSpec = getChildHeightMeasureSpec(heightMeasureSpec, lp);
        child.measure(MeasureSpec.makeMeasureSpec(newWidth, MeasureSpec.EXACTLY),
            childHeightMeasureSpec);
        largestCrossSize = Math.max(largestCrossSize, child.getMeasuredHeight()
            + lp.topMargin + lp.bottomMargin);
        flexLine.mMainSize += child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
      } else {
        // The direction of main axis is vertical
        float rawCalculatedHeight = child.getMeasuredHeight();
        if (i == flexLine.mItemCount - 1) {
          rawCalculatedHeight += accumulatedRoundError;
          accumulatedRoundError = 0;
        }
        int newHeight = Math.round(rawCalculatedHeight);
        accumulatedRoundError += (rawCalculatedHeight - newHeight);
        if (accumulatedRoundError > 1.0) {
          newHeight += 1;
          accumulatedRoundError -= 1;
        } else if (accumulatedRoundError < -1.0) {
          newHeight -= 1;
          accumulatedRoundError += 1;
        }
        int childWidthMeasureSpec = getChildWidthMeasureSpec(widthMeasureSpec, lp);
        child.measure(childWidthMeasureSpec,
            MeasureSpec.makeMeasureSpec(newHeight, MeasureSpec.EXACTLY));
        largestCrossSize = Math.max(largestCrossSize, child.getMeasuredWidth()
            + lp.leftMargin + lp.rightMargin);
        flexLine.mMainSize += child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
      }
      flexLine.mCrossSize = Math.max(flexLine.mCrossSize, largestCrossSize);
      childIndex++;
    }

    return childIndex;
  }

  private int getChildWidthMeasureSpec(int widthMeasureSpec, LayoutParams lp) {
    return getChildMeasureSpec(widthMeasureSpec,
        getPaddingLeft() + getPaddingRight() + lp.leftMargin
            + lp.rightMargin, lp.width);
  }

  private int getChildHeightMeasureSpec(int heightMeasureSpec, LayoutParams lp) {
    return getChildMeasureSpec(heightMeasureSpec,
        getPaddingTop() + getPaddingBottom() + lp.topMargin
            + lp.bottomMargin, lp.height);
  }

  /**
   * Determines the cross size (Calculate the length along the cross axis).
   * Expand the cross size only if the height mode is MeasureSpec.EXACTLY, otherwise
   * use the sum of cross sizes of all flex lines.
   *
   * @param flexDirection         the flex direction attribute
   * @param widthMeasureSpec      horizontal space requirements as imposed by the parent
   * @param heightMeasureSpec     vertical space requirements as imposed by the parent
   * @param paddingAlongCrossAxis the padding value for the FlexboxLayout along the cross axis
   * @see #getFlexDirection()
   * @see #setFlexDirection(int)
   */
  private void determineCrossSize(int flexDirection, int widthMeasureSpec,
                                  int heightMeasureSpec, int paddingAlongCrossAxis) {
    // The MeasureSpec mode along the cross axis
    int mode;
    // The MeasureSpec size along the cross axis
    int size;
    switch (flexDirection) {
      case FLEX_DIRECTION_ROW:
        mode = MeasureSpec.getMode(heightMeasureSpec);
        size = MeasureSpec.getSize(heightMeasureSpec);
        break;
      case FLEX_DIRECTION_COLUMN:
        mode = MeasureSpec.getMode(widthMeasureSpec);
        size = MeasureSpec.getSize(widthMeasureSpec);
        break;
      default:
        throw new IllegalArgumentException("Invalid flex direction: " + flexDirection);
    }
    if (mode == MeasureSpec.EXACTLY) {
      int totalCrossSize = getSumOfCrossSize() + paddingAlongCrossAxis;
      if (mFlexLines.size() == 1) {
        mFlexLines.get(0).mCrossSize = size - paddingAlongCrossAxis;
        // alignContent property is valid only if the Flexbox has at least two lines
      } else if (mFlexLines.size() >= 2 && totalCrossSize < size) {
        float freeSpaceUnit = (size - totalCrossSize) / (float) mFlexLines.size();
        float accumulatedError = 0;
        for (int i = 0, flexLinesSize = mFlexLines.size(); i < flexLinesSize; i++) {
          FlexLine flexLine = mFlexLines.get(i);
          float newCrossSizeAsFloat = flexLine.mCrossSize + freeSpaceUnit;
          if (i == mFlexLines.size() - 1) {
            newCrossSizeAsFloat += accumulatedError;
            accumulatedError = 0;
          }
          int newCrossSize = Math.round(newCrossSizeAsFloat);
          accumulatedError += (newCrossSizeAsFloat - newCrossSize);
          if (accumulatedError > 1) {
            newCrossSize += 1;
            accumulatedError -= 1;
          } else if (accumulatedError < -1) {
            newCrossSize -= 1;
            accumulatedError += 1;
          }
          flexLine.mCrossSize = newCrossSize;
        }
      }
    }
  }

  /**
   * Expand the view.
   *
   * @param flexDirection the flex direction attribute
   * @see #getFlexDirection()
   * @see #setFlexDirection(int)
   */
  private void stretchViews(int flexDirection) {
    int viewIndex = 0;
    for (FlexLine flexLine : mFlexLines) {
      for (int i = 0; i < flexLine.mItemCount; i++, viewIndex++) {
        View view = getChildAt(viewIndex);
        LayoutParams lp = (LayoutParams) view.getLayoutParams();
        switch (flexDirection) {
          case FLEX_DIRECTION_ROW:
            stretchViewVertically(view, flexLine.mCrossSize);
            break;
          case FLEX_DIRECTION_COLUMN:
            stretchViewHorizontally(view, flexLine.mCrossSize);
            break;
          default:
            throw new IllegalArgumentException(
                "Invalid flex direction: " + flexDirection);
        }
      }
    }
  }

  /**
   * Expand the view vertically to the size of the crossSize (considering the view margins)
   *
   * @param view      the View to be stretched
   * @param crossSize the cross size
   */
  private void stretchViewVertically(View view, int crossSize) {
    LayoutParams lp = (LayoutParams) view.getLayoutParams();
    int newHeight = crossSize - lp.topMargin - lp.bottomMargin;
    newHeight = Math.max(newHeight, 0);
    view.measure(MeasureSpec
            .makeMeasureSpec(view.getMeasuredWidth(), MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(newHeight, MeasureSpec.EXACTLY));
  }

  /**
   * Expand the view horizontally to the size of the crossSize (considering the view margins)
   *
   * @param view      the View to be stretched
   * @param crossSize the cross size
   */
  private void stretchViewHorizontally(View view, int crossSize) {
    LayoutParams lp = (LayoutParams) view.getLayoutParams();
    int newWidth = crossSize - lp.leftMargin - lp.rightMargin;
    newWidth = Math.max(newWidth, 0);
    view.measure(MeasureSpec
            .makeMeasureSpec(newWidth, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(view.getMeasuredHeight(), MeasureSpec.EXACTLY));
  }

  /**
   * Set this FlexboxLayouts' width and height depending on the calculated size of main axis and
   * cross axis.
   *
   * @param flexDirection     the value of the flex direction
   * @param widthMeasureSpec  horizontal space requirements as imposed by the parent
   * @param heightMeasureSpec vertical space requirements as imposed by the parent
   * @param childState        the child state of the View
   * @see #getFlexDirection()
   * @see #setFlexDirection(int)
   */
  private void setMeasuredDimensionForFlex(@FlexDirection int flexDirection, int widthMeasureSpec,
                                           int heightMeasureSpec, int childState) {
    int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    int widthSize = MeasureSpec.getSize(widthMeasureSpec);
    int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    int heightSize = MeasureSpec.getSize(heightMeasureSpec);
    int calculatedMaxHeight;
    int calculatedMaxWidth;
    switch (flexDirection) {
      case FLEX_DIRECTION_ROW:
        calculatedMaxHeight = getSumOfCrossSize() + getPaddingTop()
            + getPaddingBottom();
        calculatedMaxWidth = getLargestMainSize();
        break;
      case FLEX_DIRECTION_COLUMN:
        calculatedMaxHeight = getLargestMainSize();
        calculatedMaxWidth = getSumOfCrossSize() + getPaddingLeft() + getPaddingRight();
        break;
      default:
        throw new IllegalArgumentException("Invalid flex direction: " + flexDirection);
    }

    int widthSizeAndState;
    switch (widthMode) {
      case MeasureSpec.EXACTLY:
        if (widthSize < calculatedMaxWidth) {
          childState = ViewCompat
              .combineMeasuredStates(childState, ViewCompat.MEASURED_STATE_TOO_SMALL);
        }
        widthSizeAndState = ViewCompat.resolveSizeAndState(widthSize, widthMeasureSpec,
            childState);
        break;
      case MeasureSpec.AT_MOST: {
        if (widthSize < calculatedMaxWidth) {
          childState = ViewCompat
              .combineMeasuredStates(childState, ViewCompat.MEASURED_STATE_TOO_SMALL);
        } else {
          widthSize = calculatedMaxWidth;
        }
        widthSizeAndState = ViewCompat.resolveSizeAndState(widthSize, widthMeasureSpec,
            childState);
        break;
      }
      case MeasureSpec.UNSPECIFIED: {
        widthSizeAndState = ViewCompat
            .resolveSizeAndState(calculatedMaxWidth, widthMeasureSpec, childState);
        break;
      }
      default:
        throw new IllegalStateException("Unknown width mode is set: " + widthMode);
    }
    int heightSizeAndState;
    switch (heightMode) {
      case MeasureSpec.EXACTLY:
        if (heightSize < calculatedMaxHeight) {
          childState = ViewCompat.combineMeasuredStates(childState,
              ViewCompat.MEASURED_STATE_TOO_SMALL
                  >> ViewCompat.MEASURED_HEIGHT_STATE_SHIFT);
        }
        heightSizeAndState = ViewCompat.resolveSizeAndState(heightSize, heightMeasureSpec,
            childState);
        break;
      case MeasureSpec.AT_MOST: {
        if (heightSize < calculatedMaxHeight) {
          childState = ViewCompat.combineMeasuredStates(childState,
              ViewCompat.MEASURED_STATE_TOO_SMALL
                  >> ViewCompat.MEASURED_HEIGHT_STATE_SHIFT);
        } else {
          heightSize = calculatedMaxHeight;
        }
        heightSizeAndState = ViewCompat.resolveSizeAndState(heightSize, heightMeasureSpec,
            childState);
        break;
      }
      case MeasureSpec.UNSPECIFIED: {
        heightSizeAndState = ViewCompat.resolveSizeAndState(calculatedMaxHeight,
            heightMeasureSpec, childState);
        break;
      }
      default:
        throw new IllegalStateException("Unknown height mode is set: " + heightMode);
    }
    setMeasuredDimension(widthSizeAndState, heightSizeAndState);
  }

  /**
   * Determine if a wrap is required (add a new flex line).
   *
   * @param mode          the width or height mode along the main axis direction
   * @param maxSize       the max size along the main axis direction
   * @param currentLength the accumulated current length
   * @param childLength   the length of a child view which is to be collected to the flex line
   * @param lp            the LayoutParams for the view being determined whether a new flex line
   *                      is needed
   * @return {@code true} if a wrap is required, {@code false} otherwise
   * @see #getFlexWrap()
   * @see #setFlexWrap(int)
   */
  private boolean isWrapRequired(int mode, int maxSize, int currentLength, int childLength,
                                 LayoutParams lp, int childAbsoluteIndex, int childRelativeIndexInFlexLine) {
    if (mFlexWrap == FLEX_WRAP_NOWRAP) {
      return false;
    }
    if (mode == MeasureSpec.UNSPECIFIED) {
      return false;
    }

    return maxSize < currentLength + childLength;
  }

  /**
   * Retrieve the largest main size of all flex lines.
   *
   * @return the largest main size
   */
  private int getLargestMainSize() {
    int largestSize = Integer.MIN_VALUE;
    for (FlexLine flexLine : mFlexLines) {
      largestSize = Math.max(largestSize, flexLine.mMainSize);
    }
    return largestSize;
  }

  /**
   * Retrieve the sum of the cross sizes of all flex lines including divider lengths.
   *
   * @return the sum of the cross sizes
   */
  private int getSumOfCrossSize() {
    int sum = 0;
    for (int i = 0, size = mFlexLines.size(); i < size; i++) {
      FlexLine flexLine = mFlexLines.get(i);

      sum += flexLine.mCrossSize;
    }
    return sum;
  }

  private boolean isMainAxisDirectionHorizontal(@FlexDirection int flexDirection) {
    return flexDirection == FLEX_DIRECTION_ROW;
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    int layoutDirection = ViewCompat.getLayoutDirection(this);
    boolean isRtl;
    switch (mFlexDirection) {
      case FLEX_DIRECTION_ROW:
        isRtl = layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL;
        layoutHorizontal(isRtl, left, top, right, bottom);
        break;
      case FLEX_DIRECTION_COLUMN:
        isRtl = layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL;
        layoutVertical(isRtl, false, left, top, right, bottom);
        break;
      default:
        throw new IllegalStateException("Invalid flex direction is set: " + mFlexDirection);
    }
  }

  /**
   * Sub method for {@link #onLayout(boolean, int, int, int, int)} when the
   * {@link #mFlexDirection} is {@link #FLEX_DIRECTION_ROW}.
   *
   * @param isRtl  {@code true} if the horizontal layout direction is right to left, {@code
   *               false} otherwise.
   * @param left   the left position of this View
   * @param top    the top position of this View
   * @param right  the right position of this View
   * @param bottom the bottom position of this View
   * @see #getFlexWrap()
   * @see #setFlexWrap(int)
   */
  private void layoutHorizontal(boolean isRtl, int left, int top, int right, int bottom) {
    int paddingLeft = getPaddingLeft();
    int paddingRight = getPaddingRight();
    // Use float to reduce the round error that may happen in when justifyContent ==
    // SPACE_BETWEEN or SPACE_AROUND
    float childLeft;
    int currentViewIndex = 0;

    int height = bottom - top;
    int width = right - left;
    // childBottom is used if the mFlexWrap is FLEX_WRAP_WRAP_REVERSE otherwise
    // childTop is used to align the vertical position of the children views.
    int childBottom = height - getPaddingBottom();
    int childTop = getPaddingTop();

    // Used only for RTL layout
    // Use float to reduce the round error that may happen in when justifyContent ==
    // SPACE_BETWEEN or SPACE_AROUND
    float childRight;
    for (int i = 0, size = mFlexLines.size(); i < size; i++) {
      FlexLine flexLine = mFlexLines.get(i);
      float spaceBetweenItem = 0f;
      childLeft = paddingLeft;
      childRight = width - paddingRight;
      spaceBetweenItem = Math.max(spaceBetweenItem, 0);

      for (int j = 0; j < flexLine.mItemCount; j++) {
        View child = getChildAt(currentViewIndex);
        if (child == null) {
          continue;
        } else if (child.getVisibility() == View.GONE) {
          currentViewIndex++;
          continue;
        }
        LayoutParams lp = ((LayoutParams) child.getLayoutParams());
        childLeft += lp.leftMargin;
        childRight -= lp.rightMargin;

        if (isRtl) {
          layoutSingleChildHorizontal(child, flexLine, mFlexWrap,
              Math.round(childRight) - child.getMeasuredWidth(), childTop,
              Math.round(childRight), childTop + child.getMeasuredHeight());
        } else {
          layoutSingleChildHorizontal(child, flexLine, mFlexWrap,
              Math.round(childLeft), childTop,
              Math.round(childLeft) + child.getMeasuredWidth(),
              childTop + child.getMeasuredHeight());
        }
        childLeft += child.getMeasuredWidth() + spaceBetweenItem + lp.rightMargin;
        childRight -= child.getMeasuredWidth() + spaceBetweenItem + lp.leftMargin;
        currentViewIndex++;

        flexLine.mLeft = Math.min(flexLine.mLeft, child.getLeft() - lp.leftMargin);
        flexLine.mTop = Math.min(flexLine.mTop, child.getTop() - lp.topMargin);
        flexLine.mRight = Math.max(flexLine.mRight, child.getRight() + lp.rightMargin);
        flexLine.mBottom = Math.max(flexLine.mBottom, child.getBottom() + lp.bottomMargin);
      }
      childTop += flexLine.mCrossSize;
      childBottom -= flexLine.mCrossSize;
    }
  }

  /**
   * Place a single View when the layout direction is horizontal ({@link #mFlexDirection} is
   * {@link #FLEX_DIRECTION_ROW}).
   *
   * @param view       the View to be placed
   * @param flexLine   the {@link FlexLine} where the View belongs to
   * @param flexWrap   the flex wrap attribute of this FlexboxLayout
   * @param left       the left position of the View, which the View's margin is already taken
   *                   into account
   * @param top        the top position of the flex line where the View belongs to. The actual
   *                   View's top position is shifted depending on the flexWrap and alignItems
   *                   attributes
   * @param right      the right position of the View, which the View's margin is already taken
   *                   into account
   * @param bottom     the bottom position of the flex line where the View belongs to. The actual
   *                   View's bottom position is shifted depending on the flexWrap and alignItems
   *                   attributes
   */
  private void layoutSingleChildHorizontal(View view, FlexLine flexLine, @FlexWrap int flexWrap,
                                           int left, int top, int right, int bottom) {
    LayoutParams lp = (LayoutParams) view.getLayoutParams();
    view.layout(left, top + lp.topMargin, right, bottom + lp.topMargin);
  }

  /**
   * Sub method for {@link #onLayout(boolean, int, int, int, int)} when the
   * {@link #mFlexDirection} is {@link #FLEX_DIRECTION_COLUMN}.
   *
   * @param isRtl           {@code true} if the horizontal layout direction is right to left,
   *                        {@code false}
   *                        otherwise
   * @param fromBottomToTop {@code true} if the layout direction is bottom to top, {@code false}
   *                        otherwise
   * @param left            the left position of this View
   * @param top             the top position of this View
   * @param right           the right position of this View
   * @param bottom          the bottom position of this View
   * @see #getFlexWrap()
   * @see #setFlexWrap(int)
   */
  private void layoutVertical(boolean isRtl, boolean fromBottomToTop, int left, int top,
                              int right, int bottom) {
    int paddingTop = getPaddingTop();
    int paddingBottom = getPaddingBottom();

    int paddingRight = getPaddingRight();
    int childLeft = getPaddingLeft();
    int currentViewIndex = 0;

    int width = right - left;
    int height = bottom - top;
    // childRight is used if the mFlexWrap is FLEX_WRAP_WRAP_REVERSE otherwise
    // childLeft is used to align the horizontal position of the children views.
    int childRight = width - paddingRight;

    // Use float to reduce the round error that may happen in when justifyContent ==
    // SPACE_BETWEEN or SPACE_AROUND
    float childTop;

    // Used only for if the direction is from bottom to top
    float childBottom;

    for (int i = 0, size = mFlexLines.size(); i < size; i++) {
      FlexLine flexLine = mFlexLines.get(i);
      float spaceBetweenItem = 0f;
      childTop = paddingTop;
      childBottom = height - paddingBottom;
      spaceBetweenItem = Math.max(spaceBetweenItem, 0);

      for (int j = 0; j < flexLine.mItemCount; j++) {
        View child = getChildAt(currentViewIndex);
        if (child == null) {
          continue;
        } else if (child.getVisibility() == View.GONE) {
          currentViewIndex++;
          continue;
        }
        LayoutParams lp = ((LayoutParams) child.getLayoutParams());
        childTop += lp.topMargin;
        childBottom -= lp.bottomMargin;
        if (isRtl) {
          if (fromBottomToTop) {
            layoutSingleChildVertical(child, flexLine, true,
                childRight - child.getMeasuredWidth(),
                Math.round(childBottom) - child.getMeasuredHeight(), childRight,
                Math.round(childBottom));
          } else {
            layoutSingleChildVertical(child, flexLine, true,
                childRight - child.getMeasuredWidth(), Math.round(childTop),
                childRight, Math.round(childTop) + child.getMeasuredHeight());
          }
        } else {
          if (fromBottomToTop) {
            layoutSingleChildVertical(child, flexLine, false,
                childLeft, Math.round(childBottom) - child.getMeasuredHeight(),
                childLeft + child.getMeasuredWidth(), Math.round(childBottom));
          } else {
            layoutSingleChildVertical(child, flexLine, false,
                childLeft, Math.round(childTop),
                childLeft + child.getMeasuredWidth(),
                Math.round(childTop) + child.getMeasuredHeight());
          }
        }
        childTop += child.getMeasuredHeight() + spaceBetweenItem + lp.bottomMargin;
        childBottom -= child.getMeasuredHeight() + spaceBetweenItem + lp.topMargin;
        currentViewIndex++;

        flexLine.mLeft = Math.min(flexLine.mLeft, child.getLeft() - lp.leftMargin);
        flexLine.mTop = Math.min(flexLine.mTop, child.getTop() - lp.topMargin);
        flexLine.mRight = Math.max(flexLine.mRight, child.getRight() + lp.rightMargin);
        flexLine.mBottom = Math.max(flexLine.mBottom, child.getBottom() + lp.bottomMargin);
      }
      childLeft += flexLine.mCrossSize;
      childRight -= flexLine.mCrossSize;
    }
  }

  /**
   * Place a single View when the layout direction is vertical ({@link #mFlexDirection} is
   * {@link #FLEX_DIRECTION_COLUMN}).
   *
   * @param view       the View to be placed
   * @param flexLine   the {@link FlexLine} where the View belongs to
   * @param isRtl      {@code true} if the layout direction is right to left, {@code false}
   *                   otherwise
   * @param left       the left position of the flex line where the View belongs to. The actual
   *                   View's left position is shifted depending on the isRtl and alignItems
   *                   attributes
   * @param top        the top position of the View, which the View's margin is already taken
   *                   into account
   * @param right      the right position of the flex line where the View belongs to. The actual
   *                   View's right position is shifted depending on the isRtl and alignItems
   *                   attributes
   * @param bottom     the bottom position of the View, which the View's margin is already taken
   *                   into account
   */
  private void layoutSingleChildVertical(View view, FlexLine flexLine, boolean isRtl,
                                         int left, int top, int right, int bottom) {
    LayoutParams lp = (LayoutParams) view.getLayoutParams();
    if (!isRtl) {
      view.layout(left + lp.leftMargin, top, right + lp.leftMargin, bottom);
    } else {
      view.layout(left - lp.rightMargin, top, right - lp.rightMargin, bottom);
    }
  }

  @Override
  protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
    return p instanceof FlexboxLayout.LayoutParams;
  }

  @Override
  public LayoutParams generateLayoutParams(AttributeSet attrs) {
    return new FlexboxLayout.LayoutParams(getContext(), attrs);
  }

  @Override
  protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
    return new LayoutParams(p);
  }

  @FlexDirection
  public int getFlexDirection() {
    return mFlexDirection;
  }

  public void setFlexDirection(@FlexDirection int flexDirection) {
    if (mFlexDirection != flexDirection) {
      mFlexDirection = flexDirection;
      requestLayout();
    }
  }

  @FlexWrap
  public int getFlexWrap() {
    return mFlexWrap;
  }

  public void setFlexWrap(@FlexWrap int flexWrap) {
    if (mFlexWrap != flexWrap) {
      mFlexWrap = flexWrap;
      requestLayout();
    }
  }

  /**
   * @return the flex lines composing this flex container. This method returns a copy of the
   * original list excluding a dummy flex line (flex line that doesn't have any flex items in it
   * but used for the alignment along the cross axis).
   * Thus any changes of the returned list are not reflected to the original list.
   */
  public List<FlexLine> getFlexLines() {
    List<FlexLine> result = new ArrayList<>(mFlexLines.size());
    for (FlexLine flexLine : mFlexLines) {
      if (flexLine.getItemCountNotGone() == 0) {
        continue;
      }
      result.add(flexLine);
    }
    return result;
  }

  private void setWillNotDrawFlag() {
    setWillNotDraw(true);
  }

  /**
   * Per child parameters for children views of the {@link FlexboxLayout}.
   */
  public static class LayoutParams extends ViewGroup.MarginLayoutParams {

    public LayoutParams(Context context, AttributeSet attrs) {
      super(context, attrs);
    }

    public LayoutParams(LayoutParams source) {
      super(source);
    }

    public LayoutParams(ViewGroup.LayoutParams source) {
      super(source);
    }

    public LayoutParams(int width, int height) {
      super(new ViewGroup.LayoutParams(width, height));
    }
  }

  /**
   * A class that is used for calculating the view order which view's indices and order
   * properties from Flexbox are taken into account.
   */
  private static class Order implements Comparable<Order> {

    /** {@link View}'s index */
    int index;

    /** order property in the Flexbox */
    int order;

    @Override
    public int compareTo(@NonNull Order another) {
      if (order != another.order) {
        return order - another.order;
      }
      return index - another.index;
    }

    @Override
    public String toString() {
      return "Order{" +
          "order=" + order +
          ", index=" + index +
          '}';
    }
  }
}
