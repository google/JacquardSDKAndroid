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
package com.google.android.jacquard.sample.scan

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.jacquard.sample.KnownTag
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.scan.AdapterItem.Type
import com.google.android.jacquard.sample.utilities.Util
import com.google.android.jacquard.sdk.rx.Signal

/** Adapter for displaying [AdapterItem].  */
class ScanAdapter(private val itemClickListener: ItemClickListener) :
  ListAdapter<AdapterItem, ScanAdapter.AdapterItemViewHolder>(DiffCallback()) {
  private var previousSelectedItem: AdapterItem? = null

  override fun onViewDetachedFromWindow(holder: AdapterItemViewHolder) {
    super.onViewDetachedFromWindow(holder)
    if (holder is TagViewHolder) {
      holder.unsubscribe()
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterItemViewHolder {
    return when (Type.values()[viewType]) {
      Type.SECTION_HEADER -> getSectionViewHolder(parent)
      Type.TAG -> getTagViewHolder(parent)
    }
  }

  override fun onBindViewHolder(holder: AdapterItemViewHolder, position: Int) {
    holder.bind(getItem(position))
  }

  override fun getItemViewType(position: Int): Int {
    return getItem(position).getType().ordinal
  }

  /** Callback for click events.  */
  interface ItemClickListener {
    /** Callback for the a tag has been selected.  */
    fun onItemClick(tag: KnownTag)
  }

  private class DiffCallback : DiffUtil.ItemCallback<AdapterItem>() {
    override fun areItemsTheSame(oldItem: AdapterItem, newItem: AdapterItem): Boolean {
      val oldItemType = oldItem.getType()
      val newItemType = newItem.getType()
      return if (oldItemType === Type.TAG && newItem.getType() === Type.TAG) {
        oldItem.tag().address() == newItem.tag().address()
      } else if (oldItemType === Type.SECTION_HEADER && newItemType === Type.SECTION_HEADER) {
        oldItem.sectionHeader() == newItem.sectionHeader()
      } else false
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: AdapterItem, newItem: AdapterItem): Boolean {
      return oldItem == newItem
    }
  }

  private class SectionHeaderViewHolder(itemView: View) : AdapterItemViewHolder(itemView) {
    private val headerTitle = itemView.findViewById<TextView>(R.id.header_title)

    override fun bind(item: AdapterItem) {
      headerTitle.text = itemView.context.getString(item.sectionHeader())
    }
  }

  private inner class TagViewHolder(itemView: View, val itemClickListener: ItemClickListener) :
    AdapterItemViewHolder(itemView) {
    private val displayName = itemView.findViewById<TextView>(R.id.tag_name)
    private val address = itemView.findViewById<TextView>(R.id.tag_identifier)
    private val layout = itemView.findViewById<View>(R.id.scan_item_layout)
    private val tagRSSIValue = itemView.findViewById<TextView>(R.id.tag_rssi_value)
    private var subscription: Signal.Subscription? = null

    override fun bind(item: AdapterItem) {
      val tag = item.tag()
      displayName.text = tag.displayName()
      address.text = tag.pairingSerialNumber()
      // tag may not be connected.
      subscription = tag.rssiSignal()?.onNext { value: Int ->
        itemView.context.apply {
          tagRSSIValue.apply {
            text = getString(R.string.scan_page_rssi_info, value.toString())
            setTextColor(getColor(Util.getRSSIColor(value)))
          }
        }
      }

      layout.isSelected = item.isItemSelected
      itemView.setOnClickListener {
        if (!item.isItemSelected) {
          item.isItemSelected = true
          previousSelectedItem?.isItemSelected = false
          previousSelectedItem = item
          itemClickListener.onItemClick(tag)
        }
      }
    }

    fun unsubscribe() {
      subscription?.unsubscribe()
    }
  }

  sealed class AdapterItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: AdapterItem)
  }

  private fun getSectionViewHolder(parent: ViewGroup): SectionHeaderViewHolder {
    return SectionHeaderViewHolder(getView(parent, R.layout.scan_list_section_header))
  }

  private fun getTagViewHolder(parent: ViewGroup): TagViewHolder {
    return TagViewHolder(getView(parent, R.layout.scan_list_tag), itemClickListener)
  }

  private fun getView(parent: ViewGroup, @LayoutRes layoutResId: Int): View {
    return LayoutInflater.from(parent.context).inflate(layoutResId, parent, false)
  }
}