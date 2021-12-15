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
package com.google.android.jacquard.sample.renametag

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.jacquard.sample.ConnectivityManager
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.fragment.extensions.getViewModel
import com.google.android.jacquard.sample.utilities.Util
import com.google.android.jacquard.sdk.log.PrintLogger
import com.google.android.jacquard.sdk.rx.Signal.Subscription

/** A fragment for renaming the tag.  */
class RenameTagFragment : Fragment() {

  companion object {
    private val TAG = RenameTagFragment::class.java.simpleName
  }

  private val viewModel by lazy { getViewModel<RenameTagViewModel>() }
  private val subscriptions: MutableList<Subscription> = mutableListOf()
  private lateinit var tagNameTv: TextView
  private lateinit var renameEditLayout: View
  private lateinit var tagNameLayout: View
  private lateinit var parentView: View

  private val globalLayoutListener: OnGlobalLayoutListener = object : OnGlobalLayoutListener {
    private val defaultKeyboardHeightDP = 100
    private val estimatedKeyboardDP = defaultKeyboardHeightDP + 48
    private val rect = Rect()
    private var alreadyOpen = false
    override fun onGlobalLayout() {
      parentView.getWindowVisibleDisplayFrame(rect)
      if (isKeyBoardShown(rect, estimatedKeyboardDP.toFloat()) == alreadyOpen) {
        return
      }
      alreadyOpen = !alreadyOpen
      onKeyBoardVisibilityChanged(alreadyOpen)
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_rename_tag, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initToolbar(view)
    initViews(view)
    setKeyboardVisibility(view)
    viewModel.initCurrentTag()
    initData()
    subscribeEvents()
  }

  override fun onDestroyView() {
    parentView.viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
    for (subscription in subscriptions) {
      subscription.unsubscribe()
    }
    subscriptions.clear()
    super.onDestroyView()
  }

  private fun isKeyBoardShown(rect: Rect, estimatedKeyboardDP: Float): Boolean {
    val estimatedKeyboardHeight = TypedValue
      .applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, estimatedKeyboardDP,
        parentView.resources.displayMetrics
      ).toInt()
    val heightDiff = parentView.rootView.height - (rect.bottom - rect.top)
    return heightDiff >= estimatedKeyboardHeight
  }

  private fun initViews(view: View) {
    val tagNameEdt = view.findViewById<EditText>(R.id.tag_name_edt)
    tagNameTv = view.findViewById(R.id.tag_name)
    renameEditLayout = view.findViewById(R.id.rename_edit_layout)
    tagNameLayout = view.findViewById(R.id.tag_name_layout)
    view.findViewById<View>(R.id.clear_icon)
      .setOnClickListener { tagNameEdt.text.clear() }
    view.findViewById<View>(R.id.rename_tag).setOnClickListener {
      tagNameEdt.setText(viewModel.tagName)
      renameEditLayout.visibility = View.VISIBLE
      tagNameLayout.visibility = View.GONE
      tagNameEdt.requestFocus()
      val imm =
        requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
      imm.showSoftInput(tagNameEdt, InputMethodManager.SHOW_IMPLICIT)
    }
    tagNameEdt.setOnEditorActionListener { _, actionId, _ ->
      if (actionId != EditorInfo.IME_ACTION_DONE) {
        return@setOnEditorActionListener false
      }

      if (TextUtils.isEmpty(tagNameEdt.text)) {
        return@setOnEditorActionListener true
      }

      Util.hideSoftKeyboard(activity)
      tagNameLayout.visibility = View.VISIBLE
      renameEditLayout.visibility = View.GONE
      subscriptions.add(viewModel
                          .updateTagName(tagNameEdt.text.toString())
                          .observe({ updatedTagName: String ->
                                     tagNameTv.text = updatedTagName
                                     Util.showSnackBar(
                                       getView(),
                                       "$updatedTagName name updated successfully"
                                     )
                                     viewModel.updateKnownDevices(updatedTagName)
                                   }) { error: Throwable? ->
                            if (error == null) {
                              return@observe
                            }
                            PrintLogger.e(TAG, error.message)
                            showSnackBar(error.message)
                          })
      return@setOnEditorActionListener true
    }
  }

  private fun initData() {
    tagNameTv.text = viewModel.tagName
  }

  private fun onKeyBoardVisibilityChanged(visible: Boolean) {
    if (!visible) {
      renameEditLayout.visibility = View.GONE
      tagNameLayout.visibility = View.VISIBLE
    }
  }

  private fun setKeyboardVisibility(view: View) {
    parentView = view.rootView
    parentView.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
  }

  private fun initToolbar(root: View) {
    root.findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener {
      Util.hideSoftKeyboard(activity)
      viewModel.backArrowClick()
    }
  }

  private fun showSnackBar(message: String?) {
    Util.showSnackBar(view, message)
  }

  private fun subscribeEvents() {
    subscriptions.add(viewModel.connectivityEvents.onNext { events: ConnectivityManager.Events ->
      onEvents(events)
    })
  }

  private fun onEvents(events: ConnectivityManager.Events) {
    when (events) {
      ConnectivityManager.Events.TAG_DISCONNECTED -> showSnackBar(getString(R.string.tag_not_connected))
      ConnectivityManager.Events.TAG_DETACHED -> showSnackBar(getString(R.string.gear_detached))
      else -> PrintLogger.d(TAG, "Event$events")
    }
  }
}