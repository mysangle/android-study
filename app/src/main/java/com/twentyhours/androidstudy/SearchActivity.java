package com.twentyhours.androidstudy;

import android.app.SearchManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.SearchView;

/**
 * Created by soonhyung-imac on 3/20/17.
 */

public class SearchActivity extends AppCompatActivity {
  SearchView searchView;
  ImageButton searchBack;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_search);

    searchView = (SearchView) findViewById(R.id.search_view);
    setupSearchView();

    searchBack = (ImageButton) findViewById(R.id.searchback);
    searchBack.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        searchBack.setBackground(null);
        finishAfterTransition();
      }
    });
  }

  private void setupSearchView() {
    SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
    searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
    // hint, inputType & ime options seem to be ignored from XML! Set in code
    searchView.setQueryHint("Search");
    searchView.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
    searchView.setImeOptions(searchView.getImeOptions() | EditorInfo.IME_ACTION_SEARCH |
        EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN);
    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String query) {
        // search results
        return true;
      }

      @Override
      public boolean onQueryTextChange(String query) {
        if (TextUtils.isEmpty(query)) {
          // clear results
        }
        return true;
      }
    });
  }

  @Override
  protected void onPause() {
    // needed to suppress the default window animation when closing the activity
    overridePendingTransition(0, 0);
    super.onPause();
  }
}
