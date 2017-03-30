package com.twentyhours.androidstudy.viewpager;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.os.Parcelable;
import android.support.annotation.CallSuper;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Interpolator;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by soonhyung-imac on 3/28/17.
 */

public class ViewPager extends ViewGroup {
  PagerAdapter mAdapter;
  private Scroller mScroller;
  private boolean mIsScrollStarted;
  private static final int MAX_SETTLE_DURATION = 600; // ms
  private static final Comparator<ItemInfo> COMPARATOR = new Comparator<ItemInfo>(){
    @Override
    public int compare(ItemInfo lhs, ItemInfo rhs) {
      return lhs.position - rhs.position;
    }
  };
  private static final Interpolator sInterpolator = new Interpolator() {
    @Override
    public float getInterpolation(float t) {
      t -= 1.0f;
      return t * t * t * t * t + 1.0f;
    }
  };
  private int mTouchSlop;
  private int mMinimumVelocity;
  private int mMaximumVelocity;
  private EdgeEffectCompat mLeftEdge;
  private EdgeEffectCompat mRightEdge;
  private static final int MIN_FLING_VELOCITY = 400; // dips
  private int mFlingDistance;
  private static final int MIN_DISTANCE_FOR_FLING = 25; // dips
  private int mCloseEnough;
  private static final int CLOSE_ENOUGH = 2; // dp
  private int mDefaultGutterSize;
  private static final int DEFAULT_GUTTER_SIZE = 16; // dips

  public static final int SCROLL_STATE_IDLE = 0;
  public static final int SCROLL_STATE_DRAGGING = 1;
  public static final int SCROLL_STATE_SETTLING = 2;

  private final Runnable mEndScrollRunnable = new Runnable() {
    @Override
    public void run() {
      setScrollState(SCROLL_STATE_IDLE);
      populate();
    }
  };
  private int mScrollState = SCROLL_STATE_IDLE;

  static class ItemInfo {
    Object object;
    int position;
    boolean scrolling;
    float widthFactor;
    float offset;
  }

  private final ArrayList<ItemInfo> mItems = new ArrayList<>();
  int mCurItem;   // Index of currently displayed page.
  private int mRestoredCurItem = -1;
  private Parcelable mRestoredAdapterState = null;
  private ClassLoader mRestoredClassLoader = null;

  static final int[] LAYOUT_ATTRS = new int[] {
      android.R.attr.layout_gravity
  };

  private int mExpectedAdapterCount;
  private PagerObserver mObserver;
  private boolean mPopulatePending;
  private boolean mFirstLayout = true;
  private static final int DEFAULT_OFFSCREEN_PAGES = 1;
  private int mOffscreenPageLimit = DEFAULT_OFFSCREEN_PAGES;
  private static final int DRAW_ORDER_DEFAULT = 0;
  private static final int DRAW_ORDER_FORWARD = 1;
  private static final int DRAW_ORDER_REVERSE = 2;
  private int mDrawingOrder;
  private ArrayList<View> mDrawingOrderedChildren;
  private static final ViewPositionComparator sPositionComparator = new ViewPositionComparator();
  private boolean mInLayout;
  private int mPageMargin;
  private float mFirstOffset = -Float.MAX_VALUE;
  private float mLastOffset = Float.MAX_VALUE;
  private boolean mNeedCalculatePageOffsets = false;
  private android.support.v4.view.ViewPager.PageTransformer mPageTransformer;
  private int mPageTransformerLayerType;
  private boolean mCalledSuper;
  private final ItemInfo mTempItem = new ItemInfo();
  private int mChildWidthMeasureSpec;
  private int mChildHeightMeasureSpec;
  private int mGutterSize;
  private int mTopPageBounds;
  private int mBottomPageBounds;

  private boolean mFakeDragging;
  private VelocityTracker mVelocityTracker;
  private float mLastMotionX;
  private float mLastMotionY;
  private float mInitialMotionX;
  private float mInitialMotionY;
  private boolean mIsBeingDragged;
  private boolean mIsUnableToDrag;
  private int mActivePointerId = INVALID_POINTER;
  private static final int INVALID_POINTER = -1;


  private List<android.support.v4.view.ViewPager.OnPageChangeListener> mOnPageChangeListeners;
  private android.support.v4.view.ViewPager.OnPageChangeListener mInternalPageChangeListener;

  public ViewPager(Context context) {
    super(context);
    initViewPager();
  }

  public ViewPager(Context context, AttributeSet attrs) {
    super(context, attrs);
    initViewPager();
  }

  public ViewPager(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initViewPager();
  }

