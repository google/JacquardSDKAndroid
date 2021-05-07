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

package com.google.android.jacquard.sample;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import com.google.android.jacquard.sample.firmwareupdate.FirmwareUpdateViewModel;
import com.google.android.jacquard.sample.gesture.GestureViewModel;
import com.google.android.jacquard.sample.haptics.HapticsViewModel;
import com.google.android.jacquard.sample.home.HomeViewModel;
import com.google.android.jacquard.sample.imu.ImuSamplesListViewModel;
import com.google.android.jacquard.sample.imu.ImuViewModel;
import com.google.android.jacquard.sample.ledpattern.LedPatternViewModel;
import com.google.android.jacquard.sample.musicalthreads.MusicalThreadsViewModel;
import com.google.android.jacquard.sample.musicalthreads.player.ThreadsPlayer;
import com.google.android.jacquard.sample.places.PlacesConfigViewModel;
import com.google.android.jacquard.sample.places.PlacesDetailsViewModel;
import com.google.android.jacquard.sample.places.PlacesListViewModel;
import com.google.android.jacquard.sample.renametag.RenameTagViewModel;
import com.google.android.jacquard.sample.scan.ScanViewModel;
import com.google.android.jacquard.sample.splash.SplashViewModel;
import com.google.android.jacquard.sample.storage.ImuSessionsRepository;
import com.google.android.jacquard.sample.storage.PlacesRepository;
import com.google.android.jacquard.sample.tagmanager.TagDetailsViewModel;
import com.google.android.jacquard.sample.tagmanager.TagManagerViewModel;
import com.google.android.jacquard.sample.touchdata.TouchDataViewModel;
import com.google.android.jacquard.sdk.util.StringUtils;

/** Factory for creating view models and provide dependencies. */
public class ViewModelFactory extends ViewModelProvider.NewInstanceFactory {

  private final NavController navController;
  private final ResourceLocator resourceLocator;
  private final Application application;

  public ViewModelFactory(Application application, NavController navController) {
    this.navController = navController;
    this.resourceLocator = ((SampleApplication) application).getResourceLocator();
    this.application = application;
  }

  @SuppressWarnings("unchecked")
  @NonNull
  @Override
  public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
    ConnectivityManager connectivityManager = resourceLocator.getConnectivityManager();
    Preferences preferences = resourceLocator.getPreferences();
    if (modelClass == ScanViewModel.class) {
      return (T) new ScanViewModel(connectivityManager, preferences, navController);
    }
    if (modelClass == TouchDataViewModel.class) {
      return (T) new TouchDataViewModel(connectivityManager, navController);
    }
    if (modelClass == MusicalThreadsViewModel.class) {
      ThreadsPlayer threadsPlayer = resourceLocator.getThreadsPlayer();
      return (T) new MusicalThreadsViewModel(connectivityManager, threadsPlayer, navController);
    }
    if (modelClass == GestureViewModel.class) {
      return (T) new GestureViewModel(connectivityManager, navController, preferences);
    }
    if (modelClass == MainActivityViewModel.class) {
      return (T) new MainActivityViewModel(preferences);
    }
    if (modelClass == HapticsViewModel.class) {
      return (T) new HapticsViewModel(connectivityManager, navController,
          resourceLocator.getResources());
    }

    if (modelClass == LedPatternViewModel.class) {
      return (T) new LedPatternViewModel(connectivityManager, navController, StringUtils.getInstance(), preferences);
    }

    if (modelClass == HomeViewModel.class) {
      return (T) new HomeViewModel(preferences, navController, connectivityManager,
          resourceLocator.getRecipeManager());
    }

    if (modelClass == SplashViewModel.class) {
      return (T) new SplashViewModel(preferences, navController);
    }

    if (modelClass == TagManagerViewModel.class) {
      return (T) new TagManagerViewModel(connectivityManager, preferences, navController);
    }

    if (modelClass == RenameTagViewModel.class) {
      return (T) new RenameTagViewModel(preferences, navController,connectivityManager);
    }

    if (modelClass == TagDetailsViewModel.class) {
      return (T)
          new TagDetailsViewModel(
              connectivityManager,
              connectivityManager.getConnectedJacquardTag(),
              preferences,
              navController);
    }

    if (modelClass == FirmwareUpdateViewModel.class) {
      return (T)
          new FirmwareUpdateViewModel(resourceLocator.getFirmwareManager(), navController,
              connectivityManager);
    }
    if (modelClass == PlacesConfigViewModel.class) {
      return (T)
          new PlacesConfigViewModel(
              navController,
              resourceLocator.getRecipeManager()
          );
    }
    if (modelClass == PlacesListViewModel.class) {
      return (T)
          new PlacesListViewModel(
              connectivityManager,
              navController,
              resourceLocator.geocoder(),
              new PlacesRepository(application)
          );
    }
    if (modelClass == ImuViewModel.class) {
      return (T)
          new ImuViewModel(connectivityManager, navController,
              resourceLocator.getImuSessionDownloadDirectory(),
              new ImuSessionsRepository(application), preferences);
    }
    if (modelClass == ImuSamplesListViewModel.class) {
      return (T) new ImuSamplesListViewModel(navController);
    }
    if(modelClass == PlacesDetailsViewModel.class){
      return (T)
          new PlacesDetailsViewModel(navController, new PlacesRepository(application));
    }
    return super.create(modelClass);
  }
}
