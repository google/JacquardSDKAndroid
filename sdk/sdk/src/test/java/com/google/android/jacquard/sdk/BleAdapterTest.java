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
package com.google.android.jacquard.sdk;

import static com.google.android.jacquard.sdk.ConnectState.Type.CONNECTED;
import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.app.Instrumentation.ActivityResult;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build.VERSION_CODES;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.tag.AdvertisedJacquardTag;
import com.google.android.jacquard.sdk.tag.FakeAdvertisedJacquardTag;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link BleAdapter}.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public final class BleAdapterTest {

  private static final String FAKE_TAG_1 = "fake_tag_1";
  private static final String FAKE_TAG_2 = "fake_tag_2";
  private static final String ADDRESS = "C2:04:1C:6F:02:BA";
  private BleAdapter bleAdapter;
  private final List<Subscription> subscriptions = new ArrayList<>();
  private AdvertisedJacquardTag advertisedJacquardTag;
  private ConnectState connectState;
  private Context context;

  @Before
  public void setUp() {
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    context = ApplicationProvider.getApplicationContext();
    bleAdapter = new BleAdapter(ApplicationProvider.getApplicationContext());
    subscriptions.add(bleAdapter.startScan().onNext(tag -> advertisedJacquardTag = tag));
    subscriptions
        .add(bleAdapter.connect(context, bleAdapter.getDevice(ADDRESS), intentSender ->
            startForResult().map(result -> result.getResultCode() == Activity.RESULT_OK))
            .onNext(state -> connectState = state));
  }

  @Test
  public void startScan_returnsAdvertisedSignal() {
    // Act
    Signal<AdvertisedJacquardTag> signal = bleAdapter.startScan();
    // Assert
    assertThat(signal).isNotNull();
  }

  @Test
  public void startScan_unsubscribeScanCallback() {
    //Arrange
    Signal<AdvertisedJacquardTag> signal = bleAdapter.startScan();
    subscriptions.add(signal.onNext(tag -> advertisedJacquardTag = tag));
    // Act
    signal.next(new FakeAdvertisedJacquardTag(FAKE_TAG_1));
    unsubscribeSubscriptions();
    bleAdapter.destroy();
    signal.next(new FakeAdvertisedJacquardTag(FAKE_TAG_2));
    // Assert
    assertThat(advertisedJacquardTag.displayName()).isNotEqualTo(FAKE_TAG_2);
  }

  @Test
  public void getDevice_returnsBleDevice() {
    // Act
    BluetoothDevice bleDevice = bleAdapter.getDevice(ADDRESS);
    // Assert
    assertThat(bleDevice.getAddress()).isEqualTo(ADDRESS);
  }

  @Test
  public void getDevice_returnsNull() {
    // Act
    BluetoothDevice bleDevice = bleAdapter.getDevice(/* address= */null);
    // Assert
    assertThat(bleDevice).isNull();
  }

  @Test
  public void connect_emitsConnectState() {
    // Arrange
    BluetoothDevice bleDevice = bleAdapter.getDevice(ADDRESS);
    FakeConnectState fakeConnectState = new FakeConnectState();
    fakeConnectState.setType(CONNECTED);
    // Act
    Signal<ConnectState> signal = bleAdapter.connect(context, bleDevice, intentSender ->
        startForResult().map(result -> result.getResultCode() == Activity.RESULT_OK));
    subscriptions.add(signal.onNext(state -> this.connectState = state));
    signal.next(fakeConnectState);
    unsubscribeSubscriptions();
    // Assert
    assertThat(connectState.getType()).isEqualTo(CONNECTED);
  }

  private void unsubscribeSubscriptions() {
    for (Subscription subscription : subscriptions) {
      subscription.unsubscribe();
    }
    subscriptions.clear();
  }

  private Signal<ActivityResult> startForResult() {
  return Signal.from(new ActivityResult(Activity.RESULT_OK, null));
  }
}
