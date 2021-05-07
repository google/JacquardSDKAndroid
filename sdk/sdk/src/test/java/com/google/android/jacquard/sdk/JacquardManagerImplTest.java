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

package com.google.android.jacquard.sdk;

import static com.google.android.jacquard.sdk.connection.ConnectionState.Type.CONNECTING;
import static com.google.android.jacquard.sdk.connection.ConnectionState.Type.PREPARING_TO_CONNECT;
import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.app.Instrumentation.ActivityResult;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentSender;
import android.os.Build;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.ManagerScanningException.BluetoothDeviceNotFound;
import com.google.android.jacquard.sdk.ManagerScanningException.BluetoothUnavailableException;
import com.google.android.jacquard.sdk.connection.ConnectionState;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.FakePeripheral;
import com.google.android.jacquard.sdk.model.SdkConfig;
import com.google.android.jacquard.sdk.remote.FakeLocalRemoteFunction;
import com.google.android.jacquard.sdk.rx.Fn;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.tag.AdvertisedJacquardTag;
import com.google.android.jacquard.sdk.tag.FakeAdvertisedJacquardTag;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link JacquardManagerImpl}.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {Build.VERSION_CODES.P}, manifest = Config.NONE)
public class JacquardManagerImplTest {

  private static final String FAKE_TAG = "fake_tag_1";
  private static final String FAKE_ADDRESS = "C2:04:1C:6F:02:BA";
  private final Fn<IntentSender, Signal<Boolean>> consumer = intentSender -> Signal
      .from(new ActivityResult(Activity.RESULT_OK, null))
      .map(result -> result.getResultCode() == Activity.RESULT_OK);
  private FakeBleAdapter bleAdapter;
  private JacquardManagerImpl jacquardManager;
  private Context context;

  @Before
  public void setUp() {
    context = ApplicationProvider.getApplicationContext();
    PrintLogger.initialize(context);
    bleAdapter = new FakeBleAdapter(context);
    jacquardManager = new JacquardManagerImpl(bleAdapter,
        new FakeLocalRemoteFunction(context.getResources()));
  }

  @Test
  public void getApplicationContext() {
    // Act
    Context applicationContext = new JacquardManagerImpl(context).getApplicationContext();

    // Assert
    assertThat(applicationContext).isEqualTo(context);
  }

  @Test
  public void startScanning_BluetoothUnavailableException() {
    // Assign
    AtomicReference<Throwable> throwableReference = new AtomicReference<>();
    // Act
    jacquardManager.startScanning().onError(throwableReference::set);
    // Assert
    assertThat(throwableReference.get()).isInstanceOf(BluetoothUnavailableException.class);
  }

  @Test
  public void startScanning() {
    // Assign
    BluetoothAdapter.getDefaultAdapter().enable();
    // Act
    Signal<AdvertisedJacquardTag> signal = jacquardManager.startScanning();
    // Assert
    assertThat(signal.hasError()).isFalse();
  }

  @Test
  public void testConnectByAddress_BluetoothUnavailableException() {
    // Assign
    AtomicReference<Throwable> throwableReference = new AtomicReference<>();
    // Act
    jacquardManager.connect(context, "test", consumer).onError(throwableReference::set);
    // Assert
    assertThat(throwableReference.get()).isInstanceOf(BluetoothUnavailableException.class);
  }

  @Test
  public void testConnectByAddress_BluetoothDeviceNotFound() {
    // Assign
    AtomicReference<Throwable> throwableReference = new AtomicReference<>();
    BluetoothAdapter.getDefaultAdapter().enable();
    // Act
    jacquardManager.connect(context, "test", consumer).onError(throwableReference::set);
    // Assert
    assertThat(throwableReference.get()).isInstanceOf(BluetoothDeviceNotFound.class);
  }

  @Test
  public void testConnectByAddress_preparingToConnect() {
    // Assign
    AtomicReference<ConnectionState> connectionStateReference = new AtomicReference<>();
    BluetoothAdapter.getDefaultAdapter().enable();
    jacquardManager.connect(context, FAKE_ADDRESS, consumer).consume();
    // Act
    jacquardManager.connect(context, FAKE_ADDRESS, consumer).onNext(connectionStateReference::set);
    // Assert
    assertThat(connectionStateReference.get().getType()).isEqualTo(PREPARING_TO_CONNECT);
  }

  @Test
  public void testConnectByAddress_doConnect() {
    // Assign
    AtomicReference<ConnectionState> connectionStateReference = new AtomicReference<>();
    BluetoothAdapter.getDefaultAdapter().enable();
    jacquardManager.connect(context, FAKE_ADDRESS, consumer).onNext(connectionStateReference::set);
    // Act
    bleAdapter.stateSignal.next(ConnectState.ofConnected(new FakePeripheral(/* bleQueue= */ null)));
    // Assert
    assertThat(connectionStateReference.get().getType()).isEqualTo(CONNECTING);
  }

  @Test
  public void testConnectByTag() {
    // Act
    BluetoothDevice device = Mockito.mock(BluetoothDevice.class, "JacquardBluetoothDevice");
    Mockito.when(device.getAddress()).thenReturn(FAKE_ADDRESS);
    FakeAdvertisedJacquardTag tag = new FakeAdvertisedJacquardTag(FAKE_TAG, device);
    Signal<ConnectionState> signal = jacquardManager.connect(tag, consumer, context);

    // Assert
    assertThat(signal).isNotNull();
  }

  @Test
  public void testForget() {
    // Act
    jacquardManager.forget(FAKE_TAG);

    // Assert
    assertThat(jacquardManager.stateMachineSubscription.size()).isLessThan(1);
  }

  @Test
  public void init() {
    // Assign
    SdkConfig config = SdkConfig.of("clientId", "apiKey");
    // Act
    jacquardManager.init(config);
    // Assert
    assertThat(jacquardManager.getSdkConfig()).isEqualTo(config);
  }

  @Test
  public void getConnectionStateSignal_withError() {
    // Assign
    AtomicReference<Throwable> throwableReference = new AtomicReference<>();
    // Act
    jacquardManager.getConnectionStateSignal(FAKE_ADDRESS).onError(throwableReference::set);
    // Assert
    assertThat(throwableReference.get()).isInstanceOf(Exception.class);
  }

  @Test
  public void getConnectionStateSignal() {
    // Assign
    AtomicReference<ConnectionState> connectionStateReference = new AtomicReference<>();
    BluetoothAdapter.getDefaultAdapter().enable();
    jacquardManager.connect(context, FAKE_ADDRESS, consumer).consume();
    // Act
    jacquardManager.getConnectionStateSignal(FAKE_ADDRESS).onNext(connectionStateReference::set);
    // Assert
    assertThat(connectionStateReference.get().getType()).isEqualTo(PREPARING_TO_CONNECT);
  }

  @Test
  public void testDestroy() {
    // Assign
    BluetoothAdapter.getDefaultAdapter().enable();
    jacquardManager.connect(context, FAKE_ADDRESS, consumer).consume();
    // Act
    jacquardManager.destroy();
    // Assert
    assertThat(jacquardManager.stateMachines.size()).isEqualTo(0);
    assertThat(jacquardManager.stateMachineSubscription.size()).isEqualTo(0);
  }
}