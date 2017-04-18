package com.twentyhours.androidstudy.elasticdismiss;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.annotation.AttrRes;
import android.support.annotation.CheckResult;
import android.support.annotation.ColorInt;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.FrameLayout;

import com.twentyhours.androidstudy.R;

import java.util.ArrayList;
import java.util.List;

import static com.twentyhours.androidstudy.viewpager.InkPageIndicator.getFastOutSlowInInterpolator;

/**
 * 위나 아래로 지정된 값 이상 스크롤시 onDragDismissed를 호출한다.
 *   이때의 distance와 scale에 대한 조정이 가능
 *   ElasticDragDismissCallback을 등록
 *
 * 내부의 스크롤 가능한 뷰가 다음 값이 true여야 한다
 *   android:nestedScrollingEnabled="true"
 *
 * 아래의 네 함수를 통해 스크롤 정보를 얻어와서 dismiss 여부를 처리한다.
 * 따라서 내부의 스크롤 가능한 뷰가 nested scrolling이 활성화되어야 한다.
 *   onStartNestedScroll
 *   onNestedPreScroll
 *   onNestedScroll
 *   onStopNestedScroll
 */

public class ElasticDragDismissFrameLayout extends FrameLayout {
  // 얼마만큼 스크롤을 했을때 dismiss를 할 것인지의 값.
  private float dragDismissDistance = Float.MAX_VALUE;
  // 이 값이 설정되어 있는 경우 `onSizeChanged`가 호출될 때 이 값으로부터 dragDismissDistance을 계산한다.
  private float dragDismissFraction = -1f;
  // 스크롤시 얼마만큼 scale를 할 것인지의 값(비율).
  private float dragDismissScale = 1f;
  // scale을 하면 참이고 하지 않으면 거짓.
  private boolean shouldScale = false;
  // 실제 스크롤보다 얼마나 적게 이동할 것인지의 값(비율).
  private float dragElasticity = 0.8f;

  // ElasticDragDismissCallback에 대한 리스너. 여러게 등록 가능
  private List<ElasticDragDismissCallback> callbacks;

  // 얼마만큼 스크롤이 일어났는가. dragDismissDistance와의 비교를 위해 저장하는 값.
  private float totalDrag;
  private boolean draggingDown = false;
  private boolean draggingUp = false;

  public ElasticDragDismissFrameLayout(@NonNull Context context) {
    this(context, null, 0, 0);
  }

