/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.google.android.jacquard.sample.ledpattern

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.jacquard.sample.R

/** Recycler view adapter for [LedPatternFragment]. */
class LedPatternAdapter(
    private val patternItemList: MutableList<LedPatternItem>,
    private val itemClickListener: ItemClickListener
) : RecyclerView.Adapter<LedPatternAdapter.LedPatternViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LedPatternViewHolder {
        return LedPatternViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.led_pattern_item, parent, false), itemClickListener
        )
    }

    override fun onBindViewHolder(holder: LedPatternViewHolder, position: Int) {
        holder.bindView(patternItemList[position])
    }

    override fun getItemCount(): Int {
        return patternItemList.size
    }

    /** Callback for click events.  */
    interface ItemClickListener {
        /** Callback for the a led pattern that has been selected to play.  */
        fun onItemClick(patternItem: LedPatternItem)
    }

    class LedPatternViewHolder(itemView: View, val itemClickListener: ItemClickListener) :
        RecyclerView.ViewHolder(itemView) {
        private val patternName = itemView.findViewById<TextView>(R.id.txtPattern)
        private val patternIcon = itemView.findViewById<ImageView>(R.id.imgLedPattern)

        fun bindView(ledPatternItem: LedPatternItem) {
            itemView.setOnClickListener {
                itemClickListener.onItemClick(ledPatternItem)
            }
            patternIcon.setImageResource(ledPatternItem.icon)
            patternName.text = ledPatternItem.text
        }
    }
}