package com.twentyhours.androidstudy.recyclerview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.twentyhours.androidstudy.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by soonhyung-imac on 4/4/17.
 */

public class RecyclerViewActivity extends AppCompatActivity {
  Toolbar toolbar;
  RecyclerView recyclerView;
  RecyclerViewAdapter adapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_recycler_view);

    toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    recyclerView = (RecyclerView) findViewById(R.id.recyclerview);

    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    recyclerView.setLayoutManager(layoutManager);
    adapter = new RecyclerViewAdapter();
    recyclerView.setAdapter(adapter);

    List<String> items = new ArrayList<>();
    items.add("Yellow");
    items.add("Blue");
    items.add("Green");
    items.add("Red");
    items.add("Black");

    adapter.addAll(items);
    adapter.notifyDataSetChanged();
  }
}
