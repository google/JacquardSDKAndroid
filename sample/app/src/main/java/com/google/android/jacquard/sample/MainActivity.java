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

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.jacquard.sample.utilities.ShakeDetector;
import com.google.android.jacquard.sample.utilities.ShakeDetector.ShakeListener;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.rx.Signal;
import java.io.File;

public class MainActivity extends AppCompatActivity implements ShakeListener {

  private static final String TAG = MainActivity.class.getSimpleName();
  private static final BluetoothStateChangeReceiver bluetoothReceiver = new BluetoothStateChangeReceiver();
  public static final int COMPANION_DEVICE_REQUEST = 3;

  private final Signal<ActivityResult> activityResults = Signal.create();
  @SuppressWarnings("FieldCanBeLocal")
  private MainActivityViewModel viewModel;
  private ShakeDetector shakeDetector;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    PrintLogger.d(TAG,
        "onCreate # " + getString(R.string.app_version, BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE));
    setContentView(R.layout.activity_main);
    registerBluetoothReceiver();
    viewModel =
        new ViewModelProvider(this, new ViewModelFactory(getApplication(), getNavController()))
            .get(MainActivityViewModel.class);
    shakeDetector = new ShakeDetector(getBaseContext());
  }

  @Override
  protected void onPause() {
    super.onPause();
    shakeDetector.removeShakeDetector();
  }

  @Override
  protected void onResume() {
    super.onResume();
    shakeDetector.addShakeDetector(/* shakeListener = */this);
  }

  @Override
  protected void onDestroy() {
    unregisterReceiver(bluetoothReceiver);
    super.onDestroy();
  }

  private NavController getNavController() {
    NavHostFragment navHostFragment =
        (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
    if (navHostFragment == null) {
      throw new IllegalStateException("Failed to find NavHostFragment");
    }
    return navHostFragment.getNavController();
  }

  private void registerBluetoothReceiver() {
    IntentFilter filter = new IntentFilter();
    filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    registerReceiver(bluetoothReceiver, filter);
  }

  /**
   * Starts an {@link IntentSender} for result, adapted to work within a {@link Signal} framework.
   */
  public Signal<ActivityResult> startForResult(IntentSender intentSender, int requestCode) {
    try {
      startIntentSenderForResult(intentSender, requestCode, null, 0, 0, 0);
    } catch (IntentSender.SendIntentException e) {
      PrintLogger.e(TAG, "failed to launch intent sender: " + e.getMessage(), e);
      return Signal.empty(e);
    }
    return activityResults.filter(a -> a.requestCode() == requestCode).first();
  }

  @RequiresApi(api = VERSION_CODES.O)
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    activityResults.next(ActivityResult.create(requestCode, resultCode, data));
  }

  @Override
  public void onShakeDetected() {
    File loggerFile = PrintLogger.getLogFile(getApplicationContext());
    Uri fileUri = FileProvider
        .getUriForFile(this, getPackageName() + ".provider", loggerFile);
    Intent sendIntent = new Intent();
    sendIntent.setAction(Intent.ACTION_SEND);
    sendIntent.setType("*/*");
    sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
    sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    startActivity(Intent.createChooser(sendIntent, getString(R.string.share_log_file)));
  }
}
