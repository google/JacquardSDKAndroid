/*
 * Copyright 2021 Google LLC. All Rights Reserved.
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
package com.google.android.jacquard.sample.gesture

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.gesture.GestureInfoAdapter.GestureInfoViewHolder

/** Adapter for supported gestures.  */
class GestureInfoAdapter : RecyclerView.Adapter<GestureInfoViewHolder>() {
    companion object {
        private val GESTURE_OVERVIEWS = GestureInfo.values()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GestureInfoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.gesture_overview_item, parent, false)
        return GestureInfoViewHolder(view)
    }

    override fun onBindViewHolder(holder: GestureInfoViewHolder, position: Int) {
        holder.bind(GESTURE_OVERVIEWS[position])
    }

    override fun getItemCount(): Int {
        return GESTURE_OVERVIEWS.size
    }

    class GestureInfoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.gesture_overview_item_img)
        private val title: TextView = itemView.findViewById(R.id.gesture_overview_item_title)
        private val subtitle: TextView = itemView.findViewById(R.id.gesture_overview_item_subtitle)

        fun bind(gestureInfo: GestureInfo) {
            imageView.setImageResource(gestureInfo.imgResId)
            title.setText(gestureInfo.titleResId)
            subtitle.setText(gestureInfo.subtitleResId)
        }
    }
}