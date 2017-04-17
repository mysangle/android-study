package com.twentyhours.androidstudy.recyclerview;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.Observable;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.os.TraceCompat;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.ScrollingView;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.twentyhours.androidstudy.R;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by soonhyung-imac on 4/4/17.
 */

public class RecyclerView extends ViewGroup implements ScrollingView, NestedScrollingChild {
  static final String TAG = "RecyclerView";
  static final boolean DEBUG = false;
  private static final int[]  NESTED_SCROLLING_ATTRS
      = {16843830 /* android.R.attr.nestedScrollingEnabled */};
  private static final Class<?>[] LAYOUT_MANAGER_CONSTRUCTOR_SIGNATURE =
      new Class[]{Context.class, AttributeSet.class, int.class, int.class};
  public static final int SCROLL_STATE_IDLE = 0;
  public static final int SCROLL_STATE_DRAGGING = 1;
  public static final int SCROLL_STATE_SETTLING = 2;

  private static final int[] CLIP_TO_PADDING_ATTR = {android.R.attr.clipToPadding};
  boolean mClipToPadding;
  private int mTouchSlop;
  private final int mMinFlingVelocity;
  private final int mMaxFlingVelocity;
  private ItemAnimator.ItemAnimatorListener mItemAnimatorListener =
      new ItemAnimatorRestoreListener();
  ItemAnimator mItemAnimator = new DefaultItemAnimator();
  AdapterHelper mAdapterHelper;
  ChildHelper mChildHelper;
  boolean mLayoutFrozen;
  boolean mLayoutRequestEaten;
  @VisibleForTesting
  LayoutManager mLayout;
  Adapter mAdapter;
  private boolean mIgnoreMotionEventTillDown;
  private int mDispatchScrollCounter = 0;
  private int mLayoutOrScrollCounter = 0;
  private final RecyclerViewDataObserver mObserver = new RecyclerViewDataObserver();
  final Recycler mRecycler = new Recycler();

  public RecyclerView(Context context) {
    this(context, null);
  }

  public RecyclerView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public RecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    if (attrs != null) {
      TypedArray a = context.obtainStyledAttributes(attrs, CLIP_TO_PADDING_ATTR, defStyle, 0);
      mClipToPadding = a.getBoolean(0, true);
      a.recycle();
    } else {
      mClipToPadding = true;
    }
    setScrollContainer(true);
    setFocusableInTouchMode(true);

    final ViewConfiguration vc = ViewConfiguration.get(context);
    mTouchSlop = vc.getScaledTouchSlop();
    mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
    mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
    setWillNotDraw(getOverScrollMode() == View.OVER_SCROLL_NEVER);

    mItemAnimator.setListener(mItemAnimatorListener);
    initAdapterManager();
    initChildrenHelper();
    // Create the layoutManager if specified.

    boolean nestedScrollingEnabled = true;

    if (attrs != null) {
      int defStyleRes = 0;
      TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RecyclerView,
          defStyle, defStyleRes);
      String layoutManagerName = a.getString(R.styleable.RecyclerView_layoutManager);
      int descendantFocusability = a.getInt(
          R.styleable.RecyclerView_android_descendantFocusability, -1);
      if (descendantFocusability == -1) {
        setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
      }
      a.recycle();
      createLayoutManager(context, layoutManagerName, attrs, defStyle, defStyleRes);

