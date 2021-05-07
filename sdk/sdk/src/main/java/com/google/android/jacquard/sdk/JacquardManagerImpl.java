/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sdk;

import static com.google.android.jacquard.sdk.util.BluetoothUtils.isBluetoothEnabled;

import android.Manifest.permission;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentSender;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.annotation.VisibleForTesting;
import com.google.android.jacquard.sdk.ManagerScanningException.BluetoothDeviceNotFound;
import com.google.android.jacquard.sdk.ManagerScanningException.BluetoothUnavailableException;
import com.google.android.jacquard.sdk.connection.ConnectionState;
import com.google.android.jacquard.sdk.connection.TagConnectionStateMachine;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.SdkConfig;
import com.google.android.jacquard.sdk.remote.RemoteFactory;
import com.google.android.jacquard.sdk.remote.RemoteFunction;
import com.google.android.jacquard.sdk.rx.Fn;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.tag.AdvertisedJacquardTag;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concrete implementation of {@link JacquardManager}.
 */
class JacquardManagerImpl implements JacquardManager {

  private static final String TAG = JacquardManagerImpl.class.getSimpleName();
  private final BleAdapter bleAdapter;
  private final RemoteFunction remoteFunction;
  private final MemoryCache memoryCache = new MemoryCache();
  final Map<String, TagConnectionStateMachine> stateMachines = new ConcurrentHashMap<>();
  final Map<String, Subscription> stateMachineSubscription = new ConcurrentHashMap<>();
  private SdkConfig config;
  private Context context;
  private String lastConnectedDeviceAddress;

  /** Static singleton instance. */
  static JacquardManager instance;

  /**
   * Constructs a new JacquardManagerImpl.
   * @param context context providing access to the {@link android.bluetooth.BluetoothAdapter}.
   */
  public JacquardManagerImpl(Context context) {
    this(new BleAdapter(context), RemoteFactory
        .remoteInstance(context.getResources()));
    this.context = context.getApplicationContext();
  }

  /**
   * Constructs a new JacquardManagerImpl.
   *
   * @param bleAdapter adapter for providing access to the {@link android.bluetooth.BluetoothAdapter}.
   * @param remoteFunction RemoteFunction to be consumed.
   */
  JacquardManagerImpl(BleAdapter bleAdapter, RemoteFunction remoteFunction) {
    this.bleAdapter = bleAdapter;
    this.remoteFunction = remoteFunction;
  }

  @Override
  public Context getApplicationContext() {
    return context;
  }

  @RequiresPermission(permission.BLUETOOTH)
  @Override
  public Signal<AdvertisedJacquardTag> startScanning() {
    if (!isBluetoothEnabled()) {
      return Signal.empty(new BluetoothUnavailableException());
    }
    return bleAdapter.startScan();
  }

  @VisibleForTesting
  @RequiresPermission(permission.BLUETOOTH)
  Signal<ConnectionState> connect(AdvertisedJacquardTag tag,
      Fn<IntentSender, Signal<Boolean>> senderHandler, Context activityContext) {
    return connect(activityContext, tag.bluetoothDevice(), senderHandler);
  }

  @RequiresPermission(permission.BLUETOOTH)
  @Override
  public Signal<ConnectionState> connect(Context activityContext, String address,
      Fn<IntentSender, Signal<Boolean>> senderHandler) {
    if (!isBluetoothEnabled()) {
      return Signal.empty(new BluetoothUnavailableException());
    }
    BluetoothDevice bluetoothDevice = bleAdapter.getDevice(address);
    if (bluetoothDevice == null) {
      return Signal.empty(new BluetoothDeviceNotFound(address));
    }
    lastConnectedDeviceAddress = address;
    return connect(activityContext, bluetoothDevice, senderHandler);
  }

  /**
   * Releases all observables held for the tag.
   *
   * @param address mac address of the tag.
   */
  @Override
  public void forget(String address) {
    destroyStateMachine(address);
    unsubscribeStateMachine(address);
  }

  @Override
  public void init(SdkConfig config) {
    this.config = config;
  }

  /**
   * Returns the ConnectionState signal of provided address.
   *
   * @param address - Identifier of the tag.
   * @return - <code>Signal<ConnectionState></code> of provided address.
   */
  @Override
  public Signal<ConnectionState> getConnectionStateSignal(String address) {
    TagConnectionStateMachine stateMachine = stateMachines.get(address);
    if (stateMachine == null) {
      PrintLogger.e(TAG, "No state machine found for : " + address);
      return Signal.empty(new Exception("Device is not connected, address: " + address));
    }
    return stateMachine.getState();
  }

  /**
   * Retrieves {@link SdkConfig} created by {@link JacquardManager#init(SdkConfig)}.
   *
   * @return <code>SdkConfig</code> null if it is not created before this call.
   */
  @Nullable
  @Override
  public SdkConfig getSdkConfig() {
    return config;
  }