  public ViewPager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    initViewPager();
  }

  void initViewPager() {
    setWillNotDraw(false);
    setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
    setFocusable(true);
    final Context context = getContext();
    mScroller = new Scroller(context, sInterpolator);
    final ViewConfiguration configuration = ViewConfiguration.get(context);
    final float density = context.getResources().getDisplayMetrics().density;

    mTouchSlop = configuration.getScaledPagingTouchSlop();
    mMinimumVelocity = (int) (MIN_FLING_VELOCITY * density);
    mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    mLeftEdge = new EdgeEffectCompat(context);
    mRightEdge = new EdgeEffectCompat(context);

    mFlingDistance = (int) (MIN_DISTANCE_FOR_FLING * density);
    mCloseEnough = (int) (CLOSE_ENOUGH * density);
    mDefaultGutterSize = (int) (DEFAULT_GUTTER_SIZE * density);

    ViewCompat.setOnApplyWindowInsetsListener(this,
        new android.support.v4.view.OnApplyWindowInsetsListener() {
          private final Rect mTempRect = new Rect();

          @Override
          public WindowInsetsCompat onApplyWindowInsets(final View v,
                                                        final WindowInsetsCompat originalInsets) {
            // First let the ViewPager itself try and consume them...
            final WindowInsetsCompat applied =
                ViewCompat.onApplyWindowInsets(v, originalInsets);
            if (applied.isConsumed()) {
              // If the ViewPager consumed all insets, return now
              return applied;
            }

            // Now we'll manually dispatch the insets to our children. Since ViewPager
            // children are always full-height, we do not want to use the standard
            // ViewGroup dispatchApplyWindowInsets since if child 0 consumes them,
            // the rest of the children will not receive any insets. To workaround this
            // we manually dispatch the applied insets, not allowing children to
            // consume them from each other. We do however keep track of any insets
            // which are consumed, returning the union of our children's consumption
            final Rect res = mTempRect;
            res.left = applied.getSystemWindowInsetLeft();
            res.top = applied.getSystemWindowInsetTop();
            res.right = applied.getSystemWindowInsetRight();
            res.bottom = applied.getSystemWindowInsetBottom();

            for (int i = 0, count = getChildCount(); i < count; i++) {
              final WindowInsetsCompat childInsets = ViewCompat
                  .dispatchApplyWindowInsets(getChildAt(i), applied);
              // Now keep track of any consumed by tracking each dimension's min
              // value
              res.left = Math.min(childInsets.getSystemWindowInsetLeft(),
                  res.left);
              res.top = Math.min(childInsets.getSystemWindowInsetTop(),
                  res.top);
              res.right = Math.min(childInsets.getSystemWindowInsetRight(),
                  res.right);
              res.bottom = Math.min(childInsets.getSystemWindowInsetBottom(),
                  res.bottom);
            }

            // Now return a new WindowInsets, using the consumed window insets
            return applied.replaceSystemWindowInsets(
                res.left, res.top, res.right, res.bottom);
          }
        });
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    mFirstLayout = true;
  }

  @Override
  protected void onDetachedFromWindow() {
    removeCallbacks(mEndScrollRunnable);
    // To be on the safe side, abort the scroller
    if ((mScroller != null) && !mScroller.isFinished()) {
      mScroller.abortAnimation();
    }
    super.onDetachedFromWindow();
  }

  public void setAdapter(PagerAdapter adapter) {
    if (mAdapter != null) {
      mAdapter.setViewPagerObserver(null);
      mAdapter.startUpdate(this);
      for (int i = 0; i < mItems.size(); i++) {
        final ItemInfo ii = mItems.get(i);
        mAdapter.destroyItem(this, ii.position, ii.object);
      }
      mAdapter.finishUpdate(this);
      mItems.clear();
      removeNonDecorViews();
      mCurItem = 0;
      scrollTo(0, 0);
    }

    mAdapter = adapter;
    mExpectedAdapterCount = 0;

    if (mAdapter != null) {
      if (mObserver == null) {
        mObserver = new PagerObserver();
      }
      mAdapter.setViewPagerObserver(mObserver);
      mPopulatePending = false;
      final boolean wasFirstLayout = mFirstLayout;
      mFirstLayout = true;
      mExpectedAdapterCount = mAdapter.getCount();
      if (mRestoredCurItem >= 0) {
        mAdapter.restoreState(mRestoredAdapterState, mRestoredClassLoader);
        setCurrentItemInternal(mRestoredCurItem, false, true);
        mRestoredCurItem = -1;
        mRestoredAdapterState = null;
        mRestoredClassLoader = null;
      } else if (!wasFirstLayout) {
        populate();
      } else {
        requestLayout();
      }
    }
  }

  public void setCurrentItem(int item) {
    mPopulatePending = false;
    setCurrentItemInternal(item, !mFirstLayout, false);
  }

  public void setCurrentItem(int item, boolean smoothScroll) {
    mPopulatePending = false;
    setCurrentItemInternal(item, smoothScroll, false);
  }

  public int getCurrentItem() {
    return mCurItem;
  }

  void setCurrentItemInternal(int item, boolean smoothScroll, boolean always) {
    setCurrentItemInternal(item, smoothScroll, always, 0);
  }

  void setCurrentItemInternal(int item, boolean smoothScroll, boolean always, int velocity) {
    if (mAdapter == null || mAdapter.getCount() <= 0) {
      return;
    }
    if (!always && mCurItem == item && mItems.size() != 0) {
      return;
    }

    if (item < 0) {
      item = 0;
    } else if (item >= mAdapter.getCount()) {
      item = mAdapter.getCount() - 1;
    }
    final int pageLimit = mOffscreenPageLimit;
    if (item > (mCurItem + pageLimit) || item < (mCurItem - pageLimit)) {
      // We are doing a jump by more than one page.  To avoid
      // glitches, we want to keep all current pages in the view
      // until the scroll ends.
      for (int i = 0; i < mItems.size(); i++) {
        mItems.get(i).scrolling = true;
      }
    }
    final boolean dispatchSelected = mCurItem != item;

    if (mFirstLayout) {
      // We don't have any idea how big we are yet and shouldn't have any pages either.
      // Just set things up and let the pending layout handle things.
      mCurItem = item;
      if (dispatchSelected) {
        dispatchOnPageSelected(item);
      }
      requestLayout();
    } else {
      populate(item);
      scrollToItem(item, smoothScroll, velocity, dispatchSelected);
    }
  }

  private void scrollToItem(int item, boolean smoothScroll, int velocity,
                            boolean dispatchSelected) {
    final ItemInfo curInfo = infoForPosition(item);
    int destX = 0;
    if (curInfo != null) {
      final int width = getClientWidth();
      destX = (int) (width * Math.max(mFirstOffset,
          Math.min(curInfo.offset, mLastOffset)));
    }
    if (smoothScroll) {
      smoothScrollTo(destX, 0, velocity);
      if (dispatchSelected) {
        dispatchOnPageSelected(item);
      }
    } else {
      if (dispatchSelected) {
        dispatchOnPageSelected(item);
      }
      completeScroll(false);
      scrollTo(destX, 0);
      pageScrolled(destX);
    }
  }

  private void dispatchOnPageScrolled(int position, float offset, int offsetPixels) {
    if (mOnPageChangeListeners != null) {
      for (int i = 0, z = mOnPageChangeListeners.size(); i < z; i++) {
        android.support.v4.view.ViewPager.OnPageChangeListener listener = mOnPageChangeListeners.get(i);
        if (listener != null) {
          listener.onPageScrolled(position, offset, offsetPixels);
        }
      }
    }
    if (mInternalPageChangeListener != null) {
      mInternalPageChangeListener.onPageScrolled(position, offset, offsetPixels);
    }
  }

  private void dispatchOnPageSelected(int position) {
    if (mOnPageChangeListeners != null) {
      for (int i = 0, z = mOnPageChangeListeners.size(); i < z; i++) {
        android.support.v4.view.ViewPager.OnPageChangeListener listener = mOnPageChangeListeners.get(i);
        if (listener != null) {
          listener.onPageSelected(position);
        }
      }
    }
    if (mInternalPageChangeListener != null) {
      mInternalPageChangeListener.onPageSelected(position);
    }
  }

  private void dispatchOnScrollStateChanged(int state) {
    if (mOnPageChangeListeners != null) {
      for (int i = 0, z = mOnPageChangeListeners.size(); i < z; i++) {
        android.support.v4.view.ViewPager.OnPageChangeListener listener = mOnPageChangeListeners.get(i);
        if (listener != null) {
          listener.onPageScrollStateChanged(state);
        }
      }
    }
    if (mInternalPageChangeListener != null) {
      mInternalPageChangeListener.onPageScrollStateChanged(state);
    }
  }

  public void addOnPageChangeListener(android.support.v4.view.ViewPager.OnPageChangeListener listener) {
    if (mOnPageChangeListeners == null) {
      mOnPageChangeListeners = new ArrayList<>();
    }
    mOnPageChangeListeners.add(listener);
  }

  public void removeOnPageChangeListener(android.support.v4.view.ViewPager.OnPageChangeListener listener) {
    if (mOnPageChangeListeners != null) {
      mOnPageChangeListeners.remove(listener);
    }
  }

  public void clearOnPageChangeListeners() {
    if (mOnPageChangeListeners != null) {
      mOnPageChangeListeners.clear();
    }
  }

  float distanceInfluenceForSnapDuration(float f) {
    f -= 0.5f; // center the values about 0.
    f *= 0.3f * Math.PI / 2.0f;
    return (float) Math.sin(f);
  }

  @CallSuper
  protected void onPageScrolled(int position, float offset, int offsetPixels) {
    dispatchOnPageScrolled(position, offset, offsetPixels);

    if (mPageTransformer != null) {
      final int scrollX = getScrollX();
      final int childCount = getChildCount();
      for (int i = 0; i < childCount; i++) {
        final View child = getChildAt(i);
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();

        final float transformPos = (float) (child.getLeft() - scrollX) / getClientWidth();
        mPageTransformer.transformPage(child, transformPos);
      }
    }

    mCalledSuper = true;
  }

  void smoothScrollTo(int x, int y, int velocity) {
    if (getChildCount() == 0) {
      return;
    }

    int sx;
    boolean wasScrolling = (mScroller != null) && !mScroller.isFinished();
    if (wasScrolling) {
      // We're in the middle of a previously initiated scrolling. Check to see
      // whether that scrolling has actually started (if we always call getStartX
      // we can get a stale value from the scroller if it hadn't yet had its first
      // computeScrollOffset call) to decide what is the current scrolling position.
      sx = mIsScrollStarted ? mScroller.getCurrX() : mScroller.getStartX();
      // And abort the current scrolling.
      mScroller.abortAnimation();
    } else {
      sx = getScrollX();
    }
    int sy = getScrollY();
    int dx = x - sx;
    int dy = y - sy;
    if (dx == 0 && dy == 0) {
      completeScroll(false);
      populate();
      setScrollState(SCROLL_STATE_IDLE);
      return;
    }

    setScrollState(SCROLL_STATE_SETTLING);

    final int width = getClientWidth();
    final int halfWidth = width / 2;
    final float distanceRatio = Math.min(1f, 1.0f * Math.abs(dx) / width);
    final float distance = halfWidth + halfWidth
        * distanceInfluenceForSnapDuration(distanceRatio);

    int duration;
    velocity = Math.abs(velocity);
    if (velocity > 0) {
      duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
    } else {
      final float pageWidth = width * mAdapter.getPageWidth(mCurItem);
      final float pageDelta = (float) Math.abs(dx) / (pageWidth + mPageMargin);
      duration = (int) ((pageDelta + 1) * 100);
    }
    duration = Math.min(duration, MAX_SETTLE_DURATION);

    // Reset the "scroll started" flag. It will be flipped to true in all places
    // where we call computeScrollOffset().
    mIsScrollStarted = false;
    mScroller.startScroll(sx, sy, dx, dy, duration);
    ViewCompat.postInvalidateOnAnimation(this);
  }

  private void completeScroll(boolean postEvents) {
    boolean needPopulate = mScrollState == SCROLL_STATE_SETTLING;
    if (needPopulate) {
      boolean wasScrolling = !mScroller.isFinished();
      if (wasScrolling) {
        mScroller.abortAnimation();
        int oldX = getScrollX();
        int oldY = getScrollY();
        int x = mScroller.getCurrX();
        int y = mScroller.getCurrY();
        if (oldX != x || oldY != y) {
          scrollTo(x, y);
          if (x != oldX) {
            pageScrolled(x);
          }
        }
      }
    }
    mPopulatePending = false;
    for (int i = 0; i < mItems.size(); i++) {
      ItemInfo ii = mItems.get(i);
      if (ii.scrolling) {
        needPopulate = true;
        ii.scrolling = false;
      }
    }
    if (needPopulate) {
      if (postEvents) {
        ViewCompat.postOnAnimation(this, mEndScrollRunnable);
      } else {
        mEndScrollRunnable.run();
      }
    }
  }

  private boolean pageScrolled(int xpos) {
    if (mItems.size() == 0) {
      if (mFirstLayout) {
        // If we haven't been laid out yet, we probably just haven't been populated yet.
        // Let's skip this call since it doesn't make sense in this state
        return false;
      }
      mCalledSuper = false;
      onPageScrolled(0, 0, 0);
      if (!mCalledSuper) {
        throw new IllegalStateException(
            "onPageScrolled did not call superclass implementation");
      }
      return false;
    }
    final ItemInfo ii = infoForCurrentScrollPosition();
    final int width = getClientWidth();
    final int widthWithMargin = width + mPageMargin;
    final float marginOffset = (float) mPageMargin / width;
    final int currentPage = ii.position;
    final float pageOffset = (((float) xpos / width) - ii.offset)
        / (ii.widthFactor + marginOffset);
    final int offsetPixels = (int) (pageOffset * widthWithMargin);

    mCalledSuper = false;
    onPageScrolled(currentPage, pageOffset, offsetPixels);
    if (!mCalledSuper) {
      throw new IllegalStateException(
          "onPageScrolled did not call superclass implementation");
    }
    return true;
  }

  private ItemInfo infoForCurrentScrollPosition() {
    final int width = getClientWidth();
    final float scrollOffset = width > 0 ? (float) getScrollX() / width : 0;
    final float marginOffset = width > 0 ? (float) mPageMargin / width : 0;
    int lastPos = -1;
    float lastOffset = 0.f;
    float lastWidth = 0.f;
    boolean first = true;

    ItemInfo lastItem = null;
    for (int i = 0; i < mItems.size(); i++) {
      ItemInfo ii = mItems.get(i);
      float offset;
      if (!first && ii.position != lastPos + 1) {
        // Create a synthetic item for a missing page.
        ii = mTempItem;
        ii.offset = lastOffset + lastWidth + marginOffset;
        ii.position = lastPos + 1;
        ii.widthFactor = mAdapter.getPageWidth(ii.position);
        i--;
      }
      offset = ii.offset;

      final float leftBound = offset;
      final float rightBound = offset + ii.widthFactor + marginOffset;
      if (first || scrollOffset >= leftBound) {
        if (scrollOffset < rightBound || i == mItems.size() - 1) {
          return ii;
        }
      } else {
        return lastItem;
      }
      first = false;
      lastPos = ii.position;
      lastOffset = offset;
      lastWidth = ii.widthFactor;
      lastItem = ii;
    }

    return lastItem;
  }

  void setScrollState(int newState) {
    if (mScrollState == newState) {
      return;
    }

    mScrollState = newState;
    if (mPageTransformer != null) {
      // PageTransformers can do complex things that benefit from hardware layers.
      enableLayers(newState != SCROLL_STATE_IDLE);
    }
    dispatchOnScrollStateChanged(newState);
  }

  private void enableLayers(boolean enable) {
    final int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      final int layerType = enable
          ? mPageTransformerLayerType : ViewCompat.LAYER_TYPE_NONE;
      ViewCompat.setLayerType(getChildAt(i), layerType, null);
    }
  }

  private void removeNonDecorViews() {
    for (int i = 0; i < getChildCount(); i++) {
      final View child = getChildAt(i);
      final LayoutParams lp = (LayoutParams) child.getLayoutParams();
      removeViewAt(i);
      i--;
    }
  }

  void dataSetChanged() {
    // This method only gets called if our observer is attached, so mAdapter is non-null.

    final int adapterCount = mAdapter.getCount();
    mExpectedAdapterCount = adapterCount;
    boolean needPopulate = mItems.size() < mOffscreenPageLimit * 2 + 1
        && mItems.size() < adapterCount;
    int newCurrItem = mCurItem;

    boolean isUpdating = false;
    for (int i = 0; i < mItems.size(); i++) {
      final ItemInfo ii = mItems.get(i);
      final int newPos = mAdapter.getItemPosition(ii.object);

      if (newPos == PagerAdapter.POSITION_UNCHANGED) {
        continue;
      }

      if (newPos == PagerAdapter.POSITION_NONE) {
        mItems.remove(i);
        i--;

        if (!isUpdating) {
          mAdapter.startUpdate(this);
          isUpdating = true;
        }

        mAdapter.destroyItem(this, ii.position, ii.object);
        needPopulate = true;

        if (mCurItem == ii.position) {
          // Keep the current item in the valid range
          newCurrItem = Math.max(0, Math.min(mCurItem, adapterCount - 1));
          needPopulate = true;
        }
        continue;
      }

      if (ii.position != newPos) {
        if (ii.position == mCurItem) {
          // Our current item changed position. Follow it.
          newCurrItem = newPos;
        }

        ii.position = newPos;
        needPopulate = true;
      }
    }

    if (isUpdating) {
      mAdapter.finishUpdate(this);
    }

    Collections.sort(mItems, COMPARATOR);

    if (needPopulate) {
      // Reset our known page widths; populate will recompute them.
      final int childCount = getChildCount();
      for (int i = 0; i < childCount; i++) {
        final View child = getChildAt(i);
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        lp.widthFactor = 0.f;
      }

      setCurrentItemInternal(newCurrItem, false, true);
      requestLayout();
    }
  }

  void populate() {
    populate(mCurItem);
  }

  void populate(int newCurrentItem) {
    ItemInfo oldCurInfo = null;
    if (mCurItem != newCurrentItem) {
      oldCurInfo = infoForPosition(mCurItem);
      mCurItem = newCurrentItem;
    }

    if (mAdapter == null) {
      sortChildDrawingOrder();
      return;
    }

    if (mPopulatePending) {
      sortChildDrawingOrder();
      return;
    }

    if (getWindowToken() == null) {
      return;
    }

    mAdapter.startUpdate(this);

    final int pageLimit = mOffscreenPageLimit;
    final int startPos = Math.max(0, mCurItem - pageLimit);
    final int N = mAdapter.getCount();
    final int endPos = Math.min(N - 1, mCurItem + pageLimit);

    if (N != mExpectedAdapterCount) {
      String resName;
      try {
        resName = getResources().getResourceName(getId());
      } catch (Resources.NotFoundException e) {
        resName = Integer.toHexString(getId());
      }
      throw new IllegalStateException("The application's PagerAdapter changed the adapter's"
          + " contents without calling PagerAdapter#notifyDataSetChanged!"
          + " Expected adapter item count: " + mExpectedAdapterCount + ", found: " + N
          + " Pager id: " + resName
          + " Pager class: " + getClass()
          + " Problematic adapter: " + mAdapter.getClass());
    }

    // Locate the currently focused item or add it if needed.
    int curIndex = -1;
    ItemInfo curItem = null;
    for (curIndex = 0; curIndex < mItems.size(); curIndex++) {
      final ItemInfo ii = mItems.get(curIndex);
      if (ii.position >= mCurItem) {
        if (ii.position == mCurItem) {
          curItem = ii;
        }
        break;
      }
    }

    if (curItem == null && N > 0) {
      curItem = addNewItem(mCurItem, curIndex);
    }

    if (curItem != null) {
      float extraWidthLeft = 0.f;
      int itemIndex = curIndex - 1;
      ItemInfo ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
      final int clientWidth = getClientWidth();
      final float leftWidthNeeded = clientWidth <= 0 ? 0 :
          2.f - curItem.widthFactor + (float) getPaddingLeft() / (float) clientWidth;
      for (int pos = mCurItem - 1; pos >= 0; pos--) {
        if (extraWidthLeft >= leftWidthNeeded && pos < startPos) {
          if (ii == null) {
            break;
          }
          if (pos == ii.position && !ii.scrolling) {
            mItems.remove(itemIndex);
            mAdapter.destroyItem(this, pos, ii.object);
            itemIndex--;
            curIndex--;
            ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
          }
        } else if (ii != null && pos == ii.position) {
          extraWidthLeft += ii.widthFactor;
          itemIndex--;
          ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
        } else {
          ii = addNewItem(pos, itemIndex + 1);
          extraWidthLeft += ii.widthFactor;
          curIndex++;
          ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
        }
      }

      float extraWidthRight = curItem.widthFactor;
      itemIndex = curIndex + 1;
      if (extraWidthRight < 2.f) {
        ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
        final float rightWidthNeeded = clientWidth <= 0 ? 0 :
            (float) getPaddingRight() / (float) clientWidth + 2.f;
        for (int pos = mCurItem + 1; pos < N; pos++) {
          if (extraWidthRight >= rightWidthNeeded && pos > endPos) {
            if (ii == null) {
              break;
            }
            if (pos == ii.position && !ii.scrolling) {
              mItems.remove(itemIndex);
              mAdapter.destroyItem(this, pos, ii.object);
              ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
            }
          } else if (ii != null && pos == ii.position) {
            extraWidthRight += ii.widthFactor;
            itemIndex++;
            ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
          } else {
            ii = addNewItem(pos, itemIndex);
            itemIndex++;
            extraWidthRight += ii.widthFactor;
            ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
          }
        }
      }

      calculatePageOffsets(curItem, curIndex, oldCurInfo);
    }

    mAdapter.setPrimaryItem(this, mCurItem, curItem != null ? curItem.object : null);

    mAdapter.finishUpdate(this);

    final int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      final View child = getChildAt(i);
      final LayoutParams lp = (LayoutParams) child.getLayoutParams();
      lp.childIndex = i;
      if (lp.widthFactor == 0.f) {
        // 0 means requery the adapter for this, it doesn't have a valid width.
        final ItemInfo ii = infoForChild(child);
        if (ii != null) {
          lp.widthFactor = ii.widthFactor;
          lp.position = ii.position;
        }
      }
    }
    sortChildDrawingOrder();

    if (hasFocus()) {
      View currentFocused = findFocus();
      ItemInfo ii = currentFocused != null ? infoForAnyChild(currentFocused) : null;
      if (ii == null || ii.position != mCurItem) {
        for (int i = 0; i < getChildCount(); i++) {
          View child = getChildAt(i);
          ii = infoForChild(child);
          if (ii != null && ii.position == mCurItem) {
            if (child.requestFocus(View.FOCUS_FORWARD)) {
              break;
            }
          }
        }
      }
    }
  }

  private void calculatePageOffsets(ItemInfo curItem, int curIndex, ItemInfo oldCurInfo) {
    final int N = mAdapter.getCount();
    final int width = getClientWidth();
    final float marginOffset = width > 0 ? (float) mPageMargin / width : 0;
    // Fix up offsets for later layout.
    if (oldCurInfo != null) {
      final int oldCurPosition = oldCurInfo.position;
      // Base offsets off of oldCurInfo.
      if (oldCurPosition < curItem.position) {
        int itemIndex = 0;
        ItemInfo ii = null;
        float offset = oldCurInfo.offset + oldCurInfo.widthFactor + marginOffset;
        for (int pos = oldCurPosition + 1;
             pos <= curItem.position && itemIndex < mItems.size(); pos++) {
          ii = mItems.get(itemIndex);
          while (pos > ii.position && itemIndex < mItems.size() - 1) {
            itemIndex++;
            ii = mItems.get(itemIndex);
          }
          while (pos < ii.position) {
            // We don't have an item populated for this,
            // ask the adapter for an offset.
            offset += mAdapter.getPageWidth(pos) + marginOffset;
            pos++;
          }
          ii.offset = offset;
          offset += ii.widthFactor + marginOffset;
        }
      } else if (oldCurPosition > curItem.position) {
        int itemIndex = mItems.size() - 1;
        ItemInfo ii = null;
        float offset = oldCurInfo.offset;
        for (int pos = oldCurPosition - 1;
             pos >= curItem.position && itemIndex >= 0; pos--) {
          ii = mItems.get(itemIndex);
          while (pos < ii.position && itemIndex > 0) {
            itemIndex--;
            ii = mItems.get(itemIndex);
          }
          while (pos > ii.position) {
            // We don't have an item populated for this,
            // ask the adapter for an offset.
            offset -= mAdapter.getPageWidth(pos) + marginOffset;
            pos--;
          }
          offset -= ii.widthFactor + marginOffset;
          ii.offset = offset;
        }
      }
    }

    // Base all offsets off of curItem.
    final int itemCount = mItems.size();
    float offset = curItem.offset;
    int pos = curItem.position - 1;
    mFirstOffset = curItem.position == 0 ? curItem.offset : -Float.MAX_VALUE;
    mLastOffset = curItem.position == N - 1
        ? curItem.offset + curItem.widthFactor - 1 : Float.MAX_VALUE;
    // Previous pages
    for (int i = curIndex - 1; i >= 0; i--, pos--) {
      final ItemInfo ii = mItems.get(i);
      while (pos > ii.position) {
        offset -= mAdapter.getPageWidth(pos--) + marginOffset;
      }
      offset -= ii.widthFactor + marginOffset;
      ii.offset = offset;
      if (ii.position == 0) mFirstOffset = offset;
    }
    offset = curItem.offset + curItem.widthFactor + marginOffset;
    pos = curItem.position + 1;
    // Next pages
    for (int i = curIndex + 1; i < itemCount; i++, pos++) {
      final ItemInfo ii = mItems.get(i);
      while (pos < ii.position) {
        offset += mAdapter.getPageWidth(pos++) + marginOffset;
      }
      if (ii.position == N - 1) {
        mLastOffset = offset + ii.widthFactor - 1;
      }
      ii.offset = offset;
      offset += ii.widthFactor + marginOffset;
    }

    mNeedCalculatePageOffsets = false;
  }

  private void sortChildDrawingOrder() {
    if (mDrawingOrder != DRAW_ORDER_DEFAULT) {
      if (mDrawingOrderedChildren == null) {
        mDrawingOrderedChildren = new ArrayList<>();
      } else {
        mDrawingOrderedChildren.clear();
      }
      final int childCount = getChildCount();
      for (int i = 0; i < childCount; i++) {
        final View child = getChildAt(i);
        mDrawingOrderedChildren.add(child);
      }
      Collections.sort(mDrawingOrderedChildren, sPositionComparator);
    }
  }

  ItemInfo infoForAnyChild(View child) {
    ViewParent parent;
    while ((parent = child.getParent()) != this) {
      if (parent == null || !(parent instanceof View)) {
        return null;
      }
      child = (View) parent;
    }
    return infoForChild(child);
  }

  ItemInfo infoForChild(View child) {
    for (int i = 0; i < mItems.size(); i++) {
      ItemInfo ii = mItems.get(i);
      if (mAdapter.isViewFromObject(child, ii.object)) {
        return ii;
      }
    }
    return null;
  }

  ItemInfo infoForPosition(int position) {
    for (int i = 0; i < mItems.size(); i++) {
      ItemInfo ii = mItems.get(i);
      if (ii.position == position) {
        return ii;
      }
    }
    return null;
  }

  ItemInfo addNewItem(int position, int index) {
    ItemInfo ii = new ItemInfo();
    ii.position = position;
    ii.object = mAdapter.instantiateItem(this, position);
    ii.widthFactor = mAdapter.getPageWidth(position);
    if (index < 0 || index >= mItems.size()) {
      mItems.add(ii);
    } else {
      mItems.add(index, ii);
    }
    return ii;
  }

  private int getClientWidth() {
    return getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
  }

  @Override
  public void addView(View child, int index, ViewGroup.LayoutParams params) {
    if (!checkLayoutParams(params)) {
      params = generateLayoutParams(params);
    }
    final LayoutParams lp = (LayoutParams) params;
    if (mInLayout) {
      lp.needsMeasure = true;
      addViewInLayout(child, index, params);
    } else {
      super.addView(child, index, params);
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    // For simple implementation, our internal size is always 0.
    // We depend on the container to specify the layout size of
    // our view.  We can't really know what it is since we will be
    // adding and removing different arbitrary views and do not
    // want the layout to change as this happens.
    setMeasuredDimension(getDefaultSize(0, widthMeasureSpec),
        getDefaultSize(0, heightMeasureSpec));

    final int measuredWidth = getMeasuredWidth();
    final int maxGutterSize = measuredWidth / 10;
    mGutterSize = Math.min(maxGutterSize, mDefaultGutterSize);

    // Children are just made to fill our space.
    int childWidthSize = measuredWidth - getPaddingLeft() - getPaddingRight();
    int childHeightSize = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();

    mChildWidthMeasureSpec = MeasureSpec.makeMeasureSpec(childWidthSize, MeasureSpec.EXACTLY);
    mChildHeightMeasureSpec = MeasureSpec.makeMeasureSpec(childHeightSize, MeasureSpec.EXACTLY);

    // Make sure we have created all fragments that we need to have shown.
    mInLayout = true;
    populate();
    mInLayout = false;

    // Page views next.
    int size = getChildCount();
    for (int i = 0; i < size; ++i) {
      final View child = getChildAt(i);
      if (child.getVisibility() != GONE) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        final int widthSpec = MeasureSpec.makeMeasureSpec(
            (int) (childWidthSize * lp.widthFactor), MeasureSpec.EXACTLY);
        child.measure(widthSpec, mChildHeightMeasureSpec);
      }
    }
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    final int count = getChildCount();
    int width = r - l;
    int height = b - t;
    int paddingLeft = getPaddingLeft();
    int paddingTop = getPaddingTop();
    int paddingRight = getPaddingRight();
    int paddingBottom = getPaddingBottom();
    final int scrollX = getScrollX();

    final int childWidth = width - paddingLeft - paddingRight;
    // Page views. Do this once we have the right padding offsets from above.
    for (int i = 0; i < count; i++) {
      final View child = getChildAt(i);
      if (child.getVisibility() != GONE) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        ItemInfo ii;
        if ((ii = infoForChild(child)) != null) {
          int loff = (int) (childWidth * ii.offset);
          int childLeft = paddingLeft + loff;
          int childTop = paddingTop;
          if (lp.needsMeasure) {
            // This was added during layout and needs measurement.
            // Do it now that we know what we're working with.
            lp.needsMeasure = false;
            final int widthSpec = MeasureSpec.makeMeasureSpec(
                (int) (childWidth * lp.widthFactor),
                MeasureSpec.EXACTLY);
            final int heightSpec = MeasureSpec.makeMeasureSpec(
                (int) (height - paddingTop - paddingBottom),
                MeasureSpec.EXACTLY);
            child.measure(widthSpec, heightSpec);
          }
          child.layout(childLeft, childTop,
              childLeft + child.getMeasuredWidth(),
              childTop + child.getMeasuredHeight());
        }
      }
    }
    mTopPageBounds = paddingTop;
    mBottomPageBounds = height - paddingBottom;

    if (mFirstLayout) {
      scrollToItem(mCurItem, false, 0, false);
    }
    mFirstLayout = false;
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    // Make sure scroll position is set correctly.
    if (w != oldw) {
      recomputeScrollPosition(w, oldw, mPageMargin, mPageMargin);
    }
  }

  @Override
  public void computeScroll() {
    mIsScrollStarted = true;
    if (!mScroller.isFinished() && mScroller.computeScrollOffset()) {
      int oldX = getScrollX();
      int oldY = getScrollY();
      int x = mScroller.getCurrX();
      int y = mScroller.getCurrY();

      if (oldX != x || oldY != y) {
        scrollTo(x, y);
        if (!pageScrolled(x)) {
          mScroller.abortAnimation();
          scrollTo(0, y);
        }
      }

      // Keep on drawing until the animation has finished.
      ViewCompat.postInvalidateOnAnimation(this);
      return;
    }

    // Done with scroll, clean up state.
    completeScroll(true);
  }

  private void recomputeScrollPosition(int width, int oldWidth, int margin, int oldMargin) {
    if (oldWidth > 0 && !mItems.isEmpty()) {
      if (!mScroller.isFinished()) {
        mScroller.setFinalX(getCurrentItem() * getClientWidth());
      } else {
        final int widthWithMargin = width - getPaddingLeft() - getPaddingRight() + margin;
        final int oldWidthWithMargin = oldWidth - getPaddingLeft() - getPaddingRight()
            + oldMargin;
        final int xpos = getScrollX();
        final float pageOffset = (float) xpos / oldWidthWithMargin;
        final int newOffsetPixels = (int) (pageOffset * widthWithMargin);

        scrollTo(newOffsetPixels, getScrollY());
      }
    } else {
      final ItemInfo ii = infoForPosition(mCurItem);
      final float scrollOffset = ii != null ? Math.min(ii.offset, mLastOffset) : 0;
      final int scrollPos =
          (int) (scrollOffset * (width - getPaddingLeft() - getPaddingRight()));
      if (scrollPos != getScrollX()) {
        completeScroll(false);
        scrollTo(scrollPos, getScrollY());
      }
    }
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onMotionEvent will be called and we do the actual
         * scrolling there.
         */

    final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;

    // Always take care of the touch gesture being complete.
    if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
      // Release the drag.
      resetTouch();
      return false;
    }

    // Nothing more to do here if we have decided whether or not we
    // are dragging.
    if (action != MotionEvent.ACTION_DOWN) {
      if (mIsBeingDragged) {
        return true;
      }
      if (mIsUnableToDrag) {
        return false;
      }
    }

    switch (action) {
      case MotionEvent.ACTION_MOVE: {
                /*
                 * mIsBeingDragged == false, otherwise the shortcut would have caught it. Check
                 * whether the user has moved far enough from his original down touch.
                 */

                /*
                * Locally do absolute value. mLastMotionY is set to the y value
                * of the down event.
                */
        final int activePointerId = mActivePointerId;
        if (activePointerId == INVALID_POINTER) {
          // If we don't have a valid id, the touch down wasn't on content.
          break;
        }

        final int pointerIndex = ev.findPointerIndex(activePointerId);
        final float x = ev.getX(pointerIndex);
        final float dx = x - mLastMotionX;
        final float xDiff = Math.abs(dx);
        final float y = ev.getY(pointerIndex);
        final float yDiff = Math.abs(y - mInitialMotionY);

        if (dx != 0 && !isGutterDrag(mLastMotionX, dx)
            && canScroll(this, false, (int) dx, (int) x, (int) y)) {
          // Nested view has scrollable area under this point. Let it be handled there.
          mLastMotionX = x;
          mLastMotionY = y;
          mIsUnableToDrag = true;
          return false;
        }
        if (xDiff > mTouchSlop && xDiff * 0.5f > yDiff) {
          mIsBeingDragged = true;
          requestParentDisallowInterceptTouchEvent(true);
          setScrollState(SCROLL_STATE_DRAGGING);
          mLastMotionX = dx > 0
              ? mInitialMotionX + mTouchSlop : mInitialMotionX - mTouchSlop;
          mLastMotionY = y;
        } else if (yDiff > mTouchSlop) {
          // The finger has moved enough in the vertical
          // direction to be counted as a drag...  abort
          // any attempt to drag horizontally, to work correctly
          // with children that have scrolling containers.
          mIsUnableToDrag = true;
        }
        if (mIsBeingDragged) {
          // Scroll to follow the motion event
          if (performDrag(x)) {
            ViewCompat.postInvalidateOnAnimation(this);
          }
        }
        break;
      }

      case MotionEvent.ACTION_DOWN: {
                /*
                 * Remember location of down touch.
                 * ACTION_DOWN always refers to pointer index 0.
                 */
        mLastMotionX = mInitialMotionX = ev.getX();
        mLastMotionY = mInitialMotionY = ev.getY();
        mActivePointerId = ev.getPointerId(0);
        mIsUnableToDrag = false;

        mIsScrollStarted = true;
        mScroller.computeScrollOffset();
        if (mScrollState == SCROLL_STATE_SETTLING
            && Math.abs(mScroller.getFinalX() - mScroller.getCurrX()) > mCloseEnough) {
          // Let the user 'catch' the pager as it animates.
          mScroller.abortAnimation();
          mPopulatePending = false;
          populate();
          mIsBeingDragged = true;
          requestParentDisallowInterceptTouchEvent(true);
          setScrollState(SCROLL_STATE_DRAGGING);
        } else {
          completeScroll(false);
          mIsBeingDragged = false;
        }

        break;
      }

      case MotionEventCompat.ACTION_POINTER_UP:
        onSecondaryPointerUp(ev);
        break;
    }

    if (mVelocityTracker == null) {
      mVelocityTracker = VelocityTracker.obtain();
    }
    mVelocityTracker.addMovement(ev);

        /*
         * The only time we want to intercept motion events is if we are in the
         * drag mode.
         */
    return mIsBeingDragged;
  }

  private boolean isGutterDrag(float x, float dx) {
    return (x < mGutterSize && dx > 0) || (x > getWidth() - mGutterSize && dx < 0);
  }

  protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
    if (v instanceof ViewGroup) {
      final ViewGroup group = (ViewGroup) v;
      final int scrollX = v.getScrollX();
      final int scrollY = v.getScrollY();
      final int count = group.getChildCount();
      // Count backwards - let topmost views consume scroll distance first.
      for (int i = count - 1; i >= 0; i--) {
        // TODO: Add versioned support here for transformed views.
        // This will not work for transformed views in Honeycomb+
        final View child = group.getChildAt(i);
        if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight()
            && y + scrollY >= child.getTop() && y + scrollY < child.getBottom()
            && canScroll(child, true, dx, x + scrollX - child.getLeft(),
            y + scrollY - child.getTop())) {
          return true;
        }
      }
    }

    return checkV && ViewCompat.canScrollHorizontally(v, -dx);
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    if (mFakeDragging) {
      // A fake drag is in progress already, ignore this real one
      // but still eat the touch events.
      // (It is likely that the user is multi-touching the screen.)
      return true;
    }

    if (ev.getAction() == MotionEvent.ACTION_DOWN && ev.getEdgeFlags() != 0) {
      // Don't handle edge touches immediately -- they may actually belong to one of our
      // descendants.
      return false;
    }

    if (mAdapter == null || mAdapter.getCount() == 0) {
      // Nothing to present or scroll; nothing to touch.
      return false;
    }

    if (mVelocityTracker == null) {
      mVelocityTracker = VelocityTracker.obtain();
    }
    mVelocityTracker.addMovement(ev);

    final int action = ev.getAction();
    boolean needsInvalidate = false;

    switch (action & MotionEventCompat.ACTION_MASK) {
      case MotionEvent.ACTION_DOWN: {
        mScroller.abortAnimation();
        mPopulatePending = false;
        populate();

        // Remember where the motion event started
        mLastMotionX = mInitialMotionX = ev.getX();
        mLastMotionY = mInitialMotionY = ev.getY();
        mActivePointerId = ev.getPointerId(0);
        break;
      }
      case MotionEvent.ACTION_MOVE:
        if (!mIsBeingDragged) {
          final int pointerIndex = ev.findPointerIndex(mActivePointerId);
          if (pointerIndex == -1) {
            // A child has consumed some touch events and put us into an inconsistent
            // state.
            needsInvalidate = resetTouch();
            break;
          }
          final float x = ev.getX(pointerIndex);
          final float xDiff = Math.abs(x - mLastMotionX);
          final float y = ev.getY(pointerIndex);
          final float yDiff = Math.abs(y - mLastMotionY);
          if (xDiff > mTouchSlop && xDiff > yDiff) {
            mIsBeingDragged = true;
            requestParentDisallowInterceptTouchEvent(true);
            mLastMotionX = x - mInitialMotionX > 0 ? mInitialMotionX + mTouchSlop :
                mInitialMotionX - mTouchSlop;
            mLastMotionY = y;
            setScrollState(SCROLL_STATE_DRAGGING);

            // Disallow Parent Intercept, just in case
            ViewParent parent = getParent();
            if (parent != null) {
              parent.requestDisallowInterceptTouchEvent(true);
            }
          }
        }
        // Not else! Note that mIsBeingDragged can be set above.
        if (mIsBeingDragged) {
          // Scroll to follow the motion event
          final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
          final float x = ev.getX(activePointerIndex);
          needsInvalidate |= performDrag(x);
        }
        break;
      case MotionEvent.ACTION_UP:
        if (mIsBeingDragged) {
          final VelocityTracker velocityTracker = mVelocityTracker;
          velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
          int initialVelocity = (int) VelocityTrackerCompat.getXVelocity(
              velocityTracker, mActivePointerId);
          mPopulatePending = true;
          final int width = getClientWidth();
          final int scrollX = getScrollX();
          final ItemInfo ii = infoForCurrentScrollPosition();
          final float marginOffset = (float) mPageMargin / width;
          final int currentPage = ii.position;
          final float pageOffset = (((float) scrollX / width) - ii.offset)
              / (ii.widthFactor + marginOffset);
          final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
          final float x = ev.getX(activePointerIndex);
          final int totalDelta = (int) (x - mInitialMotionX);
          int nextPage = determineTargetPage(currentPage, pageOffset, initialVelocity,
              totalDelta);
          setCurrentItemInternal(nextPage, true, true, initialVelocity);

          needsInvalidate = resetTouch();
        }
        break;
      case MotionEvent.ACTION_CANCEL:
        if (mIsBeingDragged) {
          scrollToItem(mCurItem, true, 0, false);
          needsInvalidate = resetTouch();
        }
        break;
      case MotionEventCompat.ACTION_POINTER_DOWN: {
        final int index = MotionEventCompat.getActionIndex(ev);
        final float x = ev.getX(index);
        mLastMotionX = x;
        mActivePointerId = ev.getPointerId(index);
        break;
      }
      case MotionEventCompat.ACTION_POINTER_UP:
        onSecondaryPointerUp(ev);
        mLastMotionX = ev.getX(ev.findPointerIndex(mActivePointerId));
        break;
    }
    if (needsInvalidate) {
      ViewCompat.postInvalidateOnAnimation(this);
    }
    return true;
  }

  private boolean performDrag(float x) {
    boolean needsInvalidate = false;

    final float deltaX = mLastMotionX - x;
    mLastMotionX = x;

    float oldScrollX = getScrollX();
    float scrollX = oldScrollX + deltaX;
    final int width = getClientWidth();

    float leftBound = width * mFirstOffset;
    float rightBound = width * mLastOffset;
    boolean leftAbsolute = true;
    boolean rightAbsolute = true;

    final ItemInfo firstItem = mItems.get(0);
    final ItemInfo lastItem = mItems.get(mItems.size() - 1);
    if (firstItem.position != 0) {
      leftAbsolute = false;
      leftBound = firstItem.offset * width;
    }
    if (lastItem.position != mAdapter.getCount() - 1) {
      rightAbsolute = false;
      rightBound = lastItem.offset * width;
    }

    if (scrollX < leftBound) {
      if (leftAbsolute) {
        float over = leftBound - scrollX;
        needsInvalidate = mLeftEdge.onPull(Math.abs(over) / width);
      }
      scrollX = leftBound;
    } else if (scrollX > rightBound) {
      if (rightAbsolute) {
        float over = scrollX - rightBound;
        needsInvalidate = mRightEdge.onPull(Math.abs(over) / width);
      }
      scrollX = rightBound;
    }
    // Don't lose the rounded component
    mLastMotionX += scrollX - (int) scrollX;
    scrollTo((int) scrollX, getScrollY());
    pageScrolled((int) scrollX);

    return needsInvalidate;
  }

  private void onSecondaryPointerUp(MotionEvent ev) {
    final int pointerIndex = MotionEventCompat.getActionIndex(ev);
    final int pointerId = ev.getPointerId(pointerIndex);
    if (pointerId == mActivePointerId) {
      // This was our active pointer going up. Choose a new
      // active pointer and adjust accordingly.
      final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
      mLastMotionX = ev.getX(newPointerIndex);
      mActivePointerId = ev.getPointerId(newPointerIndex);
      if (mVelocityTracker != null) {
        mVelocityTracker.clear();
      }
    }
  }

  private int determineTargetPage(int currentPage, float pageOffset, int velocity, int deltaX) {
    int targetPage;
    if (Math.abs(deltaX) > mFlingDistance && Math.abs(velocity) > mMinimumVelocity) {
      targetPage = velocity > 0 ? currentPage : currentPage + 1;
    } else {
      final float truncator = currentPage >= mCurItem ? 0.4f : 0.6f;
      targetPage = currentPage + (int) (pageOffset + truncator);
    }

    if (mItems.size() > 0) {
      final ItemInfo firstItem = mItems.get(0);
      final ItemInfo lastItem = mItems.get(mItems.size() - 1);

      // Only let the user target pages we have items for
      targetPage = Math.max(firstItem.position, Math.min(targetPage, lastItem.position));
    }

    return targetPage;
  }

  private boolean resetTouch() {
    boolean needsInvalidate;
    mActivePointerId = INVALID_POINTER;
    endDrag();
    needsInvalidate = mLeftEdge.onRelease() | mRightEdge.onRelease();
    return needsInvalidate;
  }

  private void endDrag() {
    mIsBeingDragged = false;
    mIsUnableToDrag = false;

    if (mVelocityTracker != null) {
      mVelocityTracker.recycle();
      mVelocityTracker = null;
    }
  }

  private void requestParentDisallowInterceptTouchEvent(boolean disallowIntercept) {
    final ViewParent parent = getParent();
    if (parent != null) {
      parent.requestDisallowInterceptTouchEvent(disallowIntercept);
    }
  }

  @Override
  protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
    return new LayoutParams();
  }

  @Override
  protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
    return generateDefaultLayoutParams();
  }

  @Override
  protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
    return p instanceof LayoutParams && super.checkLayoutParams(p);
  }

  @Override
  public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
    return new LayoutParams(getContext(), attrs);
  }

  private class PagerObserver extends DataSetObserver {
    PagerObserver() {
    }

    @Override
    public void onChanged() {
      dataSetChanged();
    }
    @Override
    public void onInvalidated() {
      dataSetChanged();
    }
  }

  public static class LayoutParams extends ViewGroup.LayoutParams {
    public int gravity;
    float widthFactor = 0.f;
    int childIndex;
    int position;
    boolean needsMeasure;

    public LayoutParams() {
      super(MATCH_PARENT, MATCH_PARENT);
    }

    public LayoutParams(Context context, AttributeSet attrs) {
      super(context, attrs);

      final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
      gravity = a.getInteger(0, Gravity.TOP);
      a.recycle();
    }
  }

  static class ViewPositionComparator implements Comparator<View> {
    @Override
    public int compare(View lhs, View rhs) {
      final LayoutParams llp = (LayoutParams) lhs.getLayoutParams();
      final LayoutParams rlp = (LayoutParams) rhs.getLayoutParams();
      return llp.position - rlp.position;
    }
  }
}
