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

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.jacquard.sample.MainActivity
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.fragment.extensions.getViewModel
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

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    adapter = TagManagerAdapter(viewModel) { intentSender ->
      (requireActivity() as MainActivity)
        .startForResult(intentSender, MainActivity.COMPANION_DEVICE_REQUEST)
        .map { result -> result.resultCode() == Activity.RESULT_OK }
    }
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
  }

  override fun onDestroyView() {
    subscription.unsubscribe()
    super.onDestroyView()
  }

  private fun initView(view: View) {
    view.findViewById<RecyclerView>(R.id.tag_manager_recyclerview).adapter = adapter
    adapter.submitList(viewModel.getKnownTagsSection())
  }

  private fun initToolbar(view: View) {
    view.findViewById<Toolbar>(R.id.toolbar)
      .setNavigationOnClickListener { viewModel.backArrowClick() }
  }

  private fun initScanningFlow(view: View) {
    view.findViewById<ImageView>(R.id.initiate_scan_iv)
      .setOnClickListener { viewModel.initiateScan() }
  }

  private fun onState(navigation: State) {
    if (navigation.getType() == State.Type.ACTIVE)
      Util.showSnackBar(view, getString(R.string.selected_as_current_tag, navigation.active()))
  }
}