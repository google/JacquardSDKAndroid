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
package com.google.android.jacquard.sdk.tag;

import static com.google.android.jacquard.sdk.initialization.Transport.DEFAULT_TIMEOUT;

import android.bluetooth.BluetoothGatt;
import android.util.Pair;
import androidx.annotation.VisibleForTesting;
import com.google.android.jacquard.sdk.JacquardManager;
import com.google.android.jacquard.sdk.command.AttachedNotificationSubscription;
import com.google.android.jacquard.sdk.command.CommandRequest;
import com.google.android.jacquard.sdk.command.DeviceInfo;
import com.google.android.jacquard.sdk.command.DeviceInfoCommand;
import com.google.android.jacquard.sdk.command.NotificationSubscription;
import com.google.android.jacquard.sdk.command.SetTouchModeCommand;
import com.google.android.jacquard.sdk.command.UjtReadConfigCommand;
import com.google.android.jacquard.sdk.command.UjtWriteConfigCommand;
import com.google.android.jacquard.sdk.connection.Result;
import com.google.android.jacquard.sdk.datastore.DataProvider;
import com.google.android.jacquard.sdk.dfu.DfuManager;
import com.google.android.jacquard.sdk.dfu.DfuManagerImpl;
import com.google.android.jacquard.sdk.initialization.Transport;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.GearState;
import com.google.android.jacquard.sdk.model.Peripheral.WriteType;
import com.google.android.jacquard.sdk.model.TouchMode;
import com.google.android.jacquard.sdk.rx.Fn;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.util.Objects;
import com.google.atap.jacquard.protocol.JacquardProtocol;
import com.google.atap.jacquard.protocol.JacquardProtocol.BleConfiguration;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import java.util.Arrays;
import java.util.List;

/** Concrete implementation of {@link ConnectedJacquardTag}. */
public class ConnectedJacquardTagImpl implements ConnectedJacquardTag {

  private static final String TAG = ConnectedJacquardTagImpl.class.getSimpleName();
  /** Signal for observing the gear. */
  private final Signal<GearState> componentSignal;
  /** Provides access to executing command and receiving notification from the tag. */
  private final Transport transport;
  /** Provides access to executing firmware update. */
  private DfuManager dfuManager;
  /** Tag component with deviceInfo. */
  private Component tagComponent;
  /** Gear component with deviceInfo. */
  private Component gearComponent;

