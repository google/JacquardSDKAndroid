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

package com.google.android.jacquard.sample.splash;

import androidx.lifecycle.ViewModel;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import com.google.android.jacquard.sample.Preferences;

/** View model for the {@link SplashFragment}. */
public class SplashViewModel extends ViewModel {

  private final Preferences preferences;
  private final NavController navController;

  public SplashViewModel(Preferences preferences, NavController navController) {
    this.preferences = preferences;
    this.navController = navController;
  }

  public void navigateToNextScreen() {
    NavDirections navDirections;
    if (preferences.getKnownTags().isEmpty()) {
      navDirections = SplashFragmentDirections.actionToScanFragment();
    } else {
      navDirections = SplashFragmentDirections.actionToHomeFragment();
    }
    navController.navigate(navDirections);
  }
}
