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
import com.google.android.jacquard.sdk.model.JacquardError;
import com.google.android.jacquard.sdk.model.Revision;
import com.google.android.jacquard.sdk.model.SdkConfig;
import com.google.android.jacquard.sdk.model.VidPidMid;
import com.google.android.jacquard.sdk.remote.RemoteFactory;
import com.google.android.jacquard.sdk.remote.RemoteFunction;
import com.google.android.jacquard.sdk.rx.Fn;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.tag.AdvertisedJacquardTag;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import com.google.android.jacquard.sdk.util.JQUtils;
import java.util.ArrayList;
import java.util.List;
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
  private final Context context;
  private List<Revision> badFirmwareVersions;
  private final ConcurrentHashMap<String, Signal<ConnectionState>> connectionStateSignalMap =
      new ConcurrentHashMap<>();

  // For now, it will have max one entry at any point of time.
  private final ConcurrentHashMap<String, Subscription> subscriptions =
      new ConcurrentHashMap<>();

  /** Static singleton instance. */
  static JacquardManager instance;
  private VidPidMid targetUjtFirmwareVidPid;

  /**
   * Constructs a new JacquardManagerImpl.
   * @param context context providing access to the {@link android.bluetooth.BluetoothAdapter}.
   */
  public JacquardManagerImpl(Context context) {
    this.context = context;
    this.bleAdapter = new BleAdapter(context);
    this.remoteFunction = RemoteFactory.remoteInstance();
    cacheBadFirmwareVersions();
  }

  /**
   * Constructs a new JacquardManagerImpl.
   *
   * @param context context providing access to the {@link android.bluetooth.BluetoothAdapter}.
   * @param bleAdapter adapter for providing access to the {@link
   *     android.bluetooth.BluetoothAdapter}.
   * @param remoteFunction RemoteFunction to be consumed.
   */
  @VisibleForTesting
  JacquardManagerImpl(Context context, BleAdapter bleAdapter, RemoteFunction remoteFunction) {
    this.context = context;
    this.bleAdapter = bleAdapter;
    this.remoteFunction = remoteFunction;
    cacheBadFirmwareVersions();
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

  @Override
  public Signal<ConnectionState> connect(String address) {
    return connect(null, address, null);
  }

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
    clearSubscriptions(address);
    if (connectionStateSignalMap.get(address) == null) {
      connectionStateSignalMap.put(address, createConnectionStateSignal());
    }
    Signal<ConnectionState> connectionStateSignal = connectionStateSignalMap.get(address);
    subscriptions.put(
        address,
        connect(activityContext, bluetoothDevice, senderHandler)
            .tap(state -> PrintLogger.d(TAG, "createBond: " + state))
            .forward(connectionStateSignal));
    return connectionStateSignal;
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
    closeConnectionSignal(address);
    clearSubscriptions(address);
  }

  @Override
  public void init(SdkConfig config) {
    if (!JQUtils.isValidUrl(config.cloudEndpointUrl())) {
      this.config = SdkConfig
          .of(config.clientId(), config.apiKey(), BuildConfig.CLOUD_ENDPOINT_PRODUCTION);
    } else {
      this.config = config;
    }
  }

  /**
   * Emits the {@link ConnectionState} for the tag defined by address. If Tag for the address is not
   * initiated the connection process through {@link JacquardManager#connect(Context, String,
   * Fn<IntentSender, Signal<Boolean>>)}, then returned signal will provide updates after the app
   * initiate the process for the address.
   *
   * @param address Unique address of the Tag.
   * @return Signal<ConnectionState> for the Tag.
   */
  @Override
  public Signal<ConnectionState> getConnectionStateSignal(String address) {
    Signal<ConnectionState> signalState = connectionStateSignalMap.get(address);
    if (signalState == null) {
      return Signal.empty(JacquardError.ofBluetoothConnectionError(new IllegalStateException(
          "Please call JacquardManager#connect before accessing connection state")));
    }
    return connectionStateSignalMap.get(address);
  }

  /**
   * Emits a provided address connectedTag or IllegalStateException if connectedTag is not
   * connected.
   */
  @Override
  public Signal<ConnectedJacquardTag> getConnectedTag(String address) {
    return getConnectionStateSignal(address)
        .flatMap(
            state -> {
              if (!state.isType(ConnectionState.Type.CONNECTED)) {
                return Signal.empty(
                    JacquardError.ofBluetoothConnectionError(
                        new IllegalStateException("Device is not connected, address: " + address)));
              }
              return Signal.from(state.connected());
            })
        .first();
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

  @Override
  public List<Revision> getBadFirmwareVersions() {
    return badFirmwareVersions;
  }

  @Override
  public void setTargetUjtFirmwareVidPid(@Nullable VidPidMid targetUjtFirmwareVidPid) {
    this.targetUjtFirmwareVidPid = targetUjtFirmwareVidPid;
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
    closeAllConnectionSignal();
    clearAllSubscriptions();
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

    if (stateMachines.containsKey(device.getAddress())) {
      PrintLogger.d(TAG, "reusing connection for device # " + device.getAddress());
      TagConnectionStateMachine tagConnectionStateMachine = stateMachines.get(device.getAddress());
      return tagConnectionStateMachine.getState().first().flatMap(state -> {
        if (state.isType(ConnectionState.Type.DISCONNECTED)) {
          tagConnectionStateMachine.connect(/* isUserInitiated= */ true);
        }
        return tagConnectionStateMachine.getState();
      });
    }

    return remoteFunction
        .getComponents()
        .tap(RemoteFactory::createDataProvider)
        .flatMap(
            ConnectionState -> {
              destroyStateMachine(device.getAddress());
              unsubscribeStateMachine(device.getAddress());

              TagConnectionStateMachine stateMachine =
                  new TagConnectionStateMachine(
                      device,
                      badFirmwareVersions,
                      bluetoothDevice ->
                          doConnect(activityContext, bluetoothDevice, senderHandler), targetUjtFirmwareVidPid);

              stateMachines.put(device.getAddress(), stateMachine);

              stateMachine.connect(/* isUserInitiated= */ true);

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
          PrintLogger
              .d(TAG, "ConnectState: " + connectState + " deviceAddress: " + device.getAddress());
          TagConnectionStateMachine stateMachine = stateMachines.get(device.getAddress());
          if (stateMachine == null) {
            PrintLogger.e(TAG, "No state machine found for: " + device.getAddress());
            return;
          }
          stateMachine.onStateEvent(connectState);
        });
    stateMachineSubscription.put(device.getAddress(), subscription);
  }

  private void cacheBadFirmwareVersions() {
    remoteFunction
        .getBadFirmwareVersions()
        .onNext(
            list -> {
              if (badFirmwareVersions == null) {
                badFirmwareVersions = new ArrayList<>(list.size());
              }
              badFirmwareVersions.clear();
              badFirmwareVersions.addAll(list);
            });
  }

  /** Remove connectivityManager subscription.  */
  private void clearSubscriptions(String address) {
    if(!subscriptions.containsKey(address)) {
      return;
    }
    subscriptions.remove(address).unsubscribe();
  }

  private void clearAllSubscriptions() {
    for (Subscription subscription : subscriptions.values()) {
      subscription.unsubscribe();
    }
    subscriptions.clear();
  }

  private void closeAllConnectionSignal() {
    for (Signal<ConnectionState> connectionStateSignal : connectionStateSignalMap.values()) {
      connectionStateSignal.complete();
    }
    connectionStateSignalMap.clear();
  }

  private void closeConnectionSignal(String address) {
    if (connectionStateSignalMap.containsKey(address)
        && connectionStateSignalMap.get(address) != null) {
      connectionStateSignalMap.remove(address).complete();
    }
  }
  private Signal<ConnectionState> createConnectionStateSignal() {
    return Signal.<ConnectionState>create().sticky();
  }
}
