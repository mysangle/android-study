package com.twentyhours.androidstudy.youtubestylelayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.support.v7.widget.AppCompatImageView;

import com.twentyhours.androidstudy.R;

/**
 * Created by soonhyung-imac on 3/23/17.
 */

public class AspectRatioImageView extends AppCompatImageView {
  private final float aspectRatio;

  public AspectRatioImageView(Context context, AttributeSet attrs) {
    super(context, attrs);

    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AspectRatio);
    int widthRatio = a.getInteger(R.styleable.AspectRatio_widthRatio, 1);
    int heightRatio = a.getInteger(R.styleable.AspectRatio_heightRatio, 1);
    aspectRatio = (float) widthRatio / heightRatio;
    a.recycle();
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    int receivedWidth = MeasureSpec.getSize(widthMeasureSpec);
    int receivedHeight = MeasureSpec.getSize(heightMeasureSpec);

    int measuredWidth;
    int measuredHeight;
    boolean widthDynamic;
    if (heightMode == MeasureSpec.EXACTLY) {
      widthDynamic = (widthMode != MeasureSpec.EXACTLY) || (receivedWidth == 0);
    } else if (widthMode == MeasureSpec.EXACTLY) {
      widthDynamic = false;
    } else {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
      return;
    }
    if (widthDynamic) {
      // Width is dynamic.
      int w = (int) (receivedHeight * aspectRatio);
      measuredWidth = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY);
      measuredHeight = heightMeasureSpec;
    } else {
      // Height is dynamic.
      measuredWidth = widthMeasureSpec;
      int h = (int) (receivedWidth / aspectRatio);
      measuredHeight = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY);
    }
    super.onMeasure(measuredWidth, measuredHeight);
  }
}
