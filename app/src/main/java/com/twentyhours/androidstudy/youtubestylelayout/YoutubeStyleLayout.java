package com.twentyhours.androidstudy.youtubestylelayout;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Created by soonhyung-imac on 3/23/17.
 */

public class YoutubeStyleLayout extends ViewGroup {
  private int mDragRange;
  private final ViewDragHelper mDragHelper;
  private int mTop;
  private float mDragOffset;

  private float mInitialMotionX;
  private float mInitialMotionY;

  private View slidingView;
  private View mainView;

  public YoutubeStyleLayout(Context context) {
    this(context, null);
  }

  public YoutubeStyleLayout(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public YoutubeStyleLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    mDragHelper = ViewDragHelper.create(this, 1f, new DragHelperCallback());
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
    final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

    final int childCount = getChildCount();
    if (childCount != 2) {
      throw new IllegalStateException("Layout must have exactly 2 children!");
    }

    mainView = getChildAt(0); // should become first child
    slidingView = getChildAt(1); // should become second child

    int height = heightSize - getPaddingTop() - getPaddingBottom();
    int width = widthSize - getPaddingLeft() - getPaddingRight();

    // First pass. Measure based on child LayoutParams width/height.
    for (int i = 0; i < childCount; i++) {
      final View child = getChildAt(i);
      final LayoutParams lp = child.getLayoutParams();

      int childWidthSpec;
      if (lp.width == FrameLayout.LayoutParams.WRAP_CONTENT) {
        childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST);
      } else if (lp.width == FrameLayout.LayoutParams.MATCH_PARENT) {
        childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
      } else {
        childWidthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
      }

      int childHeightSpec;
      if (lp.height == FrameLayout.LayoutParams.WRAP_CONTENT) {
        childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);
      } else if (lp.height == FrameLayout.LayoutParams.MATCH_PARENT) {
        childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
      } else {
        childHeightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
      }
      child.measure(childWidthSpec, childHeightSpec);
    }

    setMeasuredDimension(widthSize, heightSize);
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    mDragRange = getHeight() - slidingView.getHeight();

    slidingView.layout(
        0,
        mTop,
        r,
        mTop + slidingView.getMeasuredHeight());

    int top = slidingView.getMeasuredHeight() - mTop;
    if (top < 0) {
      top = 0;
    }

    int bottom = b < (top + mainView.getMeasuredHeight())
        ? b
        : top + mainView.getMeasuredHeight();
    mainView.layout(0, top, r, bottom);
  }

  @Override
  public void computeScroll() {
    if (mDragHelper.continueSettling(true)) {
      ViewCompat.postInvalidateOnAnimation(this);
    }
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    final int action = MotionEventCompat.getActionMasked(ev);

    final float x = ev.getX();
    final float y = ev.getY();
    boolean interceptTap = false;

    switch (action) {
      case MotionEvent.ACTION_DOWN: {
        mInitialMotionX = x;
        mInitialMotionY = y;
        interceptTap = isViewHit(slidingView, (int) x, (int) y);
        if (!interceptTap) {
          mDragHelper.cancel();
          return false;
        }
        break;
      }
      case MotionEvent.ACTION_MOVE: {
        final float adx = Math.abs(x - mInitialMotionX);
        final float ady = Math.abs(y - mInitialMotionY);
        final int slop = mDragHelper.getTouchSlop();
        if (ady > slop && adx > ady) {
          mDragHelper.cancel();
          return false;
        }
      }
    }

    return mDragHelper.shouldInterceptTouchEvent(ev) || interceptTap;
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    if (!isEnabled()) {
      return super.onTouchEvent(ev);
    }

    mDragHelper.processTouchEvent(ev);

    final int action = ev.getAction();
    final float x = ev.getX();
    final float y = ev.getY();

    boolean isHeaderViewUnder = mDragHelper.isViewUnder(slidingView, (int) x, (int) y);
    switch (action & MotionEventCompat.ACTION_MASK) {
      case MotionEvent.ACTION_DOWN: {
        mInitialMotionX = x;
        mInitialMotionY = y;
        break;
      }
      case MotionEvent.ACTION_UP: {
        final float dx = x - mInitialMotionX;
        final float dy = y - mInitialMotionY;
        final int slop = mDragHelper.getTouchSlop();
        if (dx * dx + dy * dy < slop * slop && isHeaderViewUnder) {
          if (mDragOffset != 0) {
            smoothSlideTo(0f);
          }
        } else if (isHeaderViewUnder){
          if (mDragOffset > 0.5f) {
            smoothSlideTo(1f);
          } else {
            smoothSlideTo(0f);
          }
        }
        break;
      }
    }

    return isHeaderViewUnder && isViewHit(slidingView, (int) x, (int) y) || isViewHit(mainView, (int) x, (int) y);
  }

  private boolean isViewHit(View view, int x, int y) {
    int[] viewLocation = new int[2];
    view.getLocationOnScreen(viewLocation);
    int[] parentLocation = new int[2];
    this.getLocationOnScreen(parentLocation);
    int screenX = parentLocation[0] + x;
    int screenY = parentLocation[1] + y;
    return screenX >= viewLocation[0] && screenX < viewLocation[0] + view.getWidth() &&
        screenY >= viewLocation[1] && screenY < viewLocation[1] + view.getHeight();
  }

  boolean smoothSlideTo(float slideOffset) {
    final int topBound = getPaddingTop();
    int y = (int) (topBound + slideOffset * mDragRange);

    if (mDragHelper.smoothSlideViewTo(slidingView, slidingView.getLeft(), y)) {
      ViewCompat.postInvalidateOnAnimation(this);
      return true;
    }
    return false;
  }

  private class DragHelperCallback extends ViewDragHelper.Callback {

    @Override
    public boolean tryCaptureView(View child, int pointerId) {
      return child == slidingView;
    }

    @Override
    public int clampViewPositionVertical(View child, int top, int dy) {
      final int topBound = getPaddingTop();
      final int bottomBound = getHeight() - slidingView.getHeight() - slidingView.getPaddingBottom();

      return Math.min(Math.max(top, topBound), bottomBound);
    }

    @Override
    public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
      mTop = top;

      mDragOffset = (float) top / mDragRange;

      slidingView.setPivotX(slidingView.getWidth());
      slidingView.setPivotY(slidingView.getHeight());
      slidingView.setScaleX(1 - mDragOffset / 2);
      slidingView.setScaleY(1 - mDragOffset / 2);

      mainView.setAlpha(1 - mDragOffset);

      requestLayout();
    }

    @Override
    public int getViewVerticalDragRange(View child) {
      return mDragRange;
    }

    @Override
    public void onViewReleased(View releasedChild, float xvel, float yvel) {
      int top = getPaddingTop();
      if (yvel > 0 || (yvel == 0 && mDragOffset > 0.5f)) {
        top += mDragRange;
      }
      mDragHelper.settleCapturedViewAt(releasedChild.getLeft(), top);
    }
  }
}
