package com.twentyhours.androidstudy.elasticdismiss;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.transition.TransitionInflater;

import com.twentyhours.androidstudy.R;

/**
 * Created by soonhyung-imac on 4/17/17.
 */

public class ElasticDragDismissActivity extends AppCompatActivity {
  ElasticDragDismissFrameLayout draggableFrame;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_elastic_drag_dismiss);

    draggableFrame = (ElasticDragDismissFrameLayout) findViewById(R.id.draggable_frame);
    draggableFrame.addListener(
        new ElasticDragDismissFrameLayout.SystemChromeFader(this) {
          @Override
          public void onDragDismissed() {
            // if we drag dismiss downward then the default reversal of the enter
            // transition would slide content upward which looks weird. So reverse it.
            if (draggableFrame.getTranslationY() > 0) {
              // 아래로 액티비티를 종료시키는 transition을 사용한다.
              getWindow().setReturnTransition(
                  TransitionInflater.from(ElasticDragDismissActivity.this)
                      .inflateTransition(R.transition.about_return_downward));
            }
            finishAfterTransition();
          }
        });
  }
}
