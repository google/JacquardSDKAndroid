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
package com.google.android.jacquard.sdk.connection;

import static com.google.android.jacquard.sdk.ConnectState.Type.DISCONNECTED;
import static com.google.android.jacquard.sdk.connection.ConnectionState.Type.CONFIGURING;
import static com.google.android.jacquard.sdk.connection.ConnectionState.Type.CONNECTED;
import static com.google.android.jacquard.sdk.connection.ConnectionState.Type.CONNECTING;
import static com.google.android.jacquard.sdk.connection.ConnectionState.Type.INITIALIZING;
import static com.google.android.jacquard.sdk.connection.ConnectionState.ofConfiguring;
import static com.google.android.jacquard.sdk.connection.ConnectionState.ofConnected;
import static com.google.android.jacquard.sdk.connection.ConnectionState.ofConnecting;
import static com.google.android.jacquard.sdk.connection.ConnectionState.ofDisconnected;
import static com.google.android.jacquard.sdk.connection.ConnectionState.ofFirmwareTransferComplete;
import static com.google.android.jacquard.sdk.connection.ConnectionState.ofFirmwareUpdateInitiated;
import static com.google.android.jacquard.sdk.connection.ConnectionState.ofInitializing;
import static com.google.android.jacquard.sdk.connection.ConnectionState.ofPreparingToConnect;

