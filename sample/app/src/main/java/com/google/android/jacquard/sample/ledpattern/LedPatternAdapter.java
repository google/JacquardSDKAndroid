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
package com.google.android.jacquard.sample.ledpattern;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.jacquard.sample.R;
import java.util.List;

/** Recycler view adapter for {@link LedPatternFragment}. */
public class LedPatternAdapter
    extends RecyclerView.Adapter<LedPatternAdapter.LedPatternViewHolder> {

  private final ItemClickListener itemClickListener;
  private final List<LedPatternItem> patternItemList;
  public LedPatternAdapter(
      List<LedPatternItem> patternItemList, ItemClickListener itemClickListener) {
    this.itemClickListener = itemClickListener;
    this.patternItemList = patternItemList;
  }

  @NonNull
  @Override
  public LedPatternViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new LedPatternViewHolder(
        LayoutInflater.from(parent.getContext()).inflate(R.layout.led_pattern_item, parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull LedPatternViewHolder viewHolder, int position) {
    viewHolder.bindView();
  }

  @Override
  public int getItemCount() {
    return patternItemList.size();
  }

  /** Callback for click events. */
  interface ItemClickListener {

    /** Callback for the a led pattern that has been selected to play. */
    void onItemClick(LedPatternItem patternItem);
  }

  class LedPatternViewHolder extends RecyclerView.ViewHolder {

    private final TextView patternName;
    private final ImageView patternIcon;

    LedPatternViewHolder(View itemView) {
      super(itemView);
      patternName = itemView.findViewById(R.id.txtPattern);
      patternIcon = itemView.findViewById(R.id.imgLedPattern);
      itemView.setOnClickListener(
          v -> itemClickListener.onItemClick(patternItemList.get(getAdapterPosition())));
    }

    void bindView() {
      LedPatternItem ledPatternItem = patternItemList.get(getAdapterPosition());
      patternIcon.setImageResource(ledPatternItem.icon());
      patternName.setText(ledPatternItem.text());
    }
  }
}
