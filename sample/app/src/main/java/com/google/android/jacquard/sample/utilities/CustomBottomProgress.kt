/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sample.utilities

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.jacquard.sample.R

class CustomBottomProgress @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var progressBar: ProgressBar
    private lateinit var textProgress: TextView

    fun setProgress(progress: Int) {
        progressBar.progress = progress
        textProgress.text = String.format("%d %s", progress, "%")
    }

    init {
        initView()
    }

    private fun initView() {
        val view = inflate(context, R.layout.item_dfu_progress, this)
        progressBar = view.findViewById(R.id.progressBar)
        textProgress = view.findViewById(R.id.txtDownloadingPercentage)
    }
}