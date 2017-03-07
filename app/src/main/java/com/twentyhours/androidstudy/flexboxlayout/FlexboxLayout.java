package com.twentyhours.androidstudy.flexboxlayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;

import com.twentyhours.androidstudy.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by soonhyung-imac on 3/7/17.
 */

public class FlexboxLayout extends ViewGroup {
  // its value should be one of the explicitly named constants.
  @IntDef({FLEX_DIRECTION_ROW, FLEX_DIRECTION_ROW_REVERSE, FLEX_DIRECTION_COLUMN,
      FLEX_DIRECTION_COLUMN_REVERSE})
  // Annotations are to be discarded by the compiler.
  @Retention(RetentionPolicy.SOURCE)
  public @interface FlexDirection {

  }

  public static final int FLEX_DIRECTION_ROW = 0;
  public static final int FLEX_DIRECTION_ROW_REVERSE = 1;
  public static final int FLEX_DIRECTION_COLUMN = 2;
  public static final int FLEX_DIRECTION_COLUMN_REVERSE = 3;

  private int mFlexDirection;

  @IntDef({FLEX_WRAP_NOWRAP, FLEX_WRAP_WRAP, FLEX_WRAP_WRAP_REVERSE})
  @Retention(RetentionPolicy.SOURCE)
  public @interface FlexWrap {

  }

  public static final int FLEX_WRAP_NOWRAP = 0;
  public static final int FLEX_WRAP_WRAP = 1;
  public static final int FLEX_WRAP_WRAP_REVERSE = 2;

  private int mFlexWrap;

  @IntDef({JUSTIFY_CONTENT_FLEX_START, JUSTIFY_CONTENT_FLEX_END, JUSTIFY_CONTENT_CENTER,
      JUSTIFY_CONTENT_SPACE_BETWEEN, JUSTIFY_CONTENT_SPACE_AROUND})
  @Retention(RetentionPolicy.SOURCE)
  public @interface JustifyContent {

  }

  public static final int JUSTIFY_CONTENT_FLEX_START = 0;
  public static final int JUSTIFY_CONTENT_FLEX_END = 1;
  public static final int JUSTIFY_CONTENT_CENTER = 2;
  public static final int JUSTIFY_CONTENT_SPACE_BETWEEN = 3;
  public static final int JUSTIFY_CONTENT_SPACE_AROUND = 4;

  private int mJustifyContent;

  @IntDef({ALIGN_ITEMS_FLEX_START, ALIGN_ITEMS_FLEX_END, ALIGN_ITEMS_CENTER,
      ALIGN_ITEMS_BASELINE, ALIGN_ITEMS_STRETCH})
  @Retention(RetentionPolicy.SOURCE)
  public @interface AlignItems {

  }

  public static final int ALIGN_ITEMS_FLEX_START = 0;

  public static final int ALIGN_ITEMS_FLEX_END = 1;

  public static final int ALIGN_ITEMS_CENTER = 2;

  public static final int ALIGN_ITEMS_BASELINE = 3;

  public static final int ALIGN_ITEMS_STRETCH = 4;

  private int mAlignItems;


  @IntDef({ALIGN_CONTENT_FLEX_START, ALIGN_CONTENT_FLEX_END, ALIGN_CONTENT_CENTER,
      ALIGN_CONTENT_SPACE_BETWEEN, ALIGN_CONTENT_SPACE_AROUND, ALIGN_CONTENT_STRETCH})
  @Retention(RetentionPolicy.SOURCE)
  public @interface AlignContent {

  }

  public static final int ALIGN_CONTENT_FLEX_START = 0;

  public static final int ALIGN_CONTENT_FLEX_END = 1;

  public static final int ALIGN_CONTENT_CENTER = 2;

  public static final int ALIGN_CONTENT_SPACE_BETWEEN = 3;

  public static final int ALIGN_CONTENT_SPACE_AROUND = 4;

  public static final int ALIGN_CONTENT_STRETCH = 5;

  private int mAlignContent;

  // If the IntDef#flag() attribute is set to true, multiple constants can be combined.
  @IntDef(flag = true,
      value = {
          SHOW_DIVIDER_NONE,
          SHOW_DIVIDER_BEGINNING,
          SHOW_DIVIDER_MIDDLE,
          SHOW_DIVIDER_END
      })
  @Retention(RetentionPolicy.SOURCE)
  public @interface DividerMode {

  }

  public static final int SHOW_DIVIDER_NONE = 0;
  public static final int SHOW_DIVIDER_BEGINNING = 1;
  public static final int SHOW_DIVIDER_MIDDLE = 1 << 1;
  public static final int SHOW_DIVIDER_END = 1 << 2;
  private Drawable mDividerDrawableHorizontal;
  private Drawable mDividerDrawableVertical;

