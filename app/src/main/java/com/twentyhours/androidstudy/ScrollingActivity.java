package com.twentyhours.androidstudy;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.twentyhours.androidstudy.flexboxlayout.FlexboxLayoutActivity;

public class ScrollingActivity extends AppCompatActivity {
  private static final int RC_SEARCH = 0;

  Toolbar toolbar;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_scrolling);
    toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
            .setAction("Action", null).show();
      }
    });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_scrolling, menu);
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
      case R.id.action_flexbox:
        Intent intent = new Intent(this, FlexboxLayoutActivity.class);
        startActivity(intent);
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
