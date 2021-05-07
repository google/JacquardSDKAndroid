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

package com.google.android.jacquard.sample.utilities;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import com.google.android.material.snackbar.Snackbar;

/** General utilities. */
public class Util {

  /** Shows the snack bar with given message. */
  public static void showSnackBar(View rootView, String message) {
    Snackbar
        .make(rootView, message, Snackbar.LENGTH_LONG).show();
  }

  /** Hides the soft keyboard of the screen if visible. */
  public static void hideSoftKeyboard(Activity activity) {
    View view = activity.getCurrentFocus();
    if (view == null) {
      return;
    }
    InputMethodManager inputManager = (InputMethodManager) activity
        .getSystemService(Context.INPUT_METHOD_SERVICE);
    inputManager
        .hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
  }
}
