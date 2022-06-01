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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.dialog.DefaultDialog
import com.google.android.jacquard.sample.dialog.DialogUtils
import com.google.android.jacquard.sample.fragment.extensions.getViewModel
import com.google.android.jacquard.sample.fragment.extensions.showSnackBar
import com.google.android.jacquard.sample.tagmanager.TagManagerViewModel.State
import com.google.android.jacquard.sample.utilities.Util
import com.google.android.jacquard.sdk.rx.Signal

/**
 * A fragment for tag manager screen.
 */
class TagManagerFragment : Fragment() {
    private val viewModel by lazy { getViewModel<TagManagerViewModel>() }
    private lateinit var adapter: TagManagerAdapter
    private lateinit var subscription: Signal.Subscription
    private lateinit var updateAllButton: Button

    private val doneClick: (button: View) -> Unit = {
        (it as Button)
        it.isActivated = false
        it.setText(R.string.update_all_tags)
        it.setOnClickListener(updateAllClick)
        viewModel.doneClick()
    }

    private val updateAllClick: (button: View) -> Unit = {
        DialogUtils.showDialog(
            R.string.update_all_tag_title,
            R.string.update_all_tag_subtitle,
            R.string.got_it,
            R.string.cancel,
            object : DefaultDialog.DefaultDialogButtonClickListener() {
                override fun buttonClick() {
                    (it as Button)
                    it.isActivated = true
                    it.setText(R.string.stop_update)
                    it.setOnClickListener(stopUpdateClick)
                    viewModel.updateAllTags()
                }
            },
            childFragmentManager
        )
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            onNavBackClick()
        }
    }

    private val stopUpdateClick: (v: View) -> Unit = {
        DialogUtils.showDialog(
            R.string.stop_update,
            R.string.stop_update_subtitle,
            R.string.stop_update,
            R.string.cancel,
            object : DefaultDialog.DefaultDialogButtonClickListener() {
                override fun buttonClick() {
                    (it as Button)
                    it.isActivated = false
                    it.setText(R.string.done)
                    it.setOnClickListener(doneClick)
                    viewModel.stopUpdate()
                }
            },
            childFragmentManager
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = TagManagerAdapter(
            viewModel,
            resources
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tag_manager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
        initToolbar(view)
        initScanningFlow(view)
        subscription = viewModel.stateSignal.onNext { navigation -> onState(navigation) }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )
    }

    override fun onDestroyView() {
        subscription.unsubscribe()
        adapter.unSubscribe()
        super.onDestroyView()
    }

    private fun initView(view: View) {
        view.findViewById<RecyclerView>(R.id.tag_manager_recyclerview).adapter = adapter
        adapter.submitList(viewModel.getKnownTagsSection())
        updateAllButton = view.findViewById(R.id.update_all_tags_btn)
        updateAllButton.setOnClickListener(updateAllClick)
    }

    private fun initToolbar(view: View) {
        view.findViewById<Toolbar>(R.id.toolbar)
            .setNavigationOnClickListener {
                onNavBackClick()
            }
    }

    private fun initScanningFlow(view: View) {
        view.findViewById<ImageView>(R.id.initiate_scan_iv)
            .setOnClickListener {
                if (viewModel.isFirmwareUpdateInProgress) {
                    showSnackBar("You need to stop the update to leave the page")
                    return@setOnClickListener
                }
                viewModel.initiateScan()
            }
    }

    private fun onState(navigation: State) {
        when (navigation.getType()) {
            State.Type.ACTIVE -> {
                Util.showSnackBar(
                    view,
                    getString(R.string.selected_as_current_tag, navigation.active())
                )
            }

            State.Type.IDLE -> {
                resetStopButtonToUpdate()
            }

            State.Type.UPDATE_COMPLETE -> {

                DefaultDialog.DefaultDialogBuilder()
                    .setTitle(R.string.update_complete_dialog_title)
                    .setSubtitle(R.string.update_complete_dialog_all_tags_subtitle)
                    .setPositiveButtonTitleId(R.string.got_it)
                    .setShowNegativeButton(false)
                    .setShowPositiveButton(true)
                    .setCancellable(false)
                    .setShowSubtitle(true)
                    .setShowProgress(false)
                    .setPositiveButtonClick(object : DefaultDialog.DefaultDialogButtonClickListener() {
                        override fun buttonClick() {
                            resetStopButtonToDone()
                        }
                    }).build()
                    .show(childFragmentManager,  /* tag = */null)
            }

            State.Type.DISCONNECTED -> {
                resetStopButtonToDone()
            }

            else -> { /* Rest of the states ignored. */ }
        }
    }

    private fun onNavBackClick() {
        if (viewModel.isFirmwareUpdateInProgress) {
            showSnackBar("You need to stop the update to leave the page")
            return
        }
        viewModel.backArrowClick()
    }

    private fun resetStopButtonToUpdate() {
        updateAllButton.isActivated = false
        updateAllButton.setText(R.string.update_all_tags)
        updateAllButton.setOnClickListener(updateAllClick)
    }

    private fun resetStopButtonToDone() {
        updateAllButton.isActivated = false
        updateAllButton.setText(R.string.done)
        updateAllButton.setOnClickListener(doneClick)
    }
}