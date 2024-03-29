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
package com.google.android.jacquard.sample.tagmanager

import android.annotation.SuppressLint
import android.content.res.Resources
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
import com.google.android.jacquard.sdk.dfu.FirmwareUpdateState
import com.google.android.jacquard.sdk.dfu.execption.InsufficientBatteryException
import com.google.android.jacquard.sdk.dfu.execption.UpdatedFirmwareNotFoundException
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.model.JacquardError
import com.google.android.jacquard.sdk.rx.Signal
import java.util.concurrent.TimeoutException

/**
 * A adapter for tag manager fragment [TagManagerFragment].
 */
class TagManagerAdapter(
    private val viewModel: TagManagerViewModel,
    private val resources: Resources
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
        fun onTagSelect(tag: KnownTag)
    }

    companion object {
        private val TAG = TagManagerAdapter::class.java.simpleName
        private var selectedTagViewHolder: TagViewHolder? = null
        private fun getView(parent: ViewGroup, @LayoutRes layoutResId: Int): View {
            val layoutInflater = LayoutInflater.from(parent.context)
            return layoutInflater.inflate(layoutResId, parent,  /* attachToRoot= */false)
        }
    }

    private val listSubscription = mutableListOf<Signal.Subscription>()

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
        listSubscription.add(holder.bind(getItem(position)))
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).getType().ordinal
    }

    fun unSubscribe() {
        for (subscription in listSubscription) {
            subscription.unsubscribe()
        }
    }

    private fun getTagViewHolder(parent: ViewGroup): TagViewHolder {
        return TagViewHolder(
            getView(parent, R.layout.scan_list_tag), viewModel,
            resources
        )
    }

    private class DiffCallback : DiffUtil.ItemCallback<AdapterItem>() {
        override fun areItemsTheSame(oldItem: AdapterItem, newItem: AdapterItem): Boolean {
            return if (oldItem.getType() == AdapterItem.Type.TAG && newItem.getType() == AdapterItem.Type.TAG)
                oldItem.tag().address() == newItem.tag().address()
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
        abstract fun bind(item: AdapterItem): Signal.Subscription
    }

    private class TagViewHolder(
        itemView: View,
        private val viewModel: TagManagerViewModel,
        private val resources: Resources
    ) :
        AdapterItemViewHolder(itemView) {
        private val displayName = itemView.findViewById<TextView>(R.id.tag_name)
        private val address = itemView.findViewById<TextView>(R.id.tag_identifier)
        private val dfuState = itemView.findViewById<TextView>(R.id.tag_state)
        private val tagSelection = itemView.findViewById<ImageView>(R.id.tag_selected_icon)
        private val layout = itemView.findViewById<View>(R.id.scan_item_layout)

        override fun bind(item: AdapterItem): Signal.Subscription {
            val tag = item.tag()
            displayName.text = tag.displayName()
            address.text = tag.pairingSerialNumber
            setCurrentTagAsSelected(tag)
            tagSelection.setOnClickListener { onTagSelect(tag) }
            itemView.setOnClickListener { viewModel.onItemClick(tag) }
            return viewModel.fmUpdateStates[tag.address()]!!.onNext { state ->
                PrintLogger.d(TAG, "state: $state")
                when (state.getType()) {
                    TagManagerViewModel.State.Type.CHECK_FIRMWARE -> {
                        layout.isSelected = false
                        dfuState.isActivated = true
                        dfuState.text = resources.getText(R.string.tag_state_checking_for_update)
                        tagSelection.isClickable = false
                        itemView.isClickable = false
                        tagSelection.visibility = View.GONE
                    }
                    TagManagerViewModel.State.Type.NO_UPDATE ->
                        dfuState.text = resources.getText(R.string.no_update_available_title)
                    TagManagerViewModel.State.Type.IDLE -> {
                        PrintLogger.d(TAG, "IDLE")
                        layout.isSelected = false
                        dfuState.isActivated = true
                        dfuState.text = ""
                        tagSelection.isClickable = true
                        itemView.isClickable = true
                        tagSelection.visibility = View.VISIBLE
                        setCurrentTagAsSelected(tag)
                    }
                    TagManagerViewModel.State.Type.DISCONNECTED -> {
                        PrintLogger.d(TAG, "Tag Disconnected")
                    }
                    TagManagerViewModel.State.Type.FIRMWARE_STATE -> {

                        when (state.firmwareState().type) {
                            FirmwareUpdateState.Type.TRANSFER_PROGRESS -> {
                                dfuState.text = resources.getString(
                                    R.string.tag_state_updating,
                                    state.firmwareState().transferProgress().toString()
                                )
                                PrintLogger.d(
                                    TAG,
                                    "Updating: ${
                                        state.firmwareState().transferProgress()
                                    }"
                                )
                            }

                            FirmwareUpdateState.Type.EXECUTING -> {
                                PrintLogger.d(TAG, "EXECUTING")
                                dfuState.text = resources.getText(R.string.tag_state_executing)
                            }

                            FirmwareUpdateState.Type.COMPLETED -> {
                                PrintLogger.d(TAG, "COMPLETED")
                                dfuState.text = resources.getText(R.string.tag_state_completed)
                            }

                            FirmwareUpdateState.Type.STOPPED -> {
                                PrintLogger.d(TAG, "STOPPED")
                                disabledLineItemUI()
                                dfuState.text =
                                    resources.getText(R.string.update_stopped)
                            }

                            FirmwareUpdateState.Type.ERROR -> {
                                PrintLogger.d(
                                    TAG,
                                    "Error: ${state.firmwareState().error().message}"
                                )
                                // This is to reset selected line item in case Tag is disconnected
                                // and error lands here directly instead of checkForFirmware.
                                disabledLineItemUI()
                                when (state.firmwareState().error()) {
                                    is InsufficientBatteryException -> {
                                        dfuState.text =
                                            resources.getText(R.string.tag_state_low_battery)
                                    }
                                    is JacquardError, is TimeoutException -> {
                                        dfuState.text =
                                            resources.getText(R.string.tag_disconnected)
                                    }
                                    is UpdatedFirmwareNotFoundException -> {
                                        dfuState.text =
                                            resources.getText(R.string.no_update_available_title)
                                    }
                                    else -> {
                                        dfuState.text = resources.getText(R.string.generic_error)
                                    }
                                }
                            }
                            else -> {
                                // else block to avoid unused states
                            }
                        }
                    }

                    else -> {
                        // UI will be handled here in future.
                    }

                }
            }
        }

        private fun onTagSelect(tag: KnownTag) {
            selectedTagViewHolder?.layout?.isSelected = false
            selectedTagViewHolder = this@TagViewHolder
            layout.isSelected = true
            viewModel.onTagSelect(tag)
        }

        private fun setCurrentTagAsSelected(tag: KnownTag) {
            if (viewModel.isCurrentTag(tag)) {
                selectedTagViewHolder = this@TagViewHolder
                layout.isSelected = true
            }
        }

        private fun disabledLineItemUI() {
            layout.isSelected = false
            tagSelection.isClickable = false
            itemView.isClickable = false
            tagSelection.visibility = View.GONE

            dfuState.isActivated = false
        }
    }
}