  private int mShowDividerHorizontal;
  private int mShowDividerVertical;
  private int mDividerHorizontalHeight;
  private int mDividerVerticalWidth;
  private int[] mReorderedIndices;
  // Caches the {@link LayoutParams#order} attributes for children views.
  private SparseIntArray mOrderCache;
  private List<FlexLine> mFlexLines = new ArrayList<>();
  private boolean[] mChildrenFrozen;

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
    mJustifyContent = a
        .getInt(R.styleable.FlexboxLayout_justifyContent, JUSTIFY_CONTENT_FLEX_START);
    mAlignItems = a.getInt(R.styleable.FlexboxLayout_alignItems, ALIGN_ITEMS_STRETCH);
    mAlignContent = a.getInt(R.styleable.FlexboxLayout_alignContent, ALIGN_CONTENT_STRETCH);
    Drawable drawable = a.getDrawable(R.styleable.FlexboxLayout_dividerDrawable);
    if (drawable != null) {
      setDividerDrawableHorizontal(drawable);
      setDividerDrawableVertical(drawable);
    }
    Drawable drawableHorizontal = a
        .getDrawable(R.styleable.FlexboxLayout_dividerDrawableHorizontal);
    if (drawableHorizontal != null) {
      setDividerDrawableHorizontal(drawableHorizontal);
    }
    Drawable drawableVertical = a
        .getDrawable(R.styleable.FlexboxLayout_dividerDrawableVertical);
    if (drawableVertical != null) {
      setDividerDrawableVertical(drawableVertical);
    }
    int dividerMode = a.getInt(R.styleable.FlexboxLayout_showDivider, SHOW_DIVIDER_NONE);
    if (dividerMode != SHOW_DIVIDER_NONE) {
      mShowDividerVertical = dividerMode;
      mShowDividerHorizontal = dividerMode;
    }
    int dividerModeVertical = a
        .getInt(R.styleable.FlexboxLayout_showDividerVertical, SHOW_DIVIDER_NONE);
    if (dividerModeVertical != SHOW_DIVIDER_NONE) {
      mShowDividerVertical = dividerModeVertical;
    }
    int dividerModeHorizontal = a
        .getInt(R.styleable.FlexboxLayout_showDividerHorizontal, SHOW_DIVIDER_NONE);
    if (dividerModeHorizontal != SHOW_DIVIDER_NONE) {
      mShowDividerHorizontal = dividerModeHorizontal;
    }
    a.recycle();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    if (isOrderChangedFromLastMeasurement()) {
      mReorderedIndices = createReorderedIndices();
    }
  }

  /**
   * check to see if the order of children view is changed
   */
  private boolean isOrderChangedFromLastMeasurement() {
    int childCount = getChildCount();
    if (mOrderCache == null) {
      mOrderCache = new SparseIntArray(childCount);
    }
    if (mOrderCache.size() != childCount) {
      return true;
    }
    for (int i = 0; i < childCount; i++) {
      View view = getChildAt(i);
      if (view == null) {
        continue;
      }
      LayoutParams lp = (LayoutParams) view.getLayoutParams();
      if (lp.order != mOrderCache.get(i)) {
        return true;
      }
    }
    return false;
  }

  private int[] createReorderedIndices() {
    int childCount = getChildCount();
    List<Order> orders = createOrders(childCount);
    return sortOrdersIntoReorderedIndices(childCount, orders);
  }

  private int[] sortOrdersIntoReorderedIndices(int childCount, List<Order> orders) {
    Collections.sort(orders); // reorder orders
    if (mOrderCache == null) {
      mOrderCache = new SparseIntArray(childCount);
    }
    mOrderCache.clear();
    int[] reorderedIndices = new int[childCount];
    int i = 0;
    for (Order order : orders) {
      // reorderedIndices has indices reordered by order
      reorderedIndices[i] = order.index;
      mOrderCache.append(i, order.order);
      i++;
    }
    return reorderedIndices;
  }

  @NonNull
  private List<Order> createOrders(int childCount) {
    List<Order> orders = new ArrayList<>(childCount);
    for (int i = 0; i < childCount; i++) {
      View child = getChildAt(i);
      LayoutParams params = (LayoutParams) child.getLayoutParams();
      Order order = new Order();
      order.order = params.order;
      order.index = i;
      orders.add(order);
    }
    return orders;
  }

  @Override
  protected void onLayout(boolean b, int i, int i1, int i2, int i3) {

  }

  public void setDividerDrawableHorizontal(Drawable divider) {
    if (divider == mDividerDrawableHorizontal) {
      return;
    }
    mDividerDrawableHorizontal = divider;
    if (divider != null) {
      mDividerHorizontalHeight = divider.getIntrinsicHeight();
    } else {
      mDividerHorizontalHeight = 0;
    }
    setWillNotDrawFlag();
    requestLayout();
  }

  public void setDividerDrawableVertical(Drawable divider) {
    if (divider == mDividerDrawableVertical) {
      return;
    }
    mDividerDrawableVertical = divider;
    if (divider != null) {
      mDividerVerticalWidth = divider.getIntrinsicWidth();
    } else {
      mDividerVerticalWidth = 0;
    }
    setWillNotDrawFlag();
    requestLayout();
  }

  private void setWillNotDrawFlag() {
    if (mDividerDrawableHorizontal == null && mDividerDrawableVertical == null) {
      // onDraw is not called
      setWillNotDraw(true);
    } else {
      // onDraw is called
      setWillNotDraw(false);
    }
  }

  public static class LayoutParams extends ViewGroup.MarginLayoutParams {
    private static final int ORDER_DEFAULT = 1;

    public int order = ORDER_DEFAULT;

    public LayoutParams(Context c, AttributeSet attrs) {
      super(c, attrs);
    }

    public LayoutParams(int width, int height) {
      super(width, height);
    }

    public LayoutParams(MarginLayoutParams source) {
      super(source);
    }

    public LayoutParams(ViewGroup.LayoutParams source) {
      super(source);
    }
  }

  private static class Order implements Comparable<Order> {
    int index;
    int order;

    @Override
    public int compareTo(Order another) {
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
