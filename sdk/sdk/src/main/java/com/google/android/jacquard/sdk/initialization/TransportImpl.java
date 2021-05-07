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
package com.google.android.jacquard.sdk.initialization;

import android.util.Pair;
import com.google.android.jacquard.sdk.JqExtensionRegistry;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.CharacteristicUpdate;
import com.google.android.jacquard.sdk.model.Peripheral;
import com.google.android.jacquard.sdk.model.Peripheral.WriteType;
import com.google.android.jacquard.sdk.pairing.RequiredCharacteristics;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.util.BluetoothSig;
import com.google.atap.jacquard.protocol.JacquardProtocol.AttachedNotification;
import com.google.atap.jacquard.protocol.JacquardProtocol.Notification;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Concrete implementation of {@link Transport}. */
class TransportImpl implements Transport {

  private static final String TAG = TransportImpl.class.getSimpleName();
  private static final int RSSI_DURATION_MS = 1000;
  private static final int RSSI_DELAY_MS = 1000;
  private final Signal<CharacteristicUpdate> valueWrittenSignal = Signal.create();
  private final Peripheral peripheral;
  private final RequiredCharacteristics characteristics;
  private final TransportState transportState;
  private final Queue<PendingRequest> pendingRequests = new ConcurrentLinkedQueue<>();
  private final Signal<Response> commandResponses = Signal.create();

  private Request inFlight;
  private Notification pendingAttachNotification;
  private boolean shouldCacheAttachNotification = true;
  final Signal<Notification> notificationSignal = Signal.create();
  final Signal<Pair<Integer, byte[]>> dataTransport = Signal.create();
  private Signal<Integer> valueRssiSignal;
  private Timer rssiTimer;

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
  public Signal<Notification> getNotificationSignal() {
    return Signal.create(signal -> {
      sendPendingNotification(signal);
      return notificationSignal.forward(signal);
    });
  }

  @Override
  public Signal<Response> enqueue(Request request, WriteType writeType, int retries, long timeout) {
    PrintLogger.d(TAG, "enqueue: " + request);
    return Signal.create(signal -> {
      pendingRequests.add(new PendingRequest(request, writeType, retries, signal, timeout));
      sendNextRequest();
      return new Subscription();
    });
  }

  @Override
  public Signal<Response> enqueue(Request request, WriteType writeType, int retries) {
    PrintLogger.d(TAG, "enqueue: " + request);
    return enqueue(request, writeType, retries, DEFAULT_TIMEOUT);
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
  private void sendPendingNotification(Signal<Notification> signal) {
    if (pendingAttachNotification == null) {
      return;
    }
    PrintLogger.d(TAG, "sending pending notification");
    signal.next(pendingAttachNotification);
    pendingAttachNotification = null;
    shouldCacheAttachNotification = false;
  }

  private synchronized void sendNextRequest() {
    if (inFlight != null || pendingRequests.isEmpty()) {
      return;
    }
    PendingRequest pendingRequest = pendingRequests.element();
    inFlight = pendingRequest.request.toBuilder().setId(transportState.getNextRequest()).build();
    sendRequest(pendingRequest).onError(error -> {
      if (pendingRequests.isEmpty()) {
        PrintLogger.d(TAG, "pendingRequests is empty");
        return;
      }
      PendingRequest request = pendingRequests.peek();
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

  private Signal<Response> sendRequest(PendingRequest pendingRequest) {
    return Signal.create(signal -> {
      try {
        PrintLogger.d(TAG, "Sending request: " + inFlight);
        byte[] packet = inFlight.toByteArray();
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
    }).flatMap(ignore -> commandResponses.filter(response -> {
      PrintLogger
          .d(TAG, "response id: " + response.getId() + " inFlight id: " + inFlight.getId());
      return response.getId() == inFlight.getId();
    }).first().tap(response -> {
      pendingRequests.peek().response.next(response);
      iterateNextRequest();
    }).timeout(pendingRequest.timeout)) // Send the time out if ujt not responded back with in 8 seconds.
    .tapError(error -> error.printStackTrace());
  }

  private void deliverPacket(byte[] packet) {
    PrintLogger.d(TAG, "deliverPacket");
    if (inFlight == null) {
      // This can happen if the tag re-sends a response, so silently drop.
      return;
    }
    try {
      Response response = Response.parseFrom(packet, JqExtensionRegistry.instance);
      PrintLogger.d(TAG, "response: " + response);
      commandResponses.next(response);
    } catch (InvalidProtocolBufferException e) {
      PrintLogger.d(TAG, "Parsing Error: " + e.getMessage());
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
    try {
      Notification notification = Notification.parseFrom(packet, JqExtensionRegistry.instance);
      PrintLogger.d(TAG,
          "Notification Received # Domain # " + notification.getDomain() + " # OpCode # "
              + notification.getOpcode());
      PrintLogger.d(TAG,
          "Notification Received # " + notification);
      cacheAttachNotification(notification);
      notificationSignal.next(notification);
    } catch (Exception e) {
      PrintLogger.e(TAG, "Failed to deserialize notification", e);
    }
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
    if (!dataTransport.hasObservers()) {
      PrintLogger.d(TAG, "There is no observer for DataTransport. Ignoring #");
      return;
    }
    if (data.length == 0) {
      PrintLogger.e(TAG, "onDataUpdate with empty data");
      return;
    }
    byte[] packet = transportState.dataFragmenter.decodeFragment(data);
    if (packet == null) {
      return;
    }
    byte[] copy = new byte[packet.length - 1];
    System.arraycopy(packet, 1, copy, 0, packet.length - 1);
    PrintLogger.d(TAG, "Data chunk received length # " + copy.length);
    dataTransport.next(Pair.create((int) packet[0], copy));
    ackDataPacket(packet[0]);
  }

  private void ackDataPacket(byte sequenceNumber) {
    byte[] ack = new byte[]{sequenceNumber, (byte) 'A'};
    List<byte[]> fragments = transportState.commandFragmenter.fragmentData(ack);
    for (byte[] fragment : fragments) {
      PrintLogger.d(TAG,
          "Sending ACK # for Sequence no # " + sequenceNumber + " # " + Arrays.toString(fragment));
      peripheral.writeCharacteristic(characteristics.rawCharacteristic,
          WriteType.WITHOUT_RESPONSE, fragment);
    }
  }

  // Attach notification are emitted immediately after connecting so to avoid loosing the
  // notification we cache the notification and emit when subscribing to notifications.
  private void cacheAttachNotification(Notification notification) {
    if (!shouldCacheAttachNotification || !notification
        .hasExtension(AttachedNotification.attached)) {
      return;
    }
    pendingAttachNotification = notification;
  }
}
