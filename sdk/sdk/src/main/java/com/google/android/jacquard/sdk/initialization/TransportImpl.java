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
package com.google.android.jacquard.sdk.initialization;

import androidx.core.util.Pair;
import com.google.android.jacquard.sdk.command.ProtoCommandRequest;
import com.google.android.jacquard.sdk.connection.Result;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.CharacteristicUpdate;
import com.google.android.jacquard.sdk.model.Peripheral;
import com.google.android.jacquard.sdk.model.Peripheral.WriteType;
import com.google.android.jacquard.sdk.pairing.RequiredCharacteristics;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.util.BluetoothSig;
import com.google.android.jacquard.sdk.util.JQUtils;
import com.google.atap.jacquard.protocol.JacquardProtocol.AttachedNotification;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/** Concrete implementation of {@link Transport}. */
class TransportImpl implements Transport {

  private static final int RSSI_DURATION_MS = 1000;
  private static final int RSSI_DELAY_MS = 1000;
  private final String TAG;
  private final Signal<CharacteristicUpdate> valueWrittenSignal = Signal.create();
  private final Peripheral peripheral;
  private final RequiredCharacteristics characteristics;
  private final TransportState transportState;
  private final Queue<PendingRequest> pendingRequests = new ConcurrentLinkedQueue<>();
  private final Signal<byte[]> commandResponses = Signal.create();
  private final Signal<Byte> ackSignal = Signal.create();
  private byte[] pendingAttachNotification;
  private boolean shouldCacheAttachNotification = true;
  final Signal<byte[]> notificationSignal = Signal.create();
  final Signal<Pair<Integer, byte[]>> dataTransport = Signal.create();
  final Signal<byte[]> rawData = Signal.create();
  private Signal<Integer> valueRssiSignal;
  private Timer rssiTimer;
  private ProtoCommandRequest inFlight;

  /**
   * Constructs a new TransportImpl class.
   *
   * @param peripheral the peripheral to transport data to and from
   * @param characteristics a set of characteristics that the peripheral supports
   * @param transportState state of connection
   */
  public TransportImpl(Peripheral peripheral, RequiredCharacteristics characteristics,
      TransportState transportState) {
    this.peripheral = peripheral;
    this.characteristics = characteristics;
    this.transportState = transportState;
    TAG = TransportImpl.class.getSimpleName() + "[" + peripheral.getDefaultDisplayName() + "]";
  }

  @Override
  public String getPeripheralIdentifier() {
    return peripheral.getTagIdentifier();
  }

  @Override
  public String getDefaultDisplayName() {
    return peripheral.getDefaultDisplayName();
  }

  @Override
  public Signal<byte[]> getNotificationSignal() {
    return Signal.create(signal -> {
      sendPendingNotification(signal);
      return notificationSignal.forward(signal);
    });
  }

  @Override
  public <ProtoRequest extends ProtoCommandRequest<?>> Signal<byte[]> enqueue(
      ProtoRequest request, WriteType writeType, int retries) {
    PrintLogger.d(TAG, "enqueue: " + request);
    return enqueue(request, writeType, retries, DEFAULT_TIMEOUT);
  }

  @Override
  public <ProtoRequest extends ProtoCommandRequest<?>> Signal<byte[]> enqueue(
      ProtoRequest request, WriteType writeType, int retries, long timeout) {
    PrintLogger.d(TAG, "enqueue: " + request);
    return Signal.create(
        signal -> {
          pendingRequests.add(new PendingRequest<>(request, writeType, retries, signal, timeout));
          sendNextRequest();
          return new Subscription();
        });
  }

  @Override
  public void characteristicUpdated(CharacteristicUpdate characteristicUpdate) {
    UUID uuid = characteristicUpdate.characteristic().getUuid();
    byte[] data = characteristicUpdate.characteristic().getValue();
    PrintLogger.d(TAG, "characteristicUpdated # " + uuid);
    data = data == null ? new byte[0] : data; // Data may be null
    if (BluetoothSig.RESPONSE_UUID.equals(uuid)) {
      onResponseUpdate(data);
      return;
    }
    if (BluetoothSig.NOTIFY_UUID.equals(uuid)) {
      onNotifyUpdate(data);
      return;
    }
    if(BluetoothSig.RAW_UUID.equals(uuid)) {
      PrintLogger.d(TAG, "Received data on raw chars");
      onDataReceived(data);
      return;
    }
    PrintLogger.d(TAG, "Got unexpected notification:" + uuid);
  }

  @Override
  public void valueWritten(CharacteristicUpdate characteristicUpdate) {
    PrintLogger.d(TAG, "valueWritten for " + characteristicUpdate.characteristic().getUuid());
    valueWrittenSignal.next(characteristicUpdate);
  }

  @Override
  public int getPendingRequestSize() {
    return pendingRequests.size();
  }

  @Override
  public Signal<CharacteristicUpdate> getValueWrittenSignal() {
    return valueWrittenSignal;
  }

  @Override
  public void requestConnectionPriority(int priority) {
    peripheral.requestConnectionPriority(priority);
  }

