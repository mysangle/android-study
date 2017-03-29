package com.twentyhours.androidstudy;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.twentyhours.androidstudy.flexboxlayout.FlexboxLayoutActivity;
import com.twentyhours.androidstudy.viewpager.ViewPagerActivity;
import com.twentyhours.androidstudy.youtubestylelayout.YoutubeStyleLayoutActivity;

/**
 * Created by soonhyung-imac on 3/21/17.
 */

public class MainActivity extends AppCompatActivity {
  private static final int RC_SEARCH = 0;

  Toolbar toolbar;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    TextView flexboxLayout = (TextView) findViewById(R.id.flexboxlayout);
    flexboxLayout.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startActivity(new Intent(MainActivity.this, FlexboxLayoutActivity.class));
      }
    });

    TextView inandoutanimation = (TextView) findViewById(R.id.inandoutanimation);
    inandoutanimation.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startActivity(new Intent(MainActivity.this, InAndOutAnimationActivity.class));
      }
    });

    final ImageView transitionIcon = (ImageView) findViewById(R.id.transition_icon);
    TextView youtubestylelayout = (TextView) findViewById(R.id.youtubestylelayout);
    youtubestylelayout.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        ActivityOptions options =
            ActivityOptions.makeSceneTransitionAnimation(MainActivity.this,
                transitionIcon, "transition");
        startActivity(new Intent(MainActivity.this, YoutubeStyleLayoutActivity.class), options.toBundle());
      }
    });

    TextView scrolling = (TextView) findViewById(R.id.scrolling);
    scrolling.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startActivity(new Intent(MainActivity.this, ScrollingActivity.class));
      }
    });

    TextView viewPager = (TextView) findViewById(R.id.viewpager);
    viewPager.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startActivity(new Intent(MainActivity.this, ViewPagerActivity.class));
      }
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    switch (item.getItemId()) {
      case R.id.action_settings:
        return true;
      case R.id.menu_search:
        View searchMenuView = toolbar.findViewById(R.id.menu_search);
        Bundle options = ActivityOptions.makeSceneTransitionAnimation(this, searchMenuView,
            getString(R.string.transition_search_back)).toBundle();
        startActivityForResult(new Intent(this, SearchActivity.class), RC_SEARCH, options);
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case RC_SEARCH:
        // reset the search icon which we hid
        View searchMenuView = toolbar.findViewById(R.id.menu_search);
        if (searchMenuView != null) {
          searchMenuView.setAlpha(1f);
        }
        break;
    }
  }
}
