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

import androidx.fragment.app.FragmentManager
import com.google.android.jacquard.sample.R
import com.google.android.jacquard.sample.dialog.DefaultDialog.DefaultDialogBuilder
import com.google.android.jacquard.sample.dialog.DefaultDialog.DefaultDialogButtonClickListener

/** Util class for showing dialog. */
object DFUUtil {
    fun showAlmostReadyDialog(
        fragmentManager: FragmentManager,
        positiveButtonClick: DefaultDialogButtonClickListener
    ) {
        DefaultDialogBuilder()
            .setTitle(R.string.almost_ready_title)
            .setSubtitle(R.string.almost_ready_subtitle)
            .setPositiveButtonTitleId(R.string.almost_ready_update_dialog_positive_btn)
            .setShowNegativeButton(false)
            .setShowPositiveButton(true)
            .setCancellable(true)
            .setShowSubtitle(true)
            .setShowProgress(false)
            .setPositiveButtonClick(positiveButtonClick)
            .build()
            .show(fragmentManager, /* tag= */ null)
    }

    fun showUpdateCompleteDialog(fragmentManager: FragmentManager) {
        DefaultDialogBuilder()
            .setTitle(R.string.update_complete_dialog_title)
            .setSubtitle(R.string.update_complete_dialog_subtitle)
            .setPositiveButtonTitleId(R.string.update_complete_dialog_positive_btn)
            .setShowNegativeButton(false)
            .setShowPositiveButton(true)
            .setCancellable(true)
            .setShowSubtitle(true)
            .setShowProgress(false)
            .build()
            .show(fragmentManager,  /* tag= */ null)
    }

    fun showErrorDialog(
        title: Int, subTitle: Int, positiveBtn: Int, fragmentManager: FragmentManager
    ) {
        DefaultDialogBuilder()
            .setTitle(title)
            .setSubtitle(subTitle)
            .setPositiveButtonTitleId(positiveBtn)
            .setShowNegativeButton(false)
            .setShowPositiveButton(true)
            .setCancellable(true)
            .setShowSubtitle(true)
            .setShowProgress(false)
            .build()
            .show(fragmentManager,  /* tag= */ null)
    }

    fun showApplyFirmwareProgress(
        fragmentManager: FragmentManager, listener: DefaultDialogButtonClickListener? = null
    ): DefaultDialog {
        val defaultDialogBuilder = DefaultDialogBuilder()
            .setTitle(R.string.apply_update_dialog_title)
            .setSubtitle(R.string.apply_update_dialog_subtitle)
            .setShowNegativeButton(false)
            .setCancellable(false)
            .setShowSubtitle(true)
            .setShowProgress(true)

        listener?.let {
            defaultDialogBuilder.setPositiveButtonClick(it)
                .setShowPositiveButton(true)
                .setPositiveButtonTitleId(R.string.ok)
        }
        return defaultDialogBuilder.build().apply {
            show(fragmentManager, /* tag= */ null)
        }
    }

    fun showMandatoryDialog(
        fragmentManager: FragmentManager, listener: DefaultDialogButtonClickListener
    ) {
        DefaultDialogBuilder()
            .setTitle(R.string.optional_update_dialog_title)
            .setSubtitle(R.string.optional_update_dialog_subtitle)
            .setPositiveButtonTitleId(R.string.optional_update_dialog_positive_btn)
            .setShowNegativeButton(false)
            .setShowPositiveButton(true)
            .setCancellable(true)
            .setShowSubtitle(true)
            .setShowProgress(false)
            .setPositiveButtonClick(listener)
            .build()
            .show(fragmentManager,  /* tag= */ null)
    }

    fun showOptionalDialog(
        fragmentManager: FragmentManager, listener: DefaultDialogButtonClickListener
    ) {
        DefaultDialogBuilder()
            .setTitle(R.string.optional_update_dialog_title)
            .setSubtitle(R.string.optional_update_dialog_subtitle)
            .setPositiveButtonTitleId(R.string.optional_update_dialog_positive_btn)
            .setNegativeButtonTitle(R.string.optional_update_dialog_negative_btn)
            .setShowNegativeButton(true)
            .setShowPositiveButton(true)
            .setCancellable(true)
            .setShowSubtitle(true)
            .setShowProgress(false)
            .setPositiveButtonClick(listener)
            .build()
            .show(fragmentManager,  /* tag= */ null)
    }

    fun showNoDFUDialog(fragmentManager: FragmentManager) {
        DefaultDialogBuilder()
            .setTitle(R.string.no_update_available_title)
            .setPositiveButtonTitleId(R.string.no_update_available_positive_btn)
            .setShowNegativeButton(false)
            .setShowPositiveButton(true)
            .setCancellable(true)
            .setShowSubtitle(false)
            .setShowProgress(false)
            .build()
            .show(fragmentManager,  /* tag= */ null)
    }
}