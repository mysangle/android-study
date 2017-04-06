package com.twentyhours.androidstudy.recyclerview;

import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by soonhyung-imac on 4/5/17.
 */

public class ChildHelper {
  private static final boolean DEBUG = false;
  private static final String TAG = "ChildrenHelper";

  final Callback mCallback;
  final Bucket mBucket;
  final List<View> mHiddenViews;

  ChildHelper(Callback callback) {
    mCallback = callback;
    mBucket = new Bucket();
    mHiddenViews = new ArrayList<View>();
  }

  static class Bucket {

  }

  interface Callback {

    int getChildCount();

    void addView(View child, int index);

    int indexOfChild(View view);

    void removeViewAt(int index);

    View getChildAt(int offset);

    void removeAllViews();

    RecyclerView.ViewHolder getChildViewHolder(View view);

    void attachViewToParent(View child, int index, ViewGroup.LayoutParams layoutParams);

    void detachViewFromParent(int offset);

    void onEnteredHiddenState(View child);

    void onLeftHiddenState(View child);
  }
}
