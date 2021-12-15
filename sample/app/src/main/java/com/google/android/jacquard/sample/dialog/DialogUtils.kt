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

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import com.google.android.jacquard.sample.MainActivity
import com.google.android.jacquard.sample.dialog.DefaultDialog.DefaultDialogBuilder
import com.google.android.jacquard.sample.dialog.DefaultDialog.DefaultDialogButtonClickListener
import java.util.*

/**
 * Utility class for dialogs.
 */
class DialogUtils {

    companion object {
        /**
         * Shows bottom sheet dialog.
         */
        @JvmStatic
        fun showBottomOptions(bottomSheetItems: ArrayList<BottomSheetItem?>, activity: Activity) {
            val bottomSheetDialogFragment: BottomSheetDialogFragment =
                BottomSheetDialogFragment.newInstance()
            val bundle = Bundle()
            bundle.putParcelableArrayList("items", bottomSheetItems)
            bottomSheetDialogFragment.arguments = bundle
            bottomSheetDialogFragment.show(
                (activity as MainActivity).supportFragmentManager, bottomSheetDialogFragment.tag
            )
        }

        /**
         * Shows confirmation dialog.
         */
        @JvmStatic
        fun showDialog(
            title: Int, subtitle: Int, positiveButton: Int, negativeButton: Int,
            clickListener: DefaultDialogButtonClickListener, fragmentManager: FragmentManager?
        ) {
            DefaultDialogBuilder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setPositiveButtonTitleId(positiveButton)
                .setNegativeButtonTitle(negativeButton)
                .setShowNegativeButton(true)
                .setShowPositiveButton(true)
                .setCancellable(false)
                .setShowSubtitle(true)
                .setShowProgress(false)
                .setPositiveButtonClick(clickListener).build()
                .show(fragmentManager!!,  /* tag= */null)
        }
    }
}