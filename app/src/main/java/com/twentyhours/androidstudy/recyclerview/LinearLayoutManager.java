package com.twentyhours.androidstudy.recyclerview;

import android.content.Context;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by soonhyung-imac on 4/4/17.
 */

public class LinearLayoutManager extends RecyclerView.LayoutManager implements
    ItemTouchHelper.ViewDropHandler, RecyclerView.SmoothScroller.ScrollVectorProvider {
  public static final int HORIZONTAL = OrientationHelper.HORIZONTAL;

  public static final int VERTICAL = OrientationHelper.VERTICAL;

  public LinearLayoutManager(Context context) {
    this(context, VERTICAL, false);
  }

  public LinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
    setOrientation(orientation);
    setReverseLayout(reverseLayout);
    setAutoMeasureEnabled(true);
  }

  public LinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr,
                             int defStyleRes) {
    Properties properties = getProperties(context, attrs, defStyleAttr, defStyleRes);
    setOrientation(properties.orientation);
    setReverseLayout(properties.reverseLayout);
    setStackFromEnd(properties.stackFromEnd);
    setAutoMeasureEnabled(true);
  }

  @Override
  public void prepareForDrop(View view, View target, int x, int y) {

  }
}
