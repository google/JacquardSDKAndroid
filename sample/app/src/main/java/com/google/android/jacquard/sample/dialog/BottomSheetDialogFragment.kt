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

package com.google.android.jacquard.sample.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.jacquard.sample.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.*

/**
 * Fragment for showing bottom sheet dialog.
 */
class BottomSheetDialogFragment : DialogFragment() {
    /**
     * listener for bottom sheet dialog item click
     */
    interface BottomSheetMenuListener {
        fun onMenuItemClicked(id: Int)
    }

    companion object {
        fun newInstance(): BottomSheetDialogFragment {
            return BottomSheetDialogFragment()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        dialog.setContentView(createDialogView(dialog, arguments))
        return dialog
    }

    override fun getTheme(): Int {
        return R.style.BaseBottomSheetDialog
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        dialog.setContentView(createDialogView(dialog, arguments))
    }

    private fun createDialogView(dialog: Dialog, arguments: Bundle?): View {
        val containerView = LayoutInflater.from(requireContext())
            .inflate(R.layout.container_bottom_sheet, null, false)
        val bottomSheetDialog = containerView.findViewById<LinearLayout>(R.id.bottom_sheet_dialog)
        arguments?.let { it ->
            val sheetItems: ArrayList<BottomSheetItem>? = it.getParcelableArrayList("items")
            sheetItems?.let {
                for (i in it.indices) {
                    bottomSheetDialog.addView(
                        addItemToContainer(dialog.context, it[i], i == it.size - 1)
                    )
                }
            }
        }
        return bottomSheetDialog
    }

    private fun addItemToContainer(
        context: Context,
        sheetItem: BottomSheetItem,
        isLastItem: Boolean
    ): View {
        return LayoutInflater.from(requireContext())
            .inflate(R.layout.item_bottom_sheet, null, false)
            .let {
                it.findViewById<TextView>(R.id.lbl_bottom_sheet_item).let { lblBottomSheetItem ->
                    if (isLastItem) {
                        lblBottomSheetItem.setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.grey_600
                            )
                        )
                    }
                    lblBottomSheetItem.text = sheetItem.name
                    lblBottomSheetItem.tag = sheetItem.id
                    lblBottomSheetItem.setOnClickListener {
                        dismiss()
                        sheetItem.bottomSheetMenuListener?.onMenuItemClicked(it.tag as Int)
                    }
                    lblBottomSheetItem
                }
            }
    }
}