  @Override
  public void onRSSIValueUpdated(int rssiValue) {
    if (valueRssiSignal.hasObservers()) {
      valueRssiSignal.next(rssiValue);
    } else {
      stopRSSITimer();
    }
  }

  @Override
  public Signal<Integer> fetchRSSIValue() {
    if (valueRssiSignal == null || valueRssiSignal.isComplete()) {
      valueRssiSignal = Signal.create();
    }
    requestRssi();
    return valueRssiSignal;
  }

  @Override
  public void stopRSSIValue() {
    if (valueRssiSignal != null) {
      valueRssiSignal.complete();
    }
    stopRSSITimer();
  }

  @Override
  public Signal<Pair<Integer, byte[]>> getDataTransport() {
    return dataTransport.scan(
        Pair.create(-1, new byte[0]),
        (acc, next) ->
            Pair.create(
                next.first, acc.first.equals(next.first) ? new byte[0] : next.second))
        .filter(x -> x.second.length > 0);
  }

  @Override
  public Signal<byte[]> getRawData() {
    return rawData;
  }

  @Override
  public Signal<Byte> getAckSignal() {
    return ackSignal;
  }

  @Override
  public Signal<Boolean> sendData(byte[] data) {
    List<byte[]> partials = transportState.dataFragmenter.fragmentData(data);
    if (partials == null) {
      return Signal.from(false);
    }
    for (byte[] fragment : partials) {
      peripheral.writeCharacteristic(characteristics.rawCharacteristic,
              WriteType.WITH_RESPONSE, fragment);
    }
    return Signal.from(true);
  }

  private void stopRSSITimer() {
    PrintLogger.d(TAG, "stopRSSITimer");
    if (rssiTimer != null) {
      PrintLogger.d(TAG, "Canceled RSSI timer.");
      rssiTimer.cancel();
      rssiTimer = null;
    }
  }

