/*
 * Copyright 2021 Google LLC
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
package com.google.android.jacquard.sample.places;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.places.GestureAdapter.GestureViewHolder;
import java.util.List;

/**
 * Recycler view adapter for {@link PlacesConfigFragment}.
 */
public class GestureAdapter extends RecyclerView.Adapter<GestureViewHolder> {
  private final ItemClickListener itemClickListener;
  private final List<GestureItem> gestureItems;

  public GestureAdapter(List<GestureItem> patternItemList, ItemClickListener itemClickListener) {
    this.itemClickListener = itemClickListener;
    this.gestureItems = patternItemList;
  }

  @NonNull
  @Override
  public GestureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new GestureViewHolder(
        LayoutInflater.from(parent.getContext()).inflate(R.layout.gesture_item, parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull GestureViewHolder viewHolder, int position) {
    viewHolder.bindView(position);
  }

  @Override
  public int getItemCount() {
    return gestureItems.size();
  }

  /**
   * Callback for click events.
   */
  interface ItemClickListener {

    /**
     * Callback for the a gesture that has been selected.
     */
    void onItemClick(GestureItem gestureItem);
  }

  class GestureViewHolder extends RecyclerView.ViewHolder {

    private final RadioButton chooseGesture;

    GestureViewHolder(View itemView) {
      super(itemView);
      chooseGesture = itemView.findViewById(R.id.gesture);
      View.OnClickListener clickListener = v -> {
        updateGestureItems(gestureItems.get(getAdapterPosition()).id);
        notifyItemRangeChanged(0, gestureItems.size());
        itemClickListener.onItemClick(gestureItems.get(getAdapterPosition()));
      };
      chooseGesture.setOnClickListener(clickListener);
      itemView.setOnClickListener(clickListener);
    }

    private void updateGestureItems(int selectedGestureId) {
      for (GestureItem gestureItem : gestureItems) {
        gestureItem.isItemSelected = (gestureItem.id == selectedGestureId);
      }
    }

    void bindView(int position) {
      GestureItem gestureItem = gestureItems.get(position);
      chooseGesture.setChecked(gestureItem.isItemSelected);
      chooseGesture.setText(gestureItem.gesture);
    }
  }
}
