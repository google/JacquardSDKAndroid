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

package com.google.android.jacquard.sample.utilities

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import com.google.android.jacquard.sample.R
import com.google.android.material.snackbar.Snackbar

/** General utilities. */
object Util {

    const val DURATION_4_SECONDS: Int = 4000

    /** Shows the snack bar with given message. */
    @JvmStatic
    fun showSnackBar(rootView: View?, message: String?) {
        rootView?.let {
            message?.let { msg ->
                Snackbar
                    .make(it, msg, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Shows the snack bar with given message.
     *
     * @param duration How long to display the message. Can be LENGTH_SHORT, LENGTH_LONG,
     *                 LENGTH_INDEFINITE, or a custom duration in milliseconds.
     */
    @JvmStatic
    fun showSnackBar(rootView: View, message: String, duration: Int) {
        Snackbar
            .make(rootView, message, Snackbar.LENGTH_LONG).setDuration(duration).show()
    }

    /**
     * Shows the snack bar with given message and action.
     */
    @JvmStatic
    fun showSnackBar(
        rootView: View?, message: String, action: String,
        actionListener: View.OnClickListener) {
        rootView?.let {
            val snackBar = Snackbar
                .make(rootView, message, Snackbar.LENGTH_LONG).setAction(action, actionListener)
            snackBar.view.findViewById<TextView>(R.id.snackbar_action)
                .setTextColor(rootView.context.getColor(R.color.white))
            snackBar.show()
        }
    }

    /**
     * Shows the snack bar with given message and action.
     */
    @JvmStatic
    fun showSnackBar(
        rootView: View, message: String, action: String,
        actionListener: View.OnClickListener, duration: Int
    ) {
        val snackBar = Snackbar
            .make(rootView, message, duration).setAction(action, actionListener)
        snackBar.view.findViewById<TextView>(R.id.snackbar_action)
            .setTextColor(rootView.context.getColor(R.color.white))
        snackBar.show()
    }

    /** Hides the soft keyboard of the screen if visible.  */
    @JvmStatic
    fun hideSoftKeyboard(activity: Activity?) {
        activity ?: return
        val view = activity.currentFocus ?: return
        val inputManager = activity
            .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager
            .hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    /** Returns RSSI color. */
    @JvmStatic
    fun getRSSIColor(rssiValue: Int): Int {
        return when {
            rssiValue <= -66 && rssiValue >= -80 -> {
                R.color.rssi_yellow
            }
            rssiValue <= -81 -> {
                R.color.rssi_red
            }
            // default value considering values higher than 66.
            else -> R.color.rssi_green
        }
    }
}