  public ElasticDragDismissFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0, 0);
  }

  public ElasticDragDismissFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  public ElasticDragDismissFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);

    final TypedArray a = getContext().obtainStyledAttributes(
        attrs, R.styleable.ElasticDragDismissFrameLayout, 0, 0);

    if (a.hasValue(R.styleable.ElasticDragDismissFrameLayout_dragDismissDistance)) {
      dragDismissDistance = a.getDimensionPixelSize(R.styleable
          .ElasticDragDismissFrameLayout_dragDismissDistance, 0);
    } else if (a.hasValue(R.styleable.ElasticDragDismissFrameLayout_dragDismissFraction)) {
      dragDismissFraction = a.getFloat(R.styleable
          .ElasticDragDismissFrameLayout_dragDismissFraction, dragDismissFraction);
    }
    if (a.hasValue(R.styleable.ElasticDragDismissFrameLayout_dragDismissScale)) {
      dragDismissScale = a.getFloat(R.styleable
          .ElasticDragDismissFrameLayout_dragDismissScale, dragDismissScale);
      shouldScale = dragDismissScale != 1f;
    }
    if (a.hasValue(R.styleable.ElasticDragDismissFrameLayout_dragElasticity)) {
      dragElasticity = a.getFloat(R.styleable.ElasticDragDismissFrameLayout_dragElasticity,
          dragElasticity);
    }

    a.recycle();
  }

  public static abstract class ElasticDragDismissCallback {
    /**
     * drag event 발생시 호출되는 함수.
     * elasticOffset: (elasticOffsetPixels / dismiss되는 거리) - 비율
     * elasticOffsetPixels: 이동시킬 값
     * rawOffset: (rawOffsetPixels / dismiss되는 거리) - 비율
     * rawOffsetPixels: 사용자가 실제로 스크롤한 거리
     */
    void onDrag(float elasticOffset, float elasticOffsetPixels,
                float rawOffset, float rawOffsetPixels) { }

    /**
     * dragDismissDistance를 넘어서게 스크롤을 한 경우 호출되는 함수.
     */
    void onDragDismissed() { }
  }

  public void addListener(ElasticDragDismissCallback listener) {
    if (callbacks == null) {
      callbacks = new ArrayList<>();
    }
    callbacks.add(listener);
  }

  public void removeListener(ElasticDragDismissCallback listener) {
    if (callbacks != null && callbacks.size() > 0) {
      callbacks.remove(listener);
    }
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    if (dragDismissFraction > 0f) {
      // dragDismissFraction이 설정되어 있는 경우 이 값을 기준으로 dragDismissDistance을 계산한다.
      dragDismissDistance = h * dragDismissFraction;
    }
  }

  /**
   * onStartNestedScroll -> onNestedPreScroll -> onNestedScroll -> onStopNestedScroll
   *                               ^                  V
   *                               |                  |
   *                               --------------------
   */

  /**
   * child가 스크롤을 시작했음을 알린다.
   * vertical 스크롤인 경우 true를 리턴한다.
   */
  @Override
  public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
    return (nestedScrollAxes & View.SCROLL_AXIS_VERTICAL) != 0;
  }

  /**
   * child가 스크롤을 진행하기 전에 작업을 한다. Parent가 직접 사용하고자 하는 스크롤 값이 있으면
   * 이 값을 적용하고 consumed에 적용한 값을 적어준다. -> child는 consumed에 있는 값은
   * 제외한 값만큼만 스크롤을 적용할 것이다.
   *   dx: Horizontal scroll distance in pixels
   *   dy: Vertical scroll distance in pixels
   *   consumed: Index 0 corresponds to dx and index 1 corresponds to dy
   */
  @Override
  public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
    // if we're in a drag gesture and the user reverses up then we should take those events
    // 위나 아래로 터치 스크롤을 진행중에 방향을 바꾸는 경우 parent에서 dy값을 적용한다.
    if (draggingDown && dy > 0 || draggingUp && dy < 0) {
      dragScale(dy);
      consumed[1] = dy;
    }
  }

  /**
   * 스크롤이 진행중일때 호출된다.
   *   dxConsumed: Horizontal scroll distance in pixels already consumed by target
   *   dyConsumed: Vertical scroll distance in pixels already consumed by target
   *   dxUnconsumed: Horizontal scroll distance in pixels not consumed by target
   *   dyUnconsumed: Vertical scroll distance in pixels not consumed by target
   */
  @Override
  public void onNestedScroll(View target, int dxConsumed, int dyConsumed,
                             int dxUnconsumed, int dyUnconsumed) {
    dragScale(dyUnconsumed);
  }

  /**
   * 스크롤이 끝났음을 알린다.
   */
  @Override
  public void onStopNestedScroll(View child) {
    if (Math.abs(totalDrag) >= dragDismissDistance) {
      // 스크롤이 지정한 값 이상이면 호출한다.
      dispatchDismissCallback();
    } else { // settle back to natural position
      // 원 위치로 돌아간다.
      animate()
          .translationY(0f)
          .scaleX(1f)
          .scaleY(1f)
          .setDuration(200L)
          .setInterpolator(getFastOutSlowInInterpolator(getContext()))
          .setListener(null)
          .start();
      totalDrag = 0;
      draggingDown = draggingUp = false;
      dispatchDragCallback(0f, 0f, 0f, 0f);
    }
  }

  private void dragScale(int scroll) {
    if (scroll == 0) return;

    totalDrag += scroll;

    // track the direction & set the pivot point for scaling
    // don't double track i.e. if start dragging down and then reverse, keep tracking as
    // dragging down until they reach the 'natural' position
    // 스크롤의 시작인 경우 관련 값을 설정한다.
    if (scroll < 0 && !draggingUp && !draggingDown) {
      draggingDown = true;
      if (shouldScale) setPivotY(getHeight());
    } else if (scroll > 0 && !draggingDown && !draggingUp) {
      draggingUp = true;
      if (shouldScale) setPivotY(0f);
    }
    // how far have we dragged relative to the distance to perform a dismiss
    // (0–1 where 1 = dismiss distance). Decreasing logarithmically as we approach the limit
    // dismiss 기준 얼마만큼의 비율을 이동하였는가를 계산. 로그를 사용하여 기준값에 가까워질수록 적게 변화하도록 한다.
    float dragFraction = (float) Math.log10(1 + (Math.abs(totalDrag) / dragDismissDistance));

    // calculate the desired translation given the drag fraction
    // 이동시킬 거리를 계산한다.
    float dragTo = dragFraction * dragDismissDistance * dragElasticity;

    if (draggingUp) {
      // as we use the absolute magnitude when calculating the drag fraction, need to
      // re-apply the drag direction
      // 위로 스크롤인 경우 값을 음수로 조정하여 화면이동이 위로 되도록 한다.
      dragTo *= -1;
    }
    // 화면 이동
    setTranslationY(dragTo);

    if (shouldScale) { // 화면 scale
      final float scale = 1 - ((1 - dragDismissScale) * dragFraction);
      setScaleX(scale);
      setScaleY(scale);
    }

    // if we've reversed direction and gone past the settle point then clear the flags to
    // allow the list to get the scroll events & reset any transforms
    if ((draggingDown && totalDrag >= 0)
        || (draggingUp && totalDrag <= 0)) {
      // 스크롤하다가 방향을 바꾸어 시작점 이상으로 넘어간 경우 원래 위치와 크기로 초기화한다.
      totalDrag = dragTo = dragFraction = 0;
      draggingDown = draggingUp = false;
      setTranslationY(0f);
      setScaleX(1f);
      setScaleY(1f);
    }
    dispatchDragCallback(dragFraction, dragTo,
        Math.min(1f, Math.abs(totalDrag) / dragDismissDistance), totalDrag);
  }

  /**
   * onDragDismissed를 호출한다.
   */
  private void dispatchDismissCallback() {
    if (callbacks != null && !callbacks.isEmpty()) {
      for (ElasticDragDismissCallback callback : callbacks) {
        callback.onDragDismissed();
      }
    }
  }

  /**
   * onDrag를 호출한다.
   */
  private void dispatchDragCallback(float elasticOffset, float elasticOffsetPixels,
                                    float rawOffset, float rawOffsetPixels) {
    if (callbacks != null && !callbacks.isEmpty()) {
      for (ElasticDragDismissCallback callback : callbacks) {
        callback.onDrag(elasticOffset, elasticOffsetPixels,
            rawOffset, rawOffsetPixels);
      }
    }
  }

  /**
   * fades system chrome (i.e. status bar and navigation bar)
   */
  public static class SystemChromeFader extends ElasticDragDismissCallback {
    private final Activity activity;
    private final int statusBarAlpha;
    private final int navBarAlpha;
    private final boolean fadeNavBar;

    public SystemChromeFader(Activity activity) {
      this.activity = activity;
      statusBarAlpha = Color.alpha(activity.getWindow().getStatusBarColor());
      navBarAlpha = Color.alpha(activity.getWindow().getNavigationBarColor());
      fadeNavBar = isNavBarOnBottom(activity);
    }

    @Override
    public void onDrag(float elasticOffset, float elasticOffsetPixels,
                       float rawOffset, float rawOffsetPixels) {
      // 위치에 따라 알파값을 조정한다.
      if (elasticOffsetPixels > 0) {
        // dragging downward, fade the status bar in proportion
        activity.getWindow().setStatusBarColor(modifyAlpha(activity.getWindow()
            .getStatusBarColor(), (int) ((1f - rawOffset) * statusBarAlpha)));
      } else if (elasticOffsetPixels == 0) {
        // reset
        activity.getWindow().setStatusBarColor(modifyAlpha(
            activity.getWindow().getStatusBarColor(), statusBarAlpha));
        activity.getWindow().setNavigationBarColor(modifyAlpha(
            activity.getWindow().getNavigationBarColor(), navBarAlpha));
      } else if (fadeNavBar) {
        // dragging upward, fade the navigation bar in proportion
        activity.getWindow().setNavigationBarColor(
            modifyAlpha(activity.getWindow().getNavigationBarColor(),
                (int) ((1f - rawOffset) * navBarAlpha)));
      }
    }

    public void onDragDismissed() {
      // 액티비티를 종료한다.
      activity.finishAfterTransition();
    }
  }

  /**
   * Determine if the navigation bar will be on the bottom of the screen, based on logic in
   * PhoneWindowManager.
   */
  public static boolean isNavBarOnBottom(@NonNull Context context) {
    final Resources res= context.getResources();
    final Configuration cfg = context.getResources().getConfiguration();
    final DisplayMetrics dm =res.getDisplayMetrics();
    boolean canMove = (dm.widthPixels != dm.heightPixels &&
        cfg.smallestScreenWidthDp < 600);
    return (!canMove || dm.widthPixels < dm.heightPixels);
  }

  /**
   * Set the alpha component of {@code color} to be {@code alpha}.
   */
  public static @CheckResult
  @ColorInt
  int modifyAlpha(@ColorInt int color,
                  @IntRange(from = 0, to = 255) int alpha) {
    return (color & 0x00ffffff) | (alpha << 24);
  }
}