  /** Returns the MemoryCache object. */
  @Override
  public MemoryCache getMemoryCache() {
    return memoryCache;
  }

  /**
   * Connects to the provided Bluetooth device and emits the connected state.
   * @param device the {@link BluetoothDevice} to connect to.
   * @return a {@link Signal} with the {@link ConnectionState}.
   */
  @RequiresPermission(permission.BLUETOOTH)
  private Signal<ConnectionState> connect(Context activityContext, BluetoothDevice device,
      Fn<IntentSender, Signal<Boolean>> senderHandler) {
    PrintLogger.d(TAG,"connect to # " + device.getAddress());
    if (!isBluetoothEnabled()) {
      return Signal.empty(new BluetoothUnavailableException());
    }

    return remoteFunction.getComponents()
        .tap(RemoteFactory::createDataProvider).flatMap(ConnectionState -> {

          destroyStateMachine(device.getAddress());
          unsubscribeStateMachine(device.getAddress());

          TagConnectionStateMachine stateMachine = new TagConnectionStateMachine(
              device,
              bluetoothDevice -> doConnect(activityContext, bluetoothDevice, senderHandler)
          );

          stateMachines.put(device.getAddress(), stateMachine);

          stateMachine.connect();

          return stateMachine.getState().tap(state -> PrintLogger.d(TAG, "connect: " + state));
        });
  }

  /**
   * Calls destroy on the state machine mapped with address.
   * @param address the bluetooth address to look up the state machine
   */
  private void destroyStateMachine(String address) {
    PrintLogger.d(TAG,"destroyStateMachine #");
    TagConnectionStateMachine stateMachine = stateMachines.remove(address);
    if (stateMachine == null) {
      return;
    }
    stateMachine.destroy();
  }

  /**
   * Calls unsubscribe on the subscription mapped with address.
   * @param address the bluetooth address to look up the subscription.
   */
  private void unsubscribeStateMachine(String address) {
    PrintLogger.d(TAG,"unsubscribeStateMachine #");
    Subscription subscription = stateMachineSubscription.remove(address);
    if (subscription == null) {
      return;
    }
    subscription.unsubscribe();
  }

  /**
   * Connects to the provided device and forwards events from the device to state machines stored in
   * {@link JacquardManagerImpl#stateMachines}.
   * <p>
   * State machines are looked up by bluetooth address.
   *
   * @param device the {@link BluetoothDevice} to connect to
   */
  private void doConnect(Context activityContext, BluetoothDevice device,
      Fn<IntentSender, Signal<Boolean>> senderHandler) {
    PrintLogger.d(TAG, "doConnect #");
    unsubscribeStateMachine(device.getAddress());
    Subscription subscription = bleAdapter.connect(activityContext, device, senderHandler)
        .onNext(connectState -> {
          String deviceAddress = getDeviceAddress(connectState);
          if (!deviceAddress.equals(lastConnectedDeviceAddress)) {
            PrintLogger.d(TAG, "deviceAddress: " + deviceAddress + " lastConnectedDeviceAddress: "
                + lastConnectedDeviceAddress);
            return;
          }
          PrintLogger.d(TAG, "ConnectState: " + connectState + " deviceAddress: " + deviceAddress);
          TagConnectionStateMachine stateMachine = stateMachines.get(deviceAddress);

          if (stateMachine == null) {
            PrintLogger.e(TAG, "No state machine found for: " + deviceAddress);
            return;
          }

          stateMachine.onStateEvent(connectState);
        });
    stateMachineSubscription.put(device.getAddress(), subscription);
  }

  private String getDeviceAddress(ConnectState connectState) {
    switch (connectState.getType()) {
      case CONNECTED:
        return connectState.connected().getTagIdentifier();
      case FAILED_TO_CONNECT:
        return connectState.failedToConnect().peripheral().getTagIdentifier();
      case DISCONNECTED:
        return connectState.disconnected().peripheral().getTagIdentifier();
      case SERVICES_DISCOVERED:
        return connectState.servicesDiscovered().peripheral().getTagIdentifier();
      case CHARACTERISTIC_UPDATED:
        return connectState.characteristicUpdated().peripheral().getTagIdentifier();
      case VALUE_WRITTEN:
        return connectState.valueWritten().peripheral().getTagIdentifier();
      case VALUE_RSSI:
        return connectState.valueRssi().peripheral().getTagIdentifier();
    }
    return "";
  }

  @Override
  public void destroy() {
    for (StateMachine<?, ConnectState> stateMachine : stateMachines.values()) {
      stateMachine.destroy();
    }
    stateMachines.clear();

    for (Subscription subscription : stateMachineSubscription.values()) {
      subscription.unsubscribe();
    }
    stateMachineSubscription.clear();
    bleAdapter.destroy();
  }
}
