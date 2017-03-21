package com.twentyhours.androidstudy;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by soonhyung-imac on 3/21/17.
 */

public class InAndOutAnimationActivity extends AppCompatActivity {
  LinearLayout inandout;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_in_and_out_animation);

    TextView button = (TextView) findViewById(R.id.button);
    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (inandout.getVisibility() == View.VISIBLE) {
          hideView();
        } else {
          showView();
        }
      }
    });

    inandout = (LinearLayout) findViewById(R.id.inandout);
  }

  private void showView() {
    Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in_bottom);
    animation.setAnimationListener(new Animation.AnimationListener() {
      @Override
      public void onAnimationStart(Animation animation) {
        inandout.setVisibility(View.VISIBLE);
      }

      @Override
      public void onAnimationEnd(Animation animation) {

      }

      @Override
      public void onAnimationRepeat(Animation animation) {

      }
    });
    inandout.setVisibility(View.INVISIBLE);
    inandout.startAnimation(animation);
  }

  private void hideView() {
    Animation animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_out_bottom);
    animation.setAnimationListener(new Animation.AnimationListener() {
      @Override
      public void onAnimationStart(Animation animation) {

      }

      @Override
      public void onAnimationEnd(Animation animation) {
        inandout.setVisibility(View.GONE);
      }

      @Override
      public void onAnimationRepeat(Animation animation) {

      }
    });
    inandout.startAnimation(animation);
  }
}