      if (Build.VERSION.SDK_INT >= 21) {
        a = context.obtainStyledAttributes(attrs, NESTED_SCROLLING_ATTRS,
            defStyle, defStyleRes);
        nestedScrollingEnabled = a.getBoolean(0, true);
        a.recycle();
      }
    } else {
      setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
    }

    // Re-set whether nested scrolling is enabled so that it is set on all API levels
    setNestedScrollingEnabled(nestedScrollingEnabled);
  }

  private void createLayoutManager(Context context, String className, AttributeSet attrs,
                                   int defStyleAttr, int defStyleRes) {
    if (className != null) {
      className = className.trim();
      if (className.length() != 0) {  // Can't use isEmpty since it was added in API 9.
        className = getFullClassName(context, className);
        try {
          ClassLoader classLoader;
          if (isInEditMode()) {
            // Stupid layoutlib cannot handle simple class loaders.
            classLoader = this.getClass().getClassLoader();
          } else {
            classLoader = context.getClassLoader();
          }
          Class<? extends LayoutManager> layoutManagerClass =
              classLoader.loadClass(className).asSubclass(LayoutManager.class);
          Constructor<? extends LayoutManager> constructor;
          Object[] constructorArgs = null;
          try {
            constructor = layoutManagerClass
                .getConstructor(LAYOUT_MANAGER_CONSTRUCTOR_SIGNATURE);
            constructorArgs = new Object[]{context, attrs, defStyleAttr, defStyleRes};
          } catch (NoSuchMethodException e) {
            try {
              constructor = layoutManagerClass.getConstructor();
            } catch (NoSuchMethodException e1) {
              e1.initCause(e);
              throw new IllegalStateException(attrs.getPositionDescription() +
                  ": Error creating LayoutManager " + className, e1);
            }
          }
          constructor.setAccessible(true);
          setLayoutManager(constructor.newInstance(constructorArgs));
        } catch (ClassNotFoundException e) {
          throw new IllegalStateException(attrs.getPositionDescription()
              + ": Unable to find LayoutManager " + className, e);
        } catch (InvocationTargetException e) {
          throw new IllegalStateException(attrs.getPositionDescription()
              + ": Could not instantiate the LayoutManager: " + className, e);
        } catch (InstantiationException e) {
          throw new IllegalStateException(attrs.getPositionDescription()
              + ": Could not instantiate the LayoutManager: " + className, e);
        } catch (IllegalAccessException e) {
          throw new IllegalStateException(attrs.getPositionDescription()
              + ": Cannot access non-public constructor " + className, e);
        } catch (ClassCastException e) {
          throw new IllegalStateException(attrs.getPositionDescription()
              + ": Class is not a LayoutManager " + className, e);
        }
      }
    }
  }

  private String getFullClassName(Context context, String className) {
    if (className.charAt(0) == '.') {
      return context.getPackageName() + className;
    }
    if (className.contains(".")) {
      return className;
    }
    return RecyclerView.class.getPackage().getName() + '.' + className;
  }

  public void setAdapter(RecyclerView.Adapter adapter) {
    // bail out if layout is frozen
    setLayoutFrozen(false);
    setAdapterInternal(adapter, false, true);
    requestLayout();
  }

  private void setAdapterInternal(Adapter adapter, boolean compatibleWithPrevious,
                                  boolean removeAndRecycleViews) {
//    if (mAdapter != null) {
//      mAdapter.unregisterAdapterDataObserver(mObserver);
//      mAdapter.onDetachedFromRecyclerView(this);
//    }
//    if (!compatibleWithPrevious || removeAndRecycleViews) {
//      removeAndRecycleViews();
//    }
//    mAdapterHelper.reset();
//    final Adapter oldAdapter = mAdapter;
//    mAdapter = adapter;
//    if (adapter != null) {
//      adapter.registerAdapterDataObserver(mObserver);
//      adapter.onAttachedToRecyclerView(this);
//    }
//    if (mLayout != null) {
//      mLayout.onAdapterChanged(oldAdapter, mAdapter);
//    }
//    mRecycler.onAdapterChanged(oldAdapter, mAdapter, compatibleWithPrevious);
//    mState.mStructureChanged = true;
//    markKnownViewsInvalid();
//  }
//
//  void removeAndRecycleViews() {
//    // end all running animations
//    if (mItemAnimator != null) {
//      mItemAnimator.endAnimations();
//    }
//    // Since animations are ended, mLayout.children should be equal to
//    // recyclerView.children. This may not be true if item animator's end does not work as
//    // expected. (e.g. not release children instantly). It is safer to use mLayout's child
//    // count.
//    if (mLayout != null) {
//      mLayout.removeAndRecycleAllViews(mRecycler);
//      mLayout.removeAndRecycleScrapInt(mRecycler);
//    }
//    // we should clear it here before adapters are swapped to ensure correct callbacks.
//    mRecycler.clear();
  }

  public void setLayoutFrozen(boolean frozen) {
    if (frozen != mLayoutFrozen) {
      assertNotInLayoutOrScroll("Do not setLayoutFrozen in layout or scroll");
      if (!frozen) {
        mLayoutFrozen = false;
        if (mLayoutRequestEaten && mLayout != null && mAdapter != null) {
          requestLayout();
        }
        mLayoutRequestEaten = false;
      } else {
        final long now = SystemClock.uptimeMillis();
        MotionEvent cancelEvent = MotionEvent.obtain(now, now,
            MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);
        onTouchEvent(cancelEvent);
        mLayoutFrozen = true;
        mIgnoreMotionEventTillDown = true;
        stopScroll();
      }
    }
  }

  void assertNotInLayoutOrScroll(String message) {
    if (isComputingLayout()) {
      if (message == null) {
        throw new IllegalStateException("Cannot call this method while RecyclerView is "
            + "computing a layout or scrolling");
      }
      throw new IllegalStateException(message);
    }
    if (mDispatchScrollCounter > 0) {
      Log.w(TAG, "Cannot call this method in a scroll callback. Scroll callbacks might be run"
              + " during a measure & layout pass where you cannot change the RecyclerView"
              + " data. Any method call that might change the structure of the RecyclerView"
              + " or the adapter contents should be postponed to the next frame.",
          new IllegalStateException(""));
    }
  }

  public boolean isComputingLayout() {
    return mLayoutOrScrollCounter > 0;
  }

  public void stopScroll() {
//    setScrollState(SCROLL_STATE_IDLE);
//    stopScrollersInternal();
  }

  public void setLayoutManager(LayoutManager layout) {
//    if (layout == mLayout) {
//      return;
//    }
//    stopScroll();
//    // TODO We should do this switch a dispatchLayout pass and animate children. There is a good
//    // chance that LayoutManagers will re-use views.
//    if (mLayout != null) {
//      // end all running animations
//      if (mItemAnimator != null) {
//        mItemAnimator.endAnimations();
//      }
//      mLayout.removeAndRecycleAllViews(mRecycler);
//      mLayout.removeAndRecycleScrapInt(mRecycler);
//      mRecycler.clear();
//
//      if (mIsAttached) {
//        mLayout.dispatchDetachedFromWindow(this, mRecycler);
//      }
//      mLayout.setRecyclerView(null);
//      mLayout = null;
//    } else {
//      mRecycler.clear();
//    }
//    // this is just a defensive measure for faulty item animators.
//    mChildHelper.removeAllViewsUnfiltered();
//    mLayout = layout;
//    if (layout != null) {
//      if (layout.mRecyclerView != null) {
//        throw new IllegalArgumentException("LayoutManager " + layout +
//            " is already attached to a RecyclerView: " + layout.mRecyclerView);
//      }
//      mLayout.setRecyclerView(this);
//      if (mIsAttached) {
//        mLayout.dispatchAttachedToWindow(this);
//      }
//    }
//    mRecycler.updateViewCacheSize();
//    requestLayout();
  }



  private void initChildrenHelper() {
//    mChildHelper = new ChildHelper(new ChildHelper.Callback() {
//      @Override
//      public int getChildCount() {
//        return RecyclerView.this.getChildCount();
//      }
//
//      @Override
//      public void addView(View child, int index) {
//        RecyclerView.this.addView(child, index);
//        dispatchChildAttached(child);
//      }
//
//      @Override
//      public int indexOfChild(View view) {
//        return RecyclerView.this.indexOfChild(view);
//      }
//
//      @Override
//      public void removeViewAt(int index) {
//        final View child = RecyclerView.this.getChildAt(index);
//        if (child != null) {
//          dispatchChildDetached(child);
//        }
//        RecyclerView.this.removeViewAt(index);
//      }
//
//      @Override
//      public View getChildAt(int offset) {
//        return RecyclerView.this.getChildAt(offset);
//      }
//
//      @Override
//      public void removeAllViews() {
//        final int count = getChildCount();
//        for (int i = 0; i < count; i ++) {
//          dispatchChildDetached(getChildAt(i));
//        }
//        RecyclerView.this.removeAllViews();
//      }
//
//      @Override
//      public ViewHolder getChildViewHolder(View view) {
//        return getChildViewHolderInt(view);
//      }
//
//      @Override
//      public void attachViewToParent(View child, int index,
//                                     ViewGroup.LayoutParams layoutParams) {
//        final ViewHolder vh = getChildViewHolderInt(child);
//        if (vh != null) {
//          if (!vh.isTmpDetached() && !vh.shouldIgnore()) {
//            throw new IllegalArgumentException("Called attach on a child which is not"
//                + " detached: " + vh);
//          }
//          if (DEBUG) {
//            Log.d(TAG, "reAttach " + vh);
//          }
//          vh.clearTmpDetachFlag();
//        }
//        RecyclerView.this.attachViewToParent(child, index, layoutParams);
//      }
//
//      @Override
//      public void detachViewFromParent(int offset) {
//        final View view = getChildAt(offset);
//        if (view != null) {
//          final ViewHolder vh = getChildViewHolderInt(view);
//          if (vh != null) {
//            if (vh.isTmpDetached() && !vh.shouldIgnore()) {
//              throw new IllegalArgumentException("called detach on an already"
//                  + " detached child " + vh);
//            }
//            if (DEBUG) {
//              Log.d(TAG, "tmpDetach " + vh);
//            }
//            vh.addFlags(ViewHolder.FLAG_TMP_DETACHED);
//          }
//        }
//        RecyclerView.this.detachViewFromParent(offset);
//      }
//
//      @Override
//      public void onEnteredHiddenState(View child) {
//        final ViewHolder vh = getChildViewHolderInt(child);
//        if (vh != null) {
//          vh.onEnteredHiddenState(RecyclerView.this);
//        }
//      }
//
//      @Override
//      public void onLeftHiddenState(View child) {
//        final ViewHolder vh = getChildViewHolderInt(child);
//        if (vh != null) {
//          vh.onLeftHiddenState(RecyclerView.this);
//        }
//      }
//    });
  }

  void initAdapterManager() {
//    mAdapterHelper = new AdapterHelper(new AdapterHelper.Callback() {
//      @Override
//      public ViewHolder findViewHolder(int position) {
//        final ViewHolder vh = findViewHolderForPosition(position, true);
//        if (vh == null) {
//          return null;
//        }
//        // ensure it is not hidden because for adapter helper, the only thing matter is that
//        // LM thinks view is a child.
//        if (mChildHelper.isHidden(vh.itemView)) {
//          if (DEBUG) {
//            Log.d(TAG, "assuming view holder cannot be find because it is hidden");
//          }
//          return null;
//        }
//        return vh;
//      }
//
//      @Override
//      public void offsetPositionsForRemovingInvisible(int start, int count) {
//        offsetPositionRecordsForRemove(start, count, true);
//        mItemsAddedOrRemoved = true;
//        mState.mDeletedInvisibleItemCountSincePreviousLayout += count;
//      }
//
//      @Override
//      public void offsetPositionsForRemovingLaidOutOrNewView(int positionStart, int itemCount) {
//        offsetPositionRecordsForRemove(positionStart, itemCount, false);
//        mItemsAddedOrRemoved = true;
//      }
//
//      @Override
//      public void markViewHoldersUpdated(int positionStart, int itemCount, Object payload) {
//        viewRangeUpdate(positionStart, itemCount, payload);
//        mItemsChanged = true;
//      }
//
//      @Override
//      public void onDispatchFirstPass(AdapterHelper.UpdateOp op) {
//        dispatchUpdate(op);
//      }
//
//      void dispatchUpdate(AdapterHelper.UpdateOp op) {
//        switch (op.cmd) {
//          case AdapterHelper.UpdateOp.ADD:
//            mLayout.onItemsAdded(RecyclerView.this, op.positionStart, op.itemCount);
//            break;
//          case AdapterHelper.UpdateOp.REMOVE:
//            mLayout.onItemsRemoved(RecyclerView.this, op.positionStart, op.itemCount);
//            break;
//          case AdapterHelper.UpdateOp.UPDATE:
//            mLayout.onItemsUpdated(RecyclerView.this, op.positionStart, op.itemCount,
//                op.payload);
//            break;
//          case AdapterHelper.UpdateOp.MOVE:
//            mLayout.onItemsMoved(RecyclerView.this, op.positionStart, op.itemCount, 1);
//            break;
//        }
//      }
//
//      @Override
//      public void onDispatchSecondPass(AdapterHelper.UpdateOp op) {
//        dispatchUpdate(op);
//      }
//
//      @Override
//      public void offsetPositionsForAdd(int positionStart, int itemCount) {
//        offsetPositionRecordsForInsert(positionStart, itemCount);
//        mItemsAddedOrRemoved = true;
//      }
//
//      @Override
//      public void offsetPositionsForMove(int from, int to) {
//        offsetPositionRecordsForMove(from, to);
//        // should we create mItemsMoved ?
//        mItemsAddedOrRemoved = true;
//      }
//    });
  }

  @Override
  protected void onMeasure(int widthSpec, int heightSpec) {
//    if (mLayout == null) {
//      defaultOnMeasure(widthSpec, heightSpec);
//      return;
//    }
//    if (mLayout.mAutoMeasure) {
//      final int widthMode = MeasureSpec.getMode(widthSpec);
//      final int heightMode = MeasureSpec.getMode(heightSpec);
//      final boolean skipMeasure = widthMode == MeasureSpec.EXACTLY
//          && heightMode == MeasureSpec.EXACTLY;
//      mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec);
//      if (skipMeasure || mAdapter == null) {
//        return;
//      }
//      if (mState.mLayoutStep == State.STEP_START) {
//        dispatchLayoutStep1();
//      }
//      // set dimensions in 2nd step. Pre-layout should happen with old dimensions for
//      // consistency
//      mLayout.setMeasureSpecs(widthSpec, heightSpec);
//      mState.mIsMeasuring = true;
//      dispatchLayoutStep2();
//
//      // now we can get the width and height from the children.
//      mLayout.setMeasuredDimensionFromChildren(widthSpec, heightSpec);
//
//      // if RecyclerView has non-exact width and height and if there is at least one child
//      // which also has non-exact width & height, we have to re-measure.
//      if (mLayout.shouldMeasureTwice()) {
//        mLayout.setMeasureSpecs(
//            MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
//            MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
//        mState.mIsMeasuring = true;
//        dispatchLayoutStep2();
//        // now we can get the width and height from the children.
//        mLayout.setMeasuredDimensionFromChildren(widthSpec, heightSpec);
//      }
//    } else {
//      if (mHasFixedSize) {
//        mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec);
//        return;
//      }
//      // custom onMeasure
//      if (mAdapterUpdateDuringMeasure) {
//        eatRequestLayout();
//        onEnterLayoutOrScroll();
//        processAdapterUpdatesAndSetAnimationFlags();
//        onExitLayoutOrScroll();
//
//        if (mState.mRunPredictiveAnimations) {
//          mState.mInPreLayout = true;
//        } else {
//          // consume remaining updates to provide a consistent state with the layout pass.
//          mAdapterHelper.consumeUpdatesInOnePass();
//          mState.mInPreLayout = false;
//        }
//        mAdapterUpdateDuringMeasure = false;
//        resumeRequestLayout(false);
//      }
//
//      if (mAdapter != null) {
//        mState.mItemCount = mAdapter.getItemCount();
//      } else {
//        mState.mItemCount = 0;
//      }
//      eatRequestLayout();
//      mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec);
//      resumeRequestLayout(false);
//      mState.mInPreLayout = false; // clear
//    }
  }

  void defaultOnMeasure(int widthSpec, int heightSpec) {
    // calling LayoutManager here is not pretty but that API is already public and it is better
    // than creating another method since this is internal.
    final int width = LayoutManager.chooseSize(widthSpec,
        getPaddingLeft() + getPaddingRight(),
        ViewCompat.getMinimumWidth(this));
    final int height = LayoutManager.chooseSize(heightSpec,
        getPaddingTop() + getPaddingBottom(),
        ViewCompat.getMinimumHeight(this));

    setMeasuredDimension(width, height);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
//    if (w != oldw || h != oldh) {
//      invalidateGlows();
//      // layout's w/h are updated during measure/layout steps.
//    }
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
//    TraceCompat.beginSection(TRACE_ON_LAYOUT_TAG);
//    dispatchLayout();
//    TraceCompat.endSection();
//    mFirstLayoutComplete = true;
  }

  void dispatchLayout() {
//    if (mAdapter == null) {
//      Log.e(TAG, "No adapter attached; skipping layout");
//      // leave the state in START
//      return;
//    }
//    if (mLayout == null) {
//      Log.e(TAG, "No layout manager attached; skipping layout");
//      // leave the state in START
//      return;
//    }
//    mState.mIsMeasuring = false;
//    if (mState.mLayoutStep == State.STEP_START) {
//      dispatchLayoutStep1();
//      mLayout.setExactMeasureSpecsFrom(this);
//      dispatchLayoutStep2();
//    } else if (mAdapterHelper.hasUpdates() || mLayout.getWidth() != getWidth() ||
//        mLayout.getHeight() != getHeight()) {
//      // First 2 steps are done in onMeasure but looks like we have to run again due to
//      // changed size.
//      mLayout.setExactMeasureSpecsFrom(this);
//      dispatchLayoutStep2();
//    } else {
//      // always make sure we sync them (to ensure mode is exact)
//      mLayout.setExactMeasureSpecsFrom(this);
//    }
//    dispatchLayoutStep3();
  }

  @Override
  public int computeHorizontalScrollRange() {
    return 0;
  }

  @Override
  public int computeHorizontalScrollOffset() {
    return 0;
  }

  @Override
  public int computeHorizontalScrollExtent() {
    return 0;
  }

  @Override
  public int computeVerticalScrollRange() {
    return 0;
  }

  @Override
  public int computeVerticalScrollOffset() {
    return 0;
  }

  @Override
  public int computeVerticalScrollExtent() {
    return 0;
  }

  public static abstract class Adapter<VH extends ViewHolder> {
    private final AdapterDataObservable mObservable = new AdapterDataObservable();

    public abstract VH onCreateViewHolder(ViewGroup parent, int viewType);

    public abstract void onBindViewHolder(VH holder, int position);

    public abstract int getItemCount();

    public int getItemViewType(int position) {
      return 0;
    }

    public final void notifyDataSetChanged() {
      mObservable.notifyChanged();
    }

    public void registerAdapterDataObserver(AdapterDataObserver observer) {
      mObservable.registerObserver(observer);
    }

    public void unregisterAdapterDataObserver(AdapterDataObserver observer) {
      mObservable.unregisterObserver(observer);
    }

    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
    }

    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
    }
  }

  public static abstract class ViewHolder {
    public final View itemView;

    public ViewHolder(View itemView) {
      if (itemView == null) {
        throw new IllegalArgumentException("itemView may not be null");
      }
      this.itemView = itemView;
    }
  }

  public static abstract class LayoutManager {
    RecyclerView mRecyclerView;
    private int mWidthMode, mHeightMode;
    private int mWidth, mHeight;
    ChildHelper mChildHelper;

    public static class Properties {

    }

    public void onAdapterChanged(Adapter oldAdapter, Adapter newAdapter) {
    }

    public static int chooseSize(int spec, int desired, int min) {
      final int mode = View.MeasureSpec.getMode(spec);
      final int size = View.MeasureSpec.getSize(spec);
      switch (mode) {
        case View.MeasureSpec.EXACTLY:
          return size;
        case View.MeasureSpec.AT_MOST:
          return Math.min(size, Math.max(desired, min));
        case View.MeasureSpec.UNSPECIFIED:
        default:
          return Math.max(desired, min);
      }
    }

    public void onMeasure(Recycler recycler, State state, int widthSpec, int heightSpec) {
      mRecyclerView.defaultOnMeasure(widthSpec, heightSpec);
    }

    void setRecyclerView(RecyclerView recyclerView) {
      if (recyclerView == null) {
        mRecyclerView = null;
        mChildHelper = null;
        mWidth = 0;
        mHeight = 0;
      } else {
        mRecyclerView = recyclerView;
        mChildHelper = recyclerView.mChildHelper;
        mWidth = recyclerView.getWidth();
        mHeight = recyclerView.getHeight();
      }
      mWidthMode = MeasureSpec.EXACTLY;
      mHeightMode = MeasureSpec.EXACTLY;
    }
  }

  public static abstract class SmoothScroller {
    public interface ScrollVectorProvider {

    }
  }

  static class AdapterDataObservable extends Observable<AdapterDataObserver> {
    public void notifyChanged() {
      // since onChanged() is implemented by the app, it could do anything, including
      // removing itself from {@link mObservers} - and that could cause problems if
      // an iterator is used on the ArrayList {@link mObservers}.
      // to avoid such problems, just march thru the list in the reverse order.
      for (int i = mObservers.size() - 1; i >= 0; i--) {
        mObservers.get(i).onChanged();
      }
    }
  }

  public static abstract class AdapterDataObserver {
    public void onChanged() {
      // Do nothing
    }
  }

  private class RecyclerViewDataObserver extends AdapterDataObserver {
    RecyclerViewDataObserver() {
    }

    @Override
    public void onChanged() {
//      assertNotInLayoutOrScroll(null);
//      mState.mStructureChanged = true;
//
//      setDataSetChangedAfterLayout();
//      if (!mAdapterHelper.hasPendingUpdates()) {
//        requestLayout();
//      }
    }
  }

  public static abstract class ItemAnimator {
    private ItemAnimatorListener mListener = null;

    interface ItemAnimatorListener {
      void onAnimationFinished(ViewHolder item);
    }

    void setListener(ItemAnimatorListener listener) {
      mListener = listener;
    }
  }

  private class ItemAnimatorRestoreListener implements ItemAnimator.ItemAnimatorListener {

    ItemAnimatorRestoreListener() {
    }

    @Override
    public void onAnimationFinished(ViewHolder item) {
//      item.setIsRecyclable(true);
//      if (item.mShadowedHolder != null && item.mShadowingHolder == null) { // old vh
//        item.mShadowedHolder = null;
//      }
//      // always null this because an OldViewHolder can never become NewViewHolder w/o being
//      // recycled.
//      item.mShadowingHolder = null;
//      if (!item.shouldBeKeptAsChild()) {
//        if (!removeAnimatingView(item.itemView) && item.isTmpDetached()) {
//          removeDetachedView(item.itemView, false);
//        }
//      }
    }
  }

  public final class Recycler {
    void onAdapterChanged(Adapter oldAdapter, Adapter newAdapter,
                          boolean compatibleWithPrevious) {
//      clear();
//      getRecycledViewPool().onAdapterChanged(oldAdapter, newAdapter, compatibleWithPrevious);
    }
  }

  public static class State {

  }
}
