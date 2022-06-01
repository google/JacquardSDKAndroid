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
 */

package com.google.android.jacquard.sample.haptics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.jacquard.sample.R

class HapticsAdapter(private val itemClickListener: ItemClickListener) :
    RecyclerView.Adapter<HapticsAdapter.HapticsViewHolder>() {

    /** Callback for click events.  */
    interface ItemClickListener {
        /** Callback for the a haptics.  */
        fun onItemClick(patternItem: HapticPatternType)
    }

    companion object {
        private val HAPTICS = arrayOf(
            HapticPatternType.INSERT_PATTERN, HapticPatternType.GESTURE_PATTERN,
            HapticPatternType.NOTIFICATION_PATTERN, HapticPatternType.ERROR_PATTERN,
            HapticPatternType.ALERT_PATTERN
        )
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HapticsViewHolder {
        return HapticsViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.haptic_item, parent, false), itemClickListener
        )
    }

    override fun getItemCount(): Int {
        return HAPTICS.size
    }

    override fun onBindViewHolder(holder: HapticsViewHolder, position: Int) {
        holder.bindView(HAPTICS[position].description)
    }

    /** A layout place holder for haptic item.  */
    class HapticsViewHolder(
        private val view: View,
        private val itemClickListener: ItemClickListener
    ) : RecyclerView.ViewHolder(view), View.OnClickListener {
        init {
            view.setOnClickListener(this)
        }

        fun bindView(haptic: String) {
            view.findViewById<TextView>(R.id.haptic_pattern_tv).text = haptic
        }

        override fun onClick(v: View) {
            itemClickListener.onItemClick(HAPTICS.get(adapterPosition))
        }
    }
}