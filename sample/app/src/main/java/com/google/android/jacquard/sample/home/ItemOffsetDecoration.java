package com.google.android.jacquard.sample.home;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Item decoration for Recyclerview with Tiles with GridLayout and section header for {@link
 * HomeTilesListAdapter}. This decoration ignore spacing for section header and provides spacing for
 * tiles, we need to define condition for the tile in {@link ItemOffsetDecoration#getItemOffsets} to
 * assign spacing.
 */
public class ItemOffsetDecoration extends RecyclerView.ItemDecoration {

  private final int mItemOffset;

  public ItemOffsetDecoration(int itemOffset) {
    mItemOffset = itemOffset;
  }

  public ItemOffsetDecoration(@NonNull Context context, @DimenRes int itemOffsetId) {
    this(context.getResources().getDimensionPixelSize(itemOffsetId));
  }

  /** Calculates and assign the spacing to the Item of the recyclerview. */
  @Override
  public void getItemOffsets(
      Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
    super.getItemOffsets(outRect, view, parent, state);
    switch (HomeTileModel.Type.values()[parent.getChildViewHolder(view).getItemViewType()]) {
      case TILE_API:
      case TILE_SAMPLE_USE_CASE:
        outRect.set(mItemOffset, mItemOffset, mItemOffset, mItemOffset);
    }
  }
}
