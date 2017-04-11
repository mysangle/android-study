package com.twentyhours.androidstudy.viewpager;

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by soonhyung-imac on 3/28/17.
 */

public abstract class PagerAdapter {
  private final DataSetObservable mObservable = new DataSetObservable();
  private DataSetObserver mViewPagerObserver;

  public static final int POSITION_UNCHANGED = -1;
  public static final int POSITION_NONE = -2;

  public abstract int getCount();

  public abstract Object instantiateItem(ViewGroup container, int position);

  public abstract void destroyItem(ViewGroup container, int position, Object object);

  public abstract boolean isViewFromObject(View view, Object object);

  public void startUpdate(ViewGroup container) {

  }

  public void finishUpdate(ViewGroup container) {

  }

  public void restoreState(Parcelable state, ClassLoader loader) {
  }

  public float getPageWidth(int position) {
    return 1.f;
  }

  public void setPrimaryItem(ViewGroup container, int position, Object object) {

  }

  public int getItemPosition(Object object) {
    return POSITION_UNCHANGED;
  }

  void setViewPagerObserver(DataSetObserver observer) {
    synchronized (this) {
      mViewPagerObserver = observer;
    }
  }

  public void notifyDataSetChanged() {
    synchronized (this) {
      if (mViewPagerObserver != null) {
        mViewPagerObserver.onChanged();
      }
    }
    mObservable.notifyChanged();
  }

  public void registerDataSetObserver(DataSetObserver observer) {
    mObservable.registerObserver(observer);
  }

  public void unregisterDataSetObserver(DataSetObserver observer) {
    mObservable.unregisterObserver(observer);
  }
}
