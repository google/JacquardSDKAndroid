/*
 * Copyright 2021 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.jacquard.sample.gesture;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.gesture.GestureAdapter.GestureViewHolder;
import com.google.android.jacquard.sample.gesture.GestureViewModel.GestureViewItem;

/** Adapter for showing the gestures performed. */
public class GestureAdapter extends ListAdapter<GestureViewItem, GestureViewHolder> {

  protected GestureAdapter() {
    super(new DiffCallback());
  }

  @NonNull
  @Override
  public GestureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    TextView view =
        (TextView)
            LayoutInflater.from(parent.getContext())
                .inflate(R.layout.gesture_list_item, parent, /*attachToRoot=*/ false);
    return new GestureViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull GestureViewHolder holder, int position) {
    holder.bind(getItem(position));
  }

  static class GestureViewHolder extends ViewHolder {

    private final TextView gestureTextView;

    public GestureViewHolder(@NonNull TextView itemView) {
      super(itemView);
      gestureTextView = itemView;
    }

    public void bind(GestureViewItem item) {
      String text =
          gestureTextView
              .getContext()
              .getString(R.string.gesture_item_body, item.gesture().gestureType().getDescription());
      gestureTextView.setText(text);
    }
  }

  private static class DiffCallback extends DiffUtil.ItemCallback<GestureViewItem> {

    @Override
    public boolean areItemsTheSame(
        @NonNull GestureViewItem oldItem, @NonNull GestureViewItem newItem) {
      return oldItem.id() == newItem.id();
    }

    @SuppressLint("DiffUtilEquals")
    @Override
    public boolean areContentsTheSame(
        @NonNull GestureViewItem oldItem, @NonNull GestureViewItem newItem) {
      return oldItem.equals(newItem);
    }
  }
}
