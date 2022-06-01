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
package com.google.android.jacquard.sample.places

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.recyclerview.widget.RecyclerView
import com.google.android.jacquard.sample.R

/**
 * Recycler view adapter for [PlacesConfigFragment].
 */
class GestureAdapter(
    private val gestureItems: List<GestureItem>,
    private val itemClickListener: ItemClickListener
) :
    RecyclerView.Adapter<GestureAdapter.GestureViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GestureViewHolder {
        return GestureViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.gesture_item, parent, false)
        )
    }

    override fun onBindViewHolder(viewHolder: GestureViewHolder, position: Int) {
        viewHolder.bindView(position)
    }

    override fun getItemCount(): Int {
        return gestureItems.size
    }

    /**
     * Callback for click events.
     */
    interface ItemClickListener {
        /**
         * Callback for the a gesture that has been selected.
         */
        fun onItemClick(gestureItem: GestureItem)
    }

    inner class GestureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chooseGesture: RadioButton = itemView.findViewById(R.id.gesture)
        private fun updateGestureItems(selectedGestureId: Int) {
            for (gestureItem in gestureItems) {
                gestureItem.isItemSelected = gestureItem.id == selectedGestureId
            }
        }

        fun bindView(position: Int) {
            val gestureItem = gestureItems[position]
            chooseGesture.isChecked = gestureItem.isItemSelected
            chooseGesture.text = gestureItem.gesture
        }

        init {
            val clickListener = View.OnClickListener { _: View ->
                updateGestureItems(gestureItems[adapterPosition].id)
                notifyItemRangeChanged(0, gestureItems.size)
                itemClickListener.onItemClick(gestureItems[adapterPosition])
            }
            chooseGesture.setOnClickListener(clickListener)
            itemView.setOnClickListener(clickListener)
        }
    }
}