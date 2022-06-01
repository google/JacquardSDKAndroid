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

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.gesture.GestureViewModel.GestureViewItem

/** Adapter for showing the gestures performed.  */
class GestureAdapter :
    ListAdapter<GestureViewItem, GestureAdapter.GestureViewHolder>(DiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GestureViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.gesture_list_item, parent,  /*attachToRoot=*/false) as TextView
        return GestureViewHolder(view)
    }

    override fun onBindViewHolder(holder: GestureViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class GestureViewHolder(private val gestureTextView: TextView) : RecyclerView.ViewHolder(
        gestureTextView
    ) {
        fun bind(item: GestureViewItem?) {
            val text = gestureTextView
                .context
                .getString(R.string.gesture_item_body, item!!.gesture.gestureType().description)
            gestureTextView.text = text
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<GestureViewItem>() {
        override fun areItemsTheSame(
            oldItem: GestureViewItem, newItem: GestureViewItem
        ): Boolean {
            return oldItem.id == newItem.id
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(
            oldItem: GestureViewItem, newItem: GestureViewItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}