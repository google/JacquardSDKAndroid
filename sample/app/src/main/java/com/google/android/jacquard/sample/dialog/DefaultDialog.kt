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

import android.app.Dialog
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.jacquard.sample.R

class DefaultDialog : DialogFragment(), View.OnClickListener {
    companion object {
        private const val EXTRA_SHOW_POSITIVE = "EXTRA_SHOW_POSITIVE"
        private const val EXTRA_SHOW_NEGATIVE = "EXTRA_SHOW_NEGATIVE"
        private const val EXTRA_SHOW_SUBTITLE = "EXTRA_SHOW_SUBTITLE"
        private const val EXTRA_SHOW_PROGRESS = "EXTRA_SHOW_PROGRESS"
        private const val EXTRA_POSITIVE_BUTTON_TEXT = "EXTRA_POSITIVE_BUTTON_TEXT"
        private const val EXTRA_NEGATIVE_BUTTON_TEXT = "EXTRA_NEGATIVE_BUTTON_TEXT"
        private const val EXTRA_SUBTITLE_TEXT = "EXTRA_SUBTITLE_TEXT"
        private const val EXTRA_SUBTITLE_TEXT_STR = "EXTRA_SUBTITLE_TEXT_STR"
        private const val EXTRA_TITLE_TEXT = "EXTRA_TITLE_TEXT"
        private const val EXTRA_IS_CANCELABLE = "EXTRA_IS_CANCELABLE"
        private const val EXTRA_POSITIVE_BUTTON_CLICK = "EXTRA_POSITIVE_BUTTON_CLICK"
        private const val EXTRA_NEGATIVE_BUTTON_CLICK = "EXTRA_NEGATIVE_BUTTON_CLICK"
    }

