package com.twentyhours.androidstudy.recyclerview;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.twentyhours.androidstudy.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by soonhyung-imac on 4/4/17.
 */

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {
  private List<String> items = new ArrayList<>();

  public void addAll(List<String> list) {
    items.addAll(list);
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.item_recycler_view, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(ViewHolder holder, int position) {
    holder.bind(items.get(position));
  }

  @Override
  public int getItemCount() {
    return items.size();
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    TextView nameView;

    public ViewHolder(View itemView) {
      super(itemView);
      nameView = (TextView) itemView.findViewById(R.id.name);
    }

    public void bind(String item) {
      nameView.setText(item);
    }
  }
}