import android.bluetooth.BluetoothDevice;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Pair;
import com.google.android.jacquard.sdk.ConnectState;
import com.google.android.jacquard.sdk.JacquardManager;
import com.google.android.jacquard.sdk.StateMachine;
import com.google.android.jacquard.sdk.command.UjtWriteConfigCommand;
import com.google.android.jacquard.sdk.dfu.DFUInfo;
import com.google.android.jacquard.sdk.initialization.InitializationState;
import com.google.android.jacquard.sdk.initialization.ProtocolInitializationStateMachine;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.JacquardError;
import com.google.android.jacquard.sdk.model.Revision;
import com.google.android.jacquard.sdk.model.VidPidMid;
import com.google.android.jacquard.sdk.pairing.TagPairingState;
import com.google.android.jacquard.sdk.pairing.TagPairingStateMachine;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.ObservesNext;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTagImpl;
import com.google.android.jacquard.sdk.util.Function;
import com.google.atap.jacquard.protocol.JacquardProtocol.BleConfiguration;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A state machine for handling connecting to a tag.
 * It receives events from {@link com.google.android.jacquard.sdk.BleAdapter} via
 * {@link TagConnectionStateMachine#onStateEvent(ConnectState)}.
 * State changes can be observed via {@link TagConnectionStateMachine#getState()}.
 * <p>
 * TagConnectionStateMachine has two child state machines.
 * First events from onConnectStateEvent are forwarded to a {@link TagPairingStateMachine}.
 * Once the tag is paired the child will be replaced with a
 * {@link ProtocolInitializationStateMachine}.
 * TagConnectionStateMachine will emit {@link ConnectionState}.
 * When the connection has been established the
 * {@link ConnectionState#ofConnected(ConnectedJacquardTag)} will be emitted with an instance of the
 * connected tag.
 */
public class TagConnectionStateMachine implements StateMachine<ConnectionState, ConnectState> {

  private static final String TAG = TagConnectionStateMachine.class.getSimpleName();
  private static final float SLOW_ADV_INTERVAL = 417.5f;
  private static final float MEDIUM_ADV_INTERVAL = 546.25f;
  private static final int NOTIFY_QUEUE_DEPTH  = 14;
  private final Signal<ConnectionState> stateSignal = Signal.<ConnectionState>create()
      .sticky();
  private final StateMachineContext stateMachineContext = new StateMachineContext();
  private final BluetoothDevice device;
  private final Function<BluetoothDevice> connectMethod;
  private final List<Revision> badFirmwareVersions;
  private final boolean shouldReconnect = true;
  private final AtomicBoolean isUserInitiated = new AtomicBoolean();
  private ConnectionState state = ofPreparingToConnect();
  private ConnectedJacquardTag tagWhileFirmwareUpdate;
  private VidPidMid targetUjtFirmwareVidPid;
  private Subscription checkUpdateSubscription;
  private Subscription applyUpdateSubscription;

  /**
   * Creates a new instance of TagConnectionStateMachine.
   *
   * @param device the device to connect to.
   * @param connectMethod a reference to BleAdapter.connect()
   */
  public TagConnectionStateMachine(
      BluetoothDevice device,
      List<Revision> badFirmwareVersions,
      Function<BluetoothDevice> connectMethod,
      @Nullable VidPidMid targetUjtFirmwareVidPid) {
    this.device = device;
    this.connectMethod = connectMethod;
    this.badFirmwareVersions = badFirmwareVersions;
    this.targetUjtFirmwareVidPid = targetUjtFirmwareVidPid;
    stateSignal.next(state);
  }

  @Override
  public Signal<ConnectionState> getState() {
    return stateSignal;
  }

  @Override
  public void onStateEvent(ConnectState state) {
    PrintLogger.d(TAG,"onConnectStateEvent # " + state);
    handleConnectStateEvent(state);
    stateMachineContext.childStateMachine.onStateEvent(state);
  }

  /** Handles events received by onConnectStateEvent. */
  private void handleConnectStateEvent(ConnectState state) {
    PrintLogger.d(TAG, "handleConnectStateEvent # " + state.getType().name());
    if (state.isType(DISCONNECTED)) {
      unsubscribeFirmwareUpdate();
      handleEvent(ConnectionEvent.ofTagDisconnected(state.disconnected().error()));
    }
  }

  private void unsubscribeFirmwareUpdate() {
    if (checkUpdateSubscription != null) {
      checkUpdateSubscription.unsubscribe();
      checkUpdateSubscription = null;
    }
    if (applyUpdateSubscription != null) {
      applyUpdateSubscription.unsubscribe();
      applyUpdateSubscription = null;
    }
  }

  /** Handles internal events. */
  private void handleEvent(ConnectionEvent event) {
    PrintLogger.d(TAG, "handleEvent: " + event + " for state:" + state);
    if (state.isTerminal()) {
      PrintLogger.d(TAG, "State machine is already terminal, ignoring event");
      return;
    }

    switch (event.getType()) {
      case CONNECTION_ERROR:
        onConnectionErrorEvent(event);
        break;
      case CONNECTION_PROGRESS:
        onConnectionProgressEvent();
        break;
      case INITIALIZATION_PROGRESS:
        onInitializationProgress();
        break;
      case TAG_PAIRED:
        onTagPairedEvent(event);
        break;
      case TAG_INITIALIZED:
        onTagInitializedEvent(event);
        break;
      case TAG_CONFIGURED:
        onTagConfiguredEvent(event);
        break;
      case FIRMWARE_UPDATE_INITIATED:
        onFirmwareUpdateInitiated(event);
        break;
      case FIRMWARE_TRANSFERRING:
        onFirmwareTransferringEvent(event);
        break;
      case FIRMWARE_TRANSFER_COMPLETE:
        onFirmwareTransferCompleteEvent(event);
        break;
      case FIRMWARE_EXECUTING:
        onFirmwareExecutingEvent(event);
        break;
      case FIRMWARE_UPDATE_ERROR:
        onFirmwareUpdateError(event);
        break;
      case TAG_DISCONNECTED:
        onTagDisconnectedEvent(event);
        break;
    }
  }

  /** Called when a {@link ConnectionEvent#ofTagDisconnected(JacquardError)} event is received. */
  private void onTagDisconnectedEvent(ConnectionEvent event) {
    PrintLogger.d(TAG, "onTagDisconnectedEvent: " + event + " current state: " + state);
    if (state.isType(ConnectionState.Type.DISCONNECTED)) {
      return;
    }
    ConnectedJacquardTagImpl tagImpl =
        (ConnectedJacquardTagImpl)
            (state.isType(CONNECTED) ? state.connected() : tagWhileFirmwareUpdate);
    if (tagImpl != null) {
      PrintLogger.d(TAG, "onTagDisconnectedEvent: ConnectedJacquardTag " + tagImpl);
      tagImpl.destroy();
    }
    stateMachineContext.childStateMachine = new EmptyChildStateMachine();

    // Got discounted while Firmware process, then stopping reconnection process.
    if (state.isType(ConnectionState.Type.FIRMWARE_UPDATE_INITIATED)
        || state.isType(ConnectionState.Type.FIRMWARE_TRANSFERRING)) {
      updateState(ofDisconnected(event.tagDisconnected()));
      return;
    }

    if (shouldReconnectForDisconnection(event.tagDisconnected())) {
      updateState(ofPreparingToConnect());
      connect(/* isUserInitiated= */ false);
    } else {
      updateState(ofDisconnected(event.tagDisconnected()));
    }
  }

  /** Called when a {@link ConnectionEvent#ofTagConfigured(ConnectedJacquardTag)} event is received. */
  private void onTagConfiguredEvent(ConnectionEvent event) {
    PrintLogger.d(TAG, "onTagConfiguredEvent: " + event);
    if (!state.isType(CONFIGURING)) {
      return;
    }
    PrintLogger.d(TAG, "onTagConfiguredEvent enter");
    PrintLogger.d(
        TAG,
        "onTagConfiguredEvent Tag version: "
            + event.tagConfigured().tagComponent().version().toString());

    if (badFirmwareCheck(event.tagConfigured().tagComponent().version())) {
      PrintLogger.d(TAG, "onTagConfiguredEvent isUserCalled: " + isUserInitiated);
      if (isUserInitiated.get()) {
        handleEvent(ConnectionEvent.ofFirmwareUpdateInitiated(event.tagConfigured()));
      } else {
        updateState(ofDisconnected(JacquardError.ofUnknownCoreBluetoothError()));
      }
    } else {
      PrintLogger.d(TAG, "No bad firmware identified");
      updateState(ofConnected(event.tagConfigured()));
      tagWhileFirmwareUpdate = null;
    }
  }

  private void onFirmwareUpdateInitiated(ConnectionEvent event) {
    PrintLogger.d(TAG, "onFirmwareUpdateInitiated");
    tagWhileFirmwareUpdate = event.firmwareUpdateInitiated();
    updateState(ofFirmwareUpdateInitiated());
    checkUpdate(event.firmwareUpdateInitiated());
  }

  private void checkUpdate(ConnectedJacquardTag tag) {
    PrintLogger.d(TAG, "checkUpdate");
    checkUpdateSubscription = tag.dfuManager()
            .checkFirmware(Collections.singletonList(tag.tagComponent()),
                    targetUjtFirmwareVidPid != null ? targetUjtFirmwareVidPid.vid() : null,
                    targetUjtFirmwareVidPid != null ? targetUjtFirmwareVidPid.pid() : null,
                    true)
            .tapError(error -> handleEvent(ConnectionEvent.ofFirmwareUpdateError(error)))
        .onNext(
            dfuInfos -> {
              PrintLogger.d(TAG, "Badfirmware checkFirmware result : " + dfuInfos);
              if (dfuInfos.isEmpty()) {
                updateState(ofConnected(tag));
                return;
              }
              applyUpdate(tag, dfuInfos);
            });
  }

  private void applyUpdate(ConnectedJacquardTag tag, List<DFUInfo> list) {
    PrintLogger.d(TAG, "applyUpdate");
    applyUpdateSubscription = tag.dfuManager()
        .applyUpdates(list, /* autoExecute= */ true)
        .distinctUntilChanged()
        .tapError(error -> handleEvent(ConnectionEvent.ofFirmwareUpdateError(error)))
        .onNext(
            firmwareUpdateState -> {
              switch (firmwareUpdateState.getType()) {
                case TRANSFER_PROGRESS:
                  handleEvent(
                      ConnectionEvent.ofFirmwareTransferring(
                          firmwareUpdateState.transferProgress()));
                  break;
                case TRANSFERRED:
                  handleEvent(ConnectionEvent.ofFirmwareTransferComplete(tag));
                  break;
                case EXECUTING:
                  handleEvent(ConnectionEvent.ofFirmwareExecuting(tag));
                  break;
                case ERROR:
                  handleEvent(ConnectionEvent.ofFirmwareUpdateError(firmwareUpdateState.error()));
                  break;
                default:
              }
            });
  }

  private void onFirmwareTransferringEvent(ConnectionEvent event) {
    PrintLogger.d(TAG, "onFirmwareTransferringEvent: " + event);
    updateState(ConnectionState.ofFirmwareTransferring(event.firmwareTransferring()));
  }

  private void onFirmwareExecutingEvent(ConnectionEvent event) {
    PrintLogger.d(TAG, "onFirmwareExecutingEvent: " + event);
    JacquardManager.getInstance().getMemoryCache().putDeviceInfo(tagWhileFirmwareUpdate.address(), null);
    updateState(ConnectionState.ofFirmwareExecuting());
  }

  private void onFirmwareTransferCompleteEvent(ConnectionEvent event) {
    PrintLogger.d(TAG, "onFirmwareTransferCompleteEvent: " + event);
    updateState(ofFirmwareTransferComplete());
  }

  private void onFirmwareUpdateError(ConnectionEvent event) {
    PrintLogger.d(TAG, "onFirmwareUpdateError: " + event);
    updateConnectionErrorState(
        ofDisconnected(JacquardError.ofJacquardInitializationError(event.firmwareUpdateError())));
  }

  /**
   * Called when a {@link ConnectionEvent#ofTagPaired(Pair)} event is received.
   */
  private void onTagPairedEvent(ConnectionEvent event) {
    PrintLogger.d(TAG, "onTagPairedEvent: " + event);

    if (!state.isType(CONNECTING)) {
      return;
    }
    PrintLogger.d(TAG, "onTagPairedEvent enter");

    updateState(ofInitializing());
    ProtocolInitializationStateMachine protocolInitializationStateMachine = new ProtocolInitializationStateMachine(
        event.tagPaired().first, event.tagPaired().second);
    initializeConnection(protocolInitializationStateMachine);
  }

  /**
   * Called when a {@link ConnectionEvent#ofTagInitialized(ConnectedJacquardTag)} event is
   * received.
   **/
  private void onTagInitializedEvent(ConnectionEvent event) {
    PrintLogger.d(TAG, "onTagInitializedEvent: " + event);
    if (!state.isType(INITIALIZING)) {
      return;
    }
    PrintLogger.d(TAG, "onTagInitializedEvent enter");
    updateState(ofConfiguring());
    configureTag(event.tagInitialized());
  }

  /** Called when a {@link ConnectionEvent#ofInitializationProgress()} event is received. */
  private void onInitializationProgress() {
    PrintLogger.d(TAG, "onInitializationProgress");
    if (!state.isType(INITIALIZING)) {
      return;
    }
    PrintLogger.d(TAG, "onTagInitializedEvent enter");

    updateState(ofInitializing());
  }

  /** Called when a {@link ConnectionEvent#ofConnectionProgress()} event is received. */
  private void onConnectionProgressEvent() {
    PrintLogger.d(TAG, "onConnectionProgressEvent with state:" + state);
    switch (state.getType()) {
      case PREPARING_TO_CONNECT:
      case CONNECTING:
        updateState(ofConnecting());
        break;
    }
  }

  /** Called when a {@link ConnectionEvent#ofConnectionError(JacquardError)} event is received. */
  private void onConnectionErrorEvent(ConnectionEvent event) {
    PrintLogger.d(TAG, "onConnectionErrorEvent: " + event);
    updateConnectionErrorState(ofDisconnected(event.connectionError()));
  }

  private void updateConnectionErrorState(ConnectionState state) {
    PrintLogger.d(TAG, "updateConnectionErrorState: " + state);
    updateState(state);
    if (shouldReconnect) {
      updateState(ofPreparingToConnect());
      connect(/* isUserInitiated= */ false);
    }
  }

  /** Inspects the error and determines if the SDK should attempt to reconnect.  */
  private boolean shouldReconnectForDisconnection(JacquardError error) {
    return !error.getType().equals(JacquardError.Type.BLUETOOTH_OFF_ERROR);
  }

  /**
   * After the pairing phase we can initialize the jacquard protocol.
   * <p>
   * The child state machine is replace with a {@link ProtocolInitializationStateMachine} and events
   * received in {@link TagConnectionStateMachine#onStateEvent(ConnectState)} are forwarded
   * to the {@link ProtocolInitializationStateMachine}. When initialization is done the child state
   * machine emits a {@link InitializationState#ofTagInitialized(ConnectedJacquardTag)} and the tag
   * is ready to use.
   *
   * @param protocolInitializationStateMachine state machine for handling the protocol
   *                                           initialization phase
   */
  @VisibleForTesting
  void initializeConnection( ProtocolInitializationStateMachine protocolInitializationStateMachine) {
    PrintLogger.d(TAG, "initializeConnection");

    stateMachineContext.childStateMachine = protocolInitializationStateMachine;
    if (stateMachineContext.childStateMachineSubscription != null) {
      stateMachineContext.childStateMachineSubscription.unsubscribe();
    }
    stateMachineContext.childStateMachineSubscription =
        protocolInitializationStateMachine
            .getState()
            .observe(
                new ObservesNext<InitializationState>() {
                  @Override
                  public void onNext(@NonNull InitializationState state) {

                    switch (state.getType()) {
                      case PAIRED:
                        // Initial state
                        break;
                      case HELLO_SENT:
                      case BEGIN_SENT:
                      case COMPONENT_INFO_SENT:
                        handleEvent(ConnectionEvent.ofInitializationProgress());
                        break;
                      case TAG_INITIALIZED:
                        handleEvent(ConnectionEvent.ofTagInitialized(state.tagInitialized()));
                        break;
                      case ERROR:
                        handleEvent(
                            ConnectionEvent.ofConnectionError(
                                JacquardError.ofJacquardInitializationError(state.error())));
                        break;
                    }
                  }
                });
    protocolInitializationStateMachine.startNegotiation();
  }

  /** Configures the tag by setting the queue depth. */
  private void configureTag(ConnectedJacquardTag tag) {
    PrintLogger.d(TAG, "ConfigureTag #");
    BleConfiguration bleConfiguration = BleConfiguration.newBuilder()
        .setMediumAdvIntervalMs(MEDIUM_ADV_INTERVAL)
        .setSlowAdvIntervalMs(SLOW_ADV_INTERVAL)
        .setNotifQueueDepth(NOTIFY_QUEUE_DEPTH)
        .build();

    UjtWriteConfigCommand command = new UjtWriteConfigCommand(bleConfiguration);

    stateMachineContext.configureSubscription = tag.enqueue(command)
        .observe(new ObservesNext<Response>() {
          @Override
          public void onNext(@NonNull Response response) {
            handleEvent(ConnectionEvent.ofTagConfigured(tag));
          }

          @Override
          public void onError(@NonNull Throwable t) {
            PrintLogger.e(TAG, "Failed to configure tag", t);
            handleEvent(
                ConnectionEvent.ofConnectionError(JacquardError.ofJacquardInitializationError(t)));
          }
        });
  }

  /** Starts the connection process. */
  public void connect(boolean isUserInitiated) {
    PrintLogger.d(TAG, "connect isUserInitiated: " + isUserInitiated);
    this.isUserInitiated.set(isUserInitiated);
    stateSignal.next(ofPreparingToConnect());
    TagPairingStateMachine tagPairingStateMachine = new TagPairingStateMachine();
    connect(tagPairingStateMachine);
  }

  /**
   * Starts the connection process.
   *
   * @param tagPairingStateMachine state machine for managing state during the pairing phase
   */
  void connect(TagPairingStateMachine tagPairingStateMachine) {
    PrintLogger.d(TAG, "connect");
    // Reset state for new connection call.
    state = ofPreparingToConnect();
    stateMachineContext.childStateMachine = tagPairingStateMachine;
    if (stateMachineContext.childStateMachineSubscription != null) {
      stateMachineContext.childStateMachineSubscription.unsubscribe();
    }
    stateMachineContext.childStateMachineSubscription = tagPairingStateMachine.getState()
        .observe(new ObservesNext<TagPairingState>() {
          @Override
          public void onNext(@NonNull TagPairingState state) {
            PrintLogger.d(TAG, "TagPairingState: " + state);
            switch (state.getType()) {
              case DISCONNECTED:
                break;
              case PREPARING_TO_CONNECT:
              case BLUETOOTH_CONNECTED:
              case SERVICES_DISCOVERED:
              case AWAITING_NOTIFICATION_UPDATES:
                handleEvent(ConnectionEvent.ofConnectionProgress());
                break;
              case TAG_PAIRED:
                handleEvent(ConnectionEvent.ofTagPaired(state.tagPaired()));
                break;
              case ERROR:
                handleEvent(ConnectionEvent.ofConnectionError(state.error()));
                break;
            }
          }
        });
    connectMethod.run(device);
  }

  /** Updates the internal state and emits the update to observers. */
  private void updateState(ConnectionState state) {
    PrintLogger.d(TAG, "updateState:" + state);
    this.state = state;
    stateSignal.next(this.state);
  }

  @Override
  public void destroy() {
    PrintLogger.d(TAG,"destroy # ");
    stateMachineContext.childStateMachineSubscription.unsubscribe();
    stateMachineContext.configureSubscription.unsubscribe();
  }

  private boolean badFirmwareCheck(Revision currentVersion) {
    return badFirmwareVersions != null && badFirmwareVersions.contains(currentVersion);
  }

  /** Holds context for the state machine. */
  private static class StateMachineContext {
    private StateMachine<?, ConnectState> childStateMachine = new EmptyChildStateMachine();
    private Subscription childStateMachineSubscription = new Subscription();
    private Subscription configureSubscription = new Subscription();
  }

  private static class EmptyChildStateMachine implements StateMachine<Void, ConnectState> {

    @Override
    public Signal<Void> getState() {
      return Signal.empty();
    }

    @Override
    public void destroy() {
      PrintLogger.d(TAG, "EmptyChildStateMachine destroy");
      // Empty
    }

    @Override
    public void onStateEvent(ConnectState state) {
      PrintLogger.d(TAG, "EmptyChildStateMachine onConnectStateEvent");
      // Empty
    }
  }
}
