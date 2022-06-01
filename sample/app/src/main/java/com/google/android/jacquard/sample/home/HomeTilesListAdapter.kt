/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sample.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.jacquard.sample.R

/** Adapter for recyclerview in [HomeFragment].  */
class HomeTilesListAdapter(
    private val context: Context,
    private val itemClick: ItemClickListener<HomeTileModel>
) : ListAdapter<HomeTileModel, HomeTilesListAdapter.AbstractViewHolder>(
    DIFF_CALLBACK
) {
    /** Creating ViewHolder for recyclerview in [HomeFragment].  */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbstractViewHolder {
        when (HomeTileModel.Type.values()[viewType]) {
            HomeTileModel.Type.TILE_API -> return TileTypeApiViewHolder(
                context,
                LayoutInflater.from(context)
                    .inflate(
                        R.layout.item_product_tile_api,  /* root= */null,  /* attachToRoot= */false
                    ),
                itemClick,
                R.drawable.shape_tile_disabled,
                R.drawable.shape_tile_enabled,
                R.color.home_tile_semi_transparent_black,
                R.color.black
            )
            HomeTileModel.Type.TILE_SAMPLE_USE_CASE -> return TileTypeSampleUseCaseViewHolder(
                context,
                LayoutInflater.from(context)
                    .inflate(
                        R.layout.item_product_tile_sample_use_case,  /* root= */
                        null,  /* attachToRoot= */
                        false
                    ),
                itemClick,
                R.drawable.shape_tile_disabled,
                R.drawable.shape_tile_enabled,
                R.color.home_tile_semi_transparent_black,
                R.color.black
            )
        }
        return SectionViewHolder(
            context,
            LayoutInflater.from(context)
                .inflate(
                    R.layout.item_product_section,  /* root= */
                    null,  /* attachToRoot= */
                    false
                ),
            itemClick
        )
    }

    /** Binding ViewHolder for recyclerview in [HomeFragment].  */
    override fun onBindViewHolder(holder: AbstractViewHolder, position: Int) {
        holder.populate(currentList[position]!!)
    }

    /** Returns the number of items.  */
    override fun getItemCount(): Int {
        return currentList.size
    }

    /** Returns the type of View.  */
    override fun getItemViewType(position: Int): Int {
        return currentList[position]!!.type!!.ordinal
    }

    abstract class AbstractViewHolder(
        protected var context: Context,
        itemView: View,
        protected var itemClick: ItemClickListener<HomeTileModel>,
        protected var drawableDisabled: Int,
        protected var drawableEnabled: Int,
        protected var textColorDisabled: Int,
        protected var textColorEnabled: Int
    ) : RecyclerView.ViewHolder(itemView) {
        abstract fun populate(model: HomeTileModel)
    }

    private class TileTypeApiViewHolder(
        context: Context,
        itemView: View,
        itemClick: ItemClickListener<HomeTileModel>,
        drawableDisabled: Int,
        drawableEnabled: Int,
        textColorDisabled: Int,
        textColorEnabled: Int
    ) : AbstractViewHolder(
        context,
        itemView,
        itemClick,
        drawableDisabled,
        drawableEnabled,
        textColorDisabled,
        textColorEnabled
    ) {
        private val textTitle: TextView = itemView.findViewById(R.id.tileTitle)
        private val textSubTitle: TextView = itemView.findViewById(R.id.tileSubTitle)
        override fun populate(model: HomeTileModel) {
            textTitle.text = context.getString(model.title)
            textSubTitle.text = context.getString(model.subTitle)
            if (!model.enabled) {
                itemView.setBackgroundResource(drawableDisabled)
                textTitle.setTextColor(context.getColor(textColorDisabled))
                textSubTitle.setTextColor(context.getColor(textColorDisabled))
                return
            }
            textTitle.setTextColor(context.getColor(textColorEnabled))
            textSubTitle.setTextColor(context.getColor(textColorEnabled))
            itemView.setBackgroundResource(drawableEnabled)
            itemView.setOnClickListener { itemClick.itemClick(model) }
        }

    }

    private class TileTypeSampleUseCaseViewHolder(
        context: Context,
        itemView: View,
        itemClick: ItemClickListener<HomeTileModel>,
        drawableDisabled: Int,
        drawableEnabled: Int,
        textColorDisabled: Int,
        textColorEnabled: Int
    ) : AbstractViewHolder(
        context,
        itemView,
        itemClick,
        drawableDisabled,
        drawableEnabled,
        textColorDisabled,
        textColorEnabled
    ) {
        private val textTitle: TextView = itemView.findViewById(R.id.tileTitle)
        override fun populate(model: HomeTileModel) {
            textTitle.text = context.getString(model.title)
            if (!model.enabled) {
                itemView.setBackgroundResource(drawableDisabled)
                textTitle.setTextColor(context.getColor(textColorDisabled))
                return
            }
            textTitle.setTextColor(context.getColor(textColorEnabled))
            itemView.setBackgroundResource(drawableEnabled)
            itemView.setOnClickListener { itemClick.itemClick(model) }
        }

    }

    private class SectionViewHolder(
        context: Context,
        itemView: View,
        itemClick: ItemClickListener<HomeTileModel>
    ) : AbstractViewHolder(
        context,
        itemView,
        itemClick,  /* drawableDisabled= */
        0,  /* drawableEnabled= */
        0,  /* textColorDisabled= */
        0,  /* textColorEnabled= */
        0
    ) {
        private val textTitle: TextView = itemView.findViewById(R.id.tileTitle)
        override fun populate(model: HomeTileModel) {
            textTitle.text = context.getString(model.title)
            if (!model.enabled) {
                return
            }
            itemView.setOnClickListener { itemClick.itemClick(model) }
        }

    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<HomeTileModel> =
            object : DiffUtil.ItemCallback<HomeTileModel>() {
                override fun areItemsTheSame(
                    oldItem: HomeTileModel, newItem: HomeTileModel
                ): Boolean {
                    return oldItem.title == newItem.title
                }

                override fun areContentsTheSame(
                    oldItem: HomeTileModel, newItem: HomeTileModel
                ): Boolean {
                    return oldItem.title == newItem.title && oldItem.type == newItem.type && oldItem.enabled == newItem.enabled
                }
            }
    }
}