  private void requestRssi() {
    if (rssiTimer != null) {
      PrintLogger.d(TAG, "Rssi timer is already running.");
      return;
    }
    rssiTimer = new Timer();
    rssiTimer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        peripheral.requestRssi();
      }
    }, RSSI_DELAY_MS, RSSI_DURATION_MS);
  }

  /** Emits the pending notification on signal. */
  private void sendPendingNotification(Signal<byte[]> signal) {
    if (pendingAttachNotification == null) {
      return;
    }
    PrintLogger.d(TAG, "sending pending notification");
    shouldCacheAttachNotification = false;
    signal.next(pendingAttachNotification);
    pendingAttachNotification = null;
  }

  private synchronized void sendNextRequest() {
    if (inFlight != null || pendingRequests.isEmpty()) {
      return;
    }
    PendingRequest<?,?> pendingRequest = pendingRequests.element();
    inFlight = pendingRequest.request;
    sendRequest(pendingRequest).onError(error -> {
      if (pendingRequests.isEmpty()) {
        PrintLogger.d(TAG, "pendingRequests is empty");
        return;
      }
      PendingRequest<?,?> request = pendingRequests.peek();
      if (request.retries > 0) {
        PrintLogger.d(TAG, "Retrying sending request");
        request.retries--;
        inFlight = null;
        sendNextRequest();
      } else {
        PrintLogger.d(TAG, "Skipping request since we're not able to send it");
        request.response.error(error);
        iterateNextRequest();
      }
    });
  }

  private Signal<byte[]> sendRequest(PendingRequest<?,?> pendingRequest) {
    AtomicReference<Result> response = new AtomicReference<>();
    return Signal.create(signal -> {
      try {
        PrintLogger.d(TAG, "Sending Proto request: " + inFlight);
        byte[] packet = inFlight.getRequest().toByteArray();
        List<byte[]> fragments = transportState.commandFragmenter.fragmentData(packet);
        for (byte[] fragment : fragments) {
          peripheral.writeCharacteristic(characteristics.commandCharacteristic,
              pendingRequest.writeType, fragment);
        }
        signal.next(1);
        signal.complete();
      } catch (Exception e) {
        PrintLogger.e(TAG, "Sending request failed", e);
        if (pendingRequest.retries > 0) {
          PrintLogger.d(TAG, "Retrying sending request");
          pendingRequest.retries--;
          inFlight = null;
          sendNextRequest();
        } else {
          PrintLogger.d(TAG, "Skipping request since we're not able to send it");
          signal.error(e);
        }
      }
      return new Subscription();
    }).flatMap(ignore -> commandResponses.filter(respPacket -> {
      // this is needed here as responseErrorCheck() sets respId in pendingRequest.request
      Result result = inFlight.responseErrorCheck(respPacket);
      response.set(result);
      if (inFlight != null) {
        // This is a rare condition where data from ujt can not be parsed.
        // The request will fail as the pendingRequest.request.responseId() is not found.
        if (result.getType() == Result.Type.FAILURE
            && result.failure() instanceof InvalidProtocolBufferException) {
          PrintLogger
              .d(TAG, "Response id not found due to parsing error.");
        }
        PrintLogger
            .d(TAG, "response id: " + pendingRequest.request.responseId() + " inFlight id: " + inFlight.getId());
        return pendingRequest.request.responseId() == inFlight.getId();
      } else {
        PrintLogger.d(TAG, "inFlight object is null.");
        return false;
      }
    }).first().tap(respPacket -> {
        PrintLogger.d(TAG, "response: " + response.get());
        if (response.get().getType() == Result.Type.FAILURE) {
          pendingRequest.response.error(response.get().failure());
        } else {
          pendingRequest.response.next(respPacket);
        }
      iterateNextRequest();
    }).timeout(pendingRequest.timeout)) // Send the time out if ujt not responded back with in 8 seconds.
    .tapError(Throwable::printStackTrace);
  }

  private void deliverPacket(byte[] packet) {
    PrintLogger.d(TAG, "deliverPacket");
    if (inFlight != null) {
      commandResponses.next(packet);
    } else {
      PrintLogger.d(
          TAG, "deliverPacket: This can happen if the tag re-sends a response, so silently drop");
    }
  }

  private void iterateNextRequest(){
    // pendingRequests.remove() will throw NPE if Queue is empty.
    // Should never throw NPE in ideal case.
    pendingRequests.remove();
    inFlight = null;
    sendNextRequest();
  }

  private void deliverNotification(byte[] packet) {
    PrintLogger.d(TAG, "deliverNotification");
    cacheAttachNotification(packet);
    notificationSignal.next(packet);
  }

  private void onResponseUpdate(byte[] data) {
    PrintLogger.d(TAG, "onResponseUpdate");
    if (data.length == 0) {
      PrintLogger.e(TAG, "onResponseUpdate with empty data");
      return;
    }
    byte[] packet = transportState.commandFragmenter.decodeFragment(data);
    if (packet == null) {
      return;
    }
    deliverPacket(packet);
  }

  private void onNotifyUpdate(byte[] data) {
    PrintLogger.d(TAG, "onNotifyUpdate");
    if (data.length == 0) {
      PrintLogger.e(TAG, "onNotifyUpdate with empty data");
      return;
    }
    byte[] packet = transportState.notificationFragmenter.decodeFragment(data);
    if (packet == null) {
      return;
    }
    deliverNotification(packet);
  }

  /**
   * Handle Data Packet received over Raw chars.
   */
  private void onDataReceived(byte[] data) {
    PrintLogger.d(TAG, "Data Received on raw chars # " + data.length + Arrays.toString(data));
    if (data.length == 0) {
      PrintLogger.e(TAG, "onDataUpdate with empty data");
      return;
    }
    if (rawData.hasObservers()) {
      PrintLogger.d(TAG, "Sending data on rawChars #");
      rawData.next(data);
    }
    if (!dataTransport.hasObservers() && !ackSignal.hasObservers()) {
      PrintLogger.d(TAG, "There is no observer for DataTransport & AckSignal. Ignoring #");
      return;
    }
    byte[] packet = transportState.dataFragmenter.decodeFragment(data);
    if (packet == null) {
      return;
    }
    PrintLogger.d(TAG, "Fragmented Packet # " + packet.length + Arrays.toString(packet));
    if (isAckPacket(packet)) {
      PrintLogger.d(TAG, "Ack received for packet number # " + packet[0]);
      ackSignal.next(packet[0]);
    } else if (isDataPacket(packet)) {
      byte[] dataReceived = new byte[packet.length - 1];
      System.arraycopy(packet, 1, dataReceived, 0, packet.length - 1);
      PrintLogger.d(TAG, "Data packet received.");
      dataTransport.next(Pair.create((int) packet[0], dataReceived));
      ackDataPacket(packet[0]);
    }
  }

  private boolean isAckPacket(byte[] packet) {
    return packet!= null && packet.length == 2 && packet[1] == (byte) 'A';
  }

  private boolean isDataPacket(byte[] packet) {
    return packet!= null && packet.length != 2 && packet[1] != (byte) 'A';
  }

  private void ackDataPacket(byte sequenceNumber) {
    byte[] ack = new byte[]{sequenceNumber, (byte) 'A'};
    List<byte[]> fragments = transportState.dataFragmenter.fragmentData(ack);
    for (byte[] fragment : fragments) {
      PrintLogger.d(TAG,
          "Sending ACK # for Sequence no # " + sequenceNumber + " # " + Arrays.toString(fragment));
      peripheral.writeCharacteristic(characteristics.rawCharacteristic,
          WriteType.WITH_RESPONSE, fragment);
    }
  }

  // Attach notification are emitted immediately after connecting so to avoid loosing the
  // notification we cache the notification and emit when subscribing to notifications.
  // TODO: Need to move cacheAttachNotification logic into ConnectedJacquardTagImpl class.
  private void cacheAttachNotification(byte[] packet) {
    if (!shouldCacheAttachNotification
        || JQUtils.getNotification(packet) == null
        || !JQUtils.getNotification(packet).hasExtension(AttachedNotification.attached)) {
      return;
    }
    pendingAttachNotification = packet;
  }
}