  /**
   * Constructs a new instance of ConnectedJacquardTagImpl.
   * @param transport provides access to executing command and receiving notification from the tag
   */
  public ConnectedJacquardTagImpl(Transport transport, DeviceInfo deviceInfo) {
    this.transport = transport;
    componentSignal = subscribe(new AttachedNotificationSubscription())
        .distinctUntilChanged()
        .flatMap(gearState -> {
          PrintLogger
              .d(TAG, "## AttachedNotificationSubscription: "
                  + " GearState: " + gearState);
          if (gearState.getType() == GearState.Type.ATTACHED) {
            // Enable touch mode when the gear is attached.
            return setGestureTouchMode(gearState);
          } else {
            gearComponent = null;
          }
          return Signal.just(gearState);
        }).sticky();

    dfuManager = new DfuManagerImpl(address());
    tagComponent = DataProvider.getDataProvider().getTagComponent(deviceInfo);
    updateTagComponent();
    requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED);
  }

  @VisibleForTesting
  ConnectedJacquardTagImpl(Transport transport, DeviceInfo deviceInfo, DfuManager dfuManager) {
    this(transport, deviceInfo);
    this.dfuManager = dfuManager;
  }

  @Override
  public String serialNumber() {
    return tagComponent.serialNumber();
  }

  @Override
  public Signal<String> getCustomAdvName() {
    return enqueue(new UjtReadConfigCommand())
        .map(response -> response.getBleConfig().getCustomAdvName());
  }

  @Override
  public Component tagComponent() {
    return tagComponent;
  }

  @Override
  public Component gearComponent() {
    return gearComponent;
  }

  @Override
  public List<Component> getComponents() {
    if (gearComponent == null) {
      return Arrays.asList(tagComponent);
    }
    return Arrays.asList(tagComponent, gearComponent);
  }

  @Override
  public DfuManager dfuManager() {
    return dfuManager;
  }

  @Override
  public Signal<GearState> getConnectedGearSignal() {
    return componentSignal;
  }

  @Override
  public Signal<Boolean> setTouchMode(Component gearComponent, TouchMode touchMode) {
    PrintLogger.d(TAG, "## setTouchMode # " + touchMode);
    return enqueue(new SetTouchModeCommand(gearComponent, touchMode)).flatMap(
        (Fn<Response, Signal<Response>>) response -> {
          PrintLogger.d(TAG, "## setTouchModeCommand response: " + response);
          BleConfiguration.Builder builder = BleConfiguration.newBuilder();
          switch (touchMode) {
            case GESTURE:
              //TODO(b/201270703) Put the values elsewhere in constants.
              builder.setNotifQueueDepth(14);
              break;
            case CONTINUOUS:
              builder.setNotifQueueDepth(2);
              break;
          }
          return enqueue(new UjtWriteConfigCommand(builder.build()))
              .tap(res -> PrintLogger.d(TAG, "## UjtWriteConfigCommand: " + res));
        }).map(v -> v.getStatus() == JacquardProtocol.Status.STATUS_OK);
  }

  @Override
  public <Res, Request extends CommandRequest<Res>> Signal<Res> enqueue(Request request,
      int retries, long timeout) {
    return Signal.create(signal -> {
      transport.enqueue(request.getRequest(), WriteType.WITHOUT_RESPONSE, retries, timeout)
          .tapError(
              signal::error).onNext(response -> {
        Result<Res> parseResult = request.parseResponse(response);
        PrintLogger.d(TAG, "Command Response Received # " + parseResult);
        switch (parseResult.getType()) {
          case SUCCESS:
            signal.next(parseResult.success());
            signal.complete();
            break;
          case FAILURE:
            signal.error(parseResult.failure());
            break;
        }
      });
      return new Subscription();
    });
  }

  @Override
  public <Res, Request extends CommandRequest<Res>> Signal<Res> enqueue(Request request,
      int retries) {
    return enqueue(request, retries, DEFAULT_TIMEOUT);
  }

  @Override
  public <Res, Request extends CommandRequest<Res>> Signal<Res> enqueue(Request request) {
    return enqueue(request, /*retries=*/2);
  }

  @Override
  public <Res> Signal<Res> subscribe(NotificationSubscription<Res> subscription) {
    return transport
        .getNotificationSignal()
        .map(subscription::extract)
        .filter(Objects::nonNull)
        .shared();
  }

  @Override
  public Signal<Pair<Integer, byte[]>> getDataTransport() {
    return transport.getDataTransport()
        .filter(Objects::nonNull)
        .filter(data -> data.second != null && data.second.length > 0)
        .shared();
  }

  @Override
  public Signal<byte[]> getRawData() {
    return transport.getRawData()
        .filter(Objects::nonNull)
        .shared();
  }

  @Override
  public String address() {
    return transport.getPeripheralIdentifier();
  }

  @Override
  public String displayName() {
    return transport.getDefaultDisplayName();
  }

  @Override
  public Signal<Integer> rssiSignal() {
    return transport.fetchRSSIValue();
  }

  @Override
  public void requestConnectionPriority(int priority) {
    PrintLogger.d(TAG, "requestConnectionPriority = " + priority);
    transport.requestConnectionPriority(priority);
  }

  public void destroy() {
    transport.stopRSSIValue();
  }

  /** Fetching the tag deviceInfo and update the tag component with updated data. */
  private void updateTagComponent() {
    PrintLogger.d(TAG, "updateTagComponent");
    // Added delay to call tagDeviceInfo, so that ProtocolInitializationStateMachine can complete
    // the tag creation process.
    Signal.just(true).delay(100).onNext(ignore ->
        enqueue(new DeviceInfoCommand(Component.TAG_ID)).onNext(deviceInfo -> {
          PrintLogger.d(TAG, "updateTagComponent deviceInfo: " + deviceInfo);
          JacquardManager.getInstance().getMemoryCache()
              .putDeviceInfo(transport.getPeripheralIdentifier(), deviceInfo);
          tagComponent = DataProvider.getDataProvider().getTagComponent(deviceInfo);
        }));
  }

  /**
   * Sets the gesture touch mode for gear.
   *
   * @return a {@link Signal} emitting {@link GearState} after setting touch mode.
   */
  private Signal<GearState> setGestureTouchMode(GearState gearState) {
    PrintLogger.d(TAG, "setGestureTouchMode");
    return Signal.create(signal -> {
      Subscription subscription = setTouchMode(gearState.attached(), TouchMode.GESTURE)
          .flatMap(response -> enqueue(new DeviceInfoCommand(gearState.attached().componentId())))
          .observe(gearDeviceInfo -> {
            PrintLogger.d(TAG, "## DeviceInfoCommand response: " + gearDeviceInfo);
            gearComponent = DataProvider.getDataProvider()
                .getGearComponent(gearState.attached().componentId(), gearDeviceInfo.vendorId(),
                    gearDeviceInfo.productId(), gearDeviceInfo.version(),
                    gearDeviceInfo.serialNumber());
            PrintLogger.d(TAG, "## Extracted gearComponent: " + gearComponent);
            signal.next(gearState);
            // This method is called from inside flatMap and until inner signal is not completed,
            // flatMap will not execute any signal event, so called signal complete here.
            signal.complete();
          }, error -> {
            if (error != null) {
              PrintLogger.d(TAG, "setGestureTouchMode error: " + error);
              // In one cases not getting any response from UJT for SetTouchModeCommand.
              // TODO: Created b/200988181 ticket for firmware team regarding this issue.
              signal.complete();
            }
          });
      return new Subscription() {
        @Override
        protected void onUnsubscribe() {
          subscription.unsubscribe();
          super.onUnsubscribe();
        }
      };
    });
  }
}
