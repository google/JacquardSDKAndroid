/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sample.home

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.annotation.DimenRes
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.google.android.jacquard.sample.home.HomeTileModel

/**
 * Item decoration for Recyclerview with Tiles with GridLayout and section header for [ ]. This decoration ignore spacing for section header and provides spacing for
 * tiles, we need to define condition for the tile in [ItemOffsetDecoration.getItemOffsets] to
 * assign spacing.
 */
class ItemOffsetDecoration(private val mItemOffset: Int) : ItemDecoration() {
    constructor(
        context: Context,
        @DimenRes itemOffsetId: Int
    ) : this(context.resources.getDimensionPixelSize(itemOffsetId))

    /** Calculates and assign the spacing to the Item of the recyclerview.  */
    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        when (HomeTileModel.Type.values()[parent.getChildViewHolder(view).itemViewType]) {
            HomeTileModel.Type.TILE_API, HomeTileModel.Type.TILE_SAMPLE_USE_CASE -> outRect[mItemOffset, mItemOffset, mItemOffset] =
                mItemOffset
        }
    }
}