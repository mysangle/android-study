package com.twentyhours.androidstudy.viewpager;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.twentyhours.androidstudy.R;

/**
 * Created by soonhyung-imac on 3/28/17.
 */

public class ViewPagerActivity extends AppCompatActivity {
  Toolbar toolbar;
  ViewPager pager;
  InkPageIndicator pageIndicator;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_view_pager);

    toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    pager = (ViewPager) findViewById(R.id.viewpager);
    ViewPagerAdapter adapter = new ViewPagerAdapter(this);
    pager.setAdapter(adapter);

    pageIndicator = (InkPageIndicator) findViewById(R.id.indicator);
    pageIndicator.setViewPager(pager);
  }

  private static class ViewPagerAdapter extends PagerAdapter {
    private final LayoutInflater layoutInflater;

    private String[] itemText = {"Page 1", "Page 2", "Page 3"};

    ViewPagerAdapter(Context context) {
      layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
      View view = layoutInflater.inflate(R.layout.item_view_pager, collection, false);
      TextView textView = (TextView) view.findViewById(R.id.item);
      textView.setText(itemText[position]);
      collection.addView(view);
      return view;
    }

    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
      collection.removeView((View) view);
    }

    @Override
    public int getCount() {
      return itemText.length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
      return view == object;
    }
  }
}
