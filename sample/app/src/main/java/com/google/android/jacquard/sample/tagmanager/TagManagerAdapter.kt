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
package com.google.android.jacquard.sample.tagmanager

import android.annotation.SuppressLint
import android.content.IntentSender
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.jacquard.sample.KnownTag
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.scan.AdapterItem
import com.google.android.jacquard.sdk.rx.Fn
import com.google.android.jacquard.sdk.rx.Signal

/**
 * A adapter for tag manager fragment [TagManagerFragment].
 */
class TagManagerAdapter(
  private val viewModel: TagManagerViewModel,
  private val senderHandler: Fn<IntentSender, Signal<Boolean>>
) : ListAdapter<AdapterItem, TagManagerAdapter.AdapterItemViewHolder>(DiffCallback()) {

  /**
   * Callback for click events.
   */
  internal sealed interface ItemClickListener {
    /**
     * Callback for the tag has been clicked.
     *
     * @param tag known tag
     */
    fun onItemClick(tag: KnownTag)

    /**
     * Callback for the tag has been selected.
     *
     * @param tag known tag
     */
    fun onTagSelect(tag: KnownTag, senderHandler: Fn<IntentSender, Signal<Boolean>>)
  }

  companion object {
    private var selectedTagViewHolder: TagViewHolder? = null
    private fun getView(parent: ViewGroup, @LayoutRes layoutResId: Int): View {
      val layoutInflater = LayoutInflater.from(parent.context)
      return layoutInflater.inflate(layoutResId, parent,  /* attachToRoot= */false)
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdapterItemViewHolder {
    val type = AdapterItem.Type.values()[viewType]
    if (type == AdapterItem.Type.TAG) return getTagViewHolder(parent)
    throw IllegalStateException("Unknown tag type: $type")
  }

  override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
    super.onDetachedFromRecyclerView(recyclerView)
    selectedTagViewHolder = null
  }

  override fun onBindViewHolder(holder: AdapterItemViewHolder, position: Int) {
    holder.bind(getItem(position), senderHandler)
  }

  override fun getItemViewType(position: Int): Int {
    return getItem(position).getType().ordinal
  }

  private fun getTagViewHolder(parent: ViewGroup): TagViewHolder {
    return TagViewHolder(getView(parent, R.layout.scan_list_tag), viewModel)
  }

  private class DiffCallback : DiffUtil.ItemCallback<AdapterItem>() {
    override fun areItemsTheSame(oldItem: AdapterItem, newItem: AdapterItem): Boolean {
      return if (oldItem.getType() == AdapterItem.Type.TAG && newItem.getType() == AdapterItem.Type.TAG)
        oldItem.tag().address().equals(newItem.tag().address())
      else false
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(
      oldItem: AdapterItem,
      newItem: AdapterItem
    ): Boolean {
      return oldItem == newItem
    }
  }

  sealed class AdapterItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: AdapterItem, senderHandler: Fn<IntentSender, Signal<Boolean>>)
  }

  private class TagViewHolder(itemView: View, private val viewModel: TagManagerViewModel) :
    AdapterItemViewHolder(itemView) {
    private val displayName = itemView.findViewById<TextView>(R.id.tag_name)
    private val address = itemView.findViewById<TextView>(R.id.tag_identifier)
    private val tagSelection = itemView.findViewById<ImageView>(R.id.tag_selected_icon)
    private val layout = itemView.findViewById<View>(R.id.scan_item_layout)

    override fun bind(item: AdapterItem, senderHandler: Fn<IntentSender, Signal<Boolean>>) {
      val tag = item.tag()
      displayName.text = tag.displayName()
      address.text = tag.pairingSerialNumber()
      setCurrentTagAsSelected(tag)
      tagSelection.setOnClickListener { onTagSelect(senderHandler, tag) }
      itemView.setOnClickListener { viewModel.onItemClick(tag) }
    }

    private fun onTagSelect(senderHandler: Fn<IntentSender, Signal<Boolean>>, tag: KnownTag) {
      selectedTagViewHolder?.layout?.isSelected = false
      selectedTagViewHolder = this@TagViewHolder
      layout.isSelected = true
      viewModel.onTagSelect(tag, senderHandler)
    }

    private fun setCurrentTagAsSelected(tag: KnownTag) {
      if (viewModel.isCurrentTag(tag)) {
        selectedTagViewHolder = this@TagViewHolder
        layout.isSelected = true
      }
    }
  }
}