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
package com.google.android.jacquard.sample.imu

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.jacquard.sample.R
import com.google.protos.atap.jacquard.core.Jacquard.ImuSample


/** View holder for recyclerview adapter [ImuDataListAdapter]. */
class ImuDataListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val imuText: TextView = view.findViewById(R.id.imu_sample_textview)
}

/** Recyclerview adapter for fragment [ImuSamplesListFragment]. */
class ImuDataListAdapter(list: List<ImuSample>, val resources: Resources) :
    RecyclerView.Adapter<ImuDataListViewHolder>() {

    private val imuSampleList = mutableListOf<ImuSample>()

    init {
        imuSampleList.addAll(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImuDataListViewHolder {
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.imu_sample_list_item, parent, false)
            .let { ImuDataListViewHolder(it) }
    }

    override fun onBindViewHolder(holder: ImuDataListViewHolder, position: Int) {
        imuSampleList[position].apply {
            holder.imuText.text = String.format(
                resources.getString(R.string.imu_text),
                accX, accY, accZ, gyroPitch, gyroRoll, gyroYaw
            )
        }
    }

    override fun getItemCount(): Int {
        return imuSampleList.size
    }

    /**
     * Add items to Recyclerview adapter.
     * This will refresh recyclerview.
     * @param imuSampleList List of [ImuSample] to add into recyclerview adapter.
     */
    fun add(imuSampleList: List<ImuSample>) {
        this.imuSampleList.addAll(imuSampleList)
        notifyDataSetChanged()
    }
}