    private var positiveButtonClick: DefaultDialogButtonClickListener? = null
    private var negativeButtonClick: DefaultDialogButtonClickListener? = null
    private var progressBar: ProgressBar? = null
    private var progressPercentage: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return init(
            inflater.inflate(
                R.layout.fragment_custom_dialog, container,
                false
            )
        )
    }

    override fun onResume() {
        super.onResume()
        overrideDialogWidth(dialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    /**
     * Populates the percentage for apply firmware progress.
     *
     * @param percentage `int`.
     */
    fun updateProgress(percentage: Int) {
        progressBar?.progress = percentage
        progressPercentage?.text = String.format("%s %%", percentage)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.dialogPositiveButton -> {
                dismiss()
                positiveButtonClick?.buttonClick()
            }
            R.id.dialogNegativeButton -> {
                dismiss()
                negativeButtonClick?.buttonClick()
            }
        }
    }

    /** Builder for [DefaultDialog].  */
    class DefaultDialogBuilder {
        private var showNegativeButton = false
        private var showPositiveButton = false
        private var showSubtitle = false
        private var showProgress = false
        private var isCancellable = true
        private var positiveButtonTitleId = R.string.optional_update_dialog_positive_btn
        private var negativeButtonTitle = R.string.optional_update_dialog_negative_btn
        private var subtitle = R.string.empty
        private var title = R.string.optional_update_dialog_title
        private var subtitleStr: String? = null
        private var positiveButtonClick: DefaultDialogButtonClickListener? = null
        private var negativeButtonClick: DefaultDialogButtonClickListener? = null
        fun setShowNegativeButton(showNegativeButton: Boolean) =
            apply { this.showNegativeButton = showNegativeButton }

        fun setShowPositiveButton(showPositiveButton: Boolean) =
            apply { this.showPositiveButton = showPositiveButton }

        fun setShowSubtitle(showSubtitle: Boolean) = apply { this.showSubtitle = showSubtitle }

        fun setShowProgress(showProgress: Boolean) = apply { this.showProgress = showProgress }

        fun setPositiveButtonTitleId(positiveButtonTitleId: Int) =
            apply { this.positiveButtonTitleId = positiveButtonTitleId }

        fun setNegativeButtonTitle(negativeButtonTitle: Int) =
            apply { this.negativeButtonTitle = negativeButtonTitle }

        fun setSubtitle(subtitle: Int) = apply { this.subtitle = subtitle }

        fun setSubtitle(subtitleStr: String) = apply { this.subtitleStr = subtitleStr }

        fun setTitle(title: Int) = apply { this.title = title }

        fun setCancellable(cancellable: Boolean) = apply { this.isCancellable = cancellable }

        fun setPositiveButtonClick(positiveButtonClick: DefaultDialogButtonClickListener?) =
            apply { this.positiveButtonClick = positiveButtonClick }

        fun setNegativeButtonClick(negativeButtonClick: DefaultDialogButtonClickListener?) =
            apply { this.negativeButtonClick = negativeButtonClick }

        /**
         * Builds [DefaultDialog] along with required parameters.
         *
         * @return `DefaultDialog`
         */
        fun build(): DefaultDialog {
            val bundle = Bundle()
            bundle.putBoolean(EXTRA_SHOW_POSITIVE, showPositiveButton)
            bundle.putBoolean(EXTRA_SHOW_NEGATIVE, showNegativeButton)
            bundle.putBoolean(EXTRA_SHOW_SUBTITLE, showSubtitle)
            bundle.putBoolean(EXTRA_SHOW_PROGRESS, showProgress)
            bundle.putBoolean(EXTRA_IS_CANCELABLE, isCancellable)
            bundle.putInt(EXTRA_POSITIVE_BUTTON_TEXT, positiveButtonTitleId)
            bundle.putInt(EXTRA_NEGATIVE_BUTTON_TEXT, negativeButtonTitle)
            bundle.putInt(EXTRA_SUBTITLE_TEXT, subtitle)
            bundle.putString(EXTRA_SUBTITLE_TEXT_STR, subtitleStr)
            bundle.putInt(EXTRA_TITLE_TEXT, title)
            bundle.putParcelable(EXTRA_POSITIVE_BUTTON_CLICK, positiveButtonClick)
            bundle.putParcelable(EXTRA_NEGATIVE_BUTTON_CLICK, negativeButtonClick)
            val dialog = DefaultDialog()
            dialog.arguments = bundle
            return dialog
        }
    }

    private fun init(parentView: View): View {
        val textViewTitle = parentView.findViewById<TextView>(R.id.page_title)
        val textViewSubTitle = parentView.findViewById<TextView>(R.id.subTitleTxt)
        textViewSubTitle.movementMethod = LinkMovementMethod.getInstance()
        val positiveBtn = parentView.findViewById<Button>(R.id.dialogPositiveButton)
        val negativeBtn = parentView.findViewById<Button>(R.id.dialogNegativeButton)
        val progressLayout = parentView.findViewById<View>(R.id.progressLayout)
        progressBar = parentView.findViewById(R.id.progressBar)
        progressPercentage = parentView.findViewById(R.id.txtProgressPercentage)
        arguments?.let {
            val showSubTitle = it.getBoolean(EXTRA_SHOW_SUBTITLE, true)
            val showProgress = it.getBoolean(EXTRA_SHOW_PROGRESS, true)
            val showPositive = it.getBoolean(EXTRA_SHOW_POSITIVE, true)
            val showNegative = it.getBoolean(EXTRA_SHOW_NEGATIVE, true)
            val isCancelable = it.getBoolean(EXTRA_IS_CANCELABLE, true)
            val titleId = it.getInt(EXTRA_TITLE_TEXT, 0)
            val subTitleId = it.getInt(EXTRA_SUBTITLE_TEXT, 0)
            val subTitleStr =
                it.getString(EXTRA_SUBTITLE_TEXT_STR, getString(R.string.empty))
            val positiveText = it.getInt(EXTRA_POSITIVE_BUTTON_TEXT, 0)
            val negativeText = it.getInt(EXTRA_NEGATIVE_BUTTON_TEXT, 0)
            positiveButtonClick = it.getParcelable(EXTRA_POSITIVE_BUTTON_CLICK)
            negativeButtonClick = it.getParcelable(EXTRA_NEGATIVE_BUTTON_CLICK)
            setCancelable(isCancelable)
            textViewTitle.setText(titleId)
            if (subTitleId == R.string.empty) {
                textViewSubTitle.text = subTitleStr
            } else {
                textViewSubTitle.setText(subTitleId)
            }
            positiveBtn.setText(positiveText)
            negativeBtn.setText(negativeText)
            positiveBtn.setOnClickListener(this)
            negativeBtn.setOnClickListener(this)
            if (!showSubTitle) {
                textViewSubTitle.visibility = View.GONE
            }
            if (!showProgress) {
                progressLayout.visibility = View.GONE
            }
            if (!showPositive) {
                positiveBtn.visibility = View.GONE
            }
            if (!showNegative) {
                negativeBtn.visibility = View.GONE
            }
        }
        return parentView
    }

    private fun overrideDialogWidth(dialog: Dialog?) {
        val params = dialog?.window?.attributes
        params?.width = ViewGroup.LayoutParams.MATCH_PARENT
        params?.height = ViewGroup.LayoutParams.WRAP_CONTENT
        dialog?.window?.attributes = params
    }

    /** Listener for positive and negative buttons of [DefaultDialog]. */
    abstract class DefaultDialogButtonClickListener() : Parcelable {
        abstract fun buttonClick()
        override fun writeToParcel(dest: Parcel?, flags: Int) {}
        override fun describeContents(): Int = 0
    }
}