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
package com.google.android.jacquard.sample.haptics;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.google.android.jacquard.sample.R;
import com.google.android.jacquard.sample.haptics.HapticsAdapter.HapticsViewHolder;

/** A adapter for {@link HapticsFragment}. */
public class HapticsAdapter extends RecyclerView.Adapter<HapticsViewHolder> {

  private static final HapticPatternType[] HAPTICS = new HapticPatternType[]{
      HapticPatternType.INSERT_PATTERN, HapticPatternType.GESTURE_PATTERN,
      HapticPatternType.NOTIFICATION_PATTERN, HapticPatternType.ERROR_PATTERN,
      HapticPatternType.ALERT_PATTERN};
  private final ItemClickListener itemClickListener;

  HapticsAdapter(ItemClickListener itemClickListener) {
    this.itemClickListener = itemClickListener;
  }

  @Override
  public HapticsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
    View view = layoutInflater.inflate(R.layout.haptic_item, parent, false);
    return new HapticsViewHolder(view);
  }

  @Override
  public void onBindViewHolder(HapticsViewHolder holder, int position) {
    holder.bindView(HAPTICS[position].getDescription());
  }

  @Override
  public int getItemCount() {
    return HAPTICS.length;
  }

  /** Callback for click events. */
  interface ItemClickListener {

    /** Callback for the a haptics. */
    void onItemClick(HapticPatternType type);
  }

  /** A layout place holder for haptic item. */
  class HapticsViewHolder extends ViewHolder implements View.OnClickListener {

    private final View view;

    HapticsViewHolder(View itemView) {
      super(itemView);
      view = itemView;
      view.setOnClickListener(this);
    }

    void bindView(String haptic) {
      TextView tv = view.findViewById(R.id.haptic_pattern_tv);
      tv.setText(haptic);
    }

    @Override
    public void onClick(View v) {
      itemClickListener.onItemClick(HAPTICS[getAdapterPosition()]);
    }
  }
}
