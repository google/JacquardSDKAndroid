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
 */

package com.google.android.jacquard.sdk.initialization;

import static com.google.atap.jacquard.protocol.JacquardProtocol.Opcode.DATA_COLLECTION_TRIAL_LIST;

import android.util.Pair;
import com.google.android.jacquard.sdk.connection.CommandResponseStatus;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.FakeDCTrialListNotification;
import com.google.android.jacquard.sdk.model.FakeImuModule;
import com.google.android.jacquard.sdk.model.Peripheral;
import com.google.android.jacquard.sdk.model.Peripheral.WriteType;
import com.google.android.jacquard.sdk.pairing.RequiredCharacteristics;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.atap.jacquard.protocol.JacquardProtocol.BatteryStatusResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.BleConfiguration;
import com.google.atap.jacquard.protocol.JacquardProtocol.ChargingStatus;
import com.google.atap.jacquard.protocol.JacquardProtocol.ConfigElement;
import com.google.atap.jacquard.protocol.JacquardProtocol.ConfigGetRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.ConfigGetResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.ConfigSetRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.DFUStatusResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.DFUWriteResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionEraseAllDataRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionEraseTrialDataRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionMode;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionStartRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionStartResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionStatus;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionStatusRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionStatusResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionStopRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionStopResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionTrialDataResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionTrialListNotification;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionTrialListRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionTrialListResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.DeviceInfoResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.ErrorNotification;
import com.google.atap.jacquard.protocol.JacquardProtocol.ImuAccelRange;
import com.google.atap.jacquard.protocol.JacquardProtocol.ImuConfiguration;
import com.google.atap.jacquard.protocol.JacquardProtocol.ImuGyroRange;
import com.google.atap.jacquard.protocol.JacquardProtocol.ListModuleResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.LoadModuleNotification;
import com.google.atap.jacquard.protocol.JacquardProtocol.Notification;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.atap.jacquard.protocol.JacquardProtocol.Status;
import com.google.atap.jacquard.protocol.JacquardProtocol.UJTConfigResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Fake implementation of {@link TransportImpl}.
 */
public class FakeTransportImpl extends TransportImpl {

  private static final String TAG_RENAME = "sample-sdk-ujt";
  private static final int MAX_IMU_PACKET_SIZE = 508;
  /**
   * Default timeout duration.
   */
  private static final int DEFAULT_TIMEOUT = 8000; // 8 seconds
  private boolean assertFailure;
  private boolean shouldThrowError;
  private boolean shouldThrowErrorDfuPrepare, shouldThrowErrorDfuWrite, shouldThrowErrorDfuStatus;
  private Response dfuStatusResponse, dfuWriteResponse;
  private int batteryLevel = 10;
  private boolean isModulePresent, isModuleActive;
  private Map<String, ConfigElement> configValues = new HashMap<>();
  private DataCollectionStatus dcStatus = DataCollectionStatus.DATA_COLLECTION_IDLE;

  public FakeTransportImpl(Peripheral peripheral,
      RequiredCharacteristics characteristics,
      TransportState transportState) {
    super(peripheral, characteristics, transportState);
    setDFUStatusResponse(
        DFUStatusResponse.newBuilder().setFinalSize(0).setFinalCrc(0).setCurrentSize(0)
            .setComponent(Component.TAG_ID).setCurrentCrc(0).build());
  }

  @Override
  public Signal<Response> enqueue(Request request, WriteType writeType, int retries) {
    return enqueue(request, writeType, retries, DEFAULT_TIMEOUT);
  }

  @Override
  public Signal<Response> enqueue(Request request, WriteType writeType, int retries, long timeout) {
    return Signal.create(responseSignal -> {
      switch (request.getDomain()) {
        case DFU:
          if (request.getOpcode() == Opcode.DFU_STATUS) {
            sendDfuStatusResponse(responseSignal, dfuStatusResponse);
          } else if (request.getOpcode() == Opcode.DFU_PREPARE) {
            sendDfuPrepareResponse(responseSignal);
          } else if (request.getOpcode() == Opcode.DFU_WRITE) {
            sendDfuWriteResponse(responseSignal, dfuWriteResponse);
          } else if (request.getOpcode() == Opcode.DFU_EXECUTE) {
            sendDeviceInfoResponse(responseSignal);
          }
          break;
        case DATA_COLLECTION:
          switch (request.getOpcode()) {
            case WEARSTATE: { // DATA_COLLECTION_DATA_ERASE
              sendEraseAllImuSessionsResponse(request, responseSignal);
              break;
            }
            case ACTIVITY: { // DATA_COLLECTION_TRIAL_DATA_ERASE
                sendEraseImuSessionResponse(request, responseSignal);
                break;
            }
            case DEVICEINFO: { // DATA_COLLECTION_TRIAL_LIST
              sendGetImuSessionListResponse(request, responseSignal);
              break;
            }
            case HELLO: { // DATA_COLLECTION_START
              dcStatus = DataCollectionStatus.DATA_COLLECTION_LOGGING;
              sendDataCollectionStartResponse(request, responseSignal);
              break;
            }
            case BEGIN: { // DATA_COLLECTION_STOP
              dcStatus = DataCollectionStatus.DATA_COLLECTION_IDLE;
              sendDataCollectionStopResponse(request, responseSignal);
              break;
            }
            case DISCONNECT: { // DATA_COLLECTION_STATUS
              sendDataCollectionStatusResponse(request, responseSignal);
              break;
            }
            case GESTURE: { //DATA_COLLECTION_TRIAL_DATA
              sendDataCollectionGetSessionDataResponse(request, responseSignal);
              Signal.from(1).delay(1000).onNext(ignore -> sendImuSessionData());
              break;
            }
          }
          break; // End of Data Collection
        case BASE:
          switch (request.getOpcode()) {
            case CONFIG_WRITE:
              sendConfigWriteResponse(responseSignal);
              break;
            case CONFIG_READ:
              sendConfigReadResponse(responseSignal);
              break;
            case DEVICEINFO:
              sendDeviceInfoResponse(responseSignal);
              break;
            case LIST_MODULES:
              sendListModuleResponse(responseSignal);
              break;
            case LOAD_MODULE:
              sendLoadModuleResponse(responseSignal);
              break;
            case UNLOAD_MODULE:
              sendUnloadModuleResponse(responseSignal);
              break;
            case BATTERY_STATUS:
              sendDeviceBatteryResponse(responseSignal);
              break;
            case CONFIG_GET:
              sendConfigGetResponse(request, responseSignal);
              break;
            case CONFIG_SET:
              ConfigSetRequest req = request.getExtension(ConfigSetRequest.configSetRequest);
              configValues.put(getConfigKey(req.getVid(), req.getPid(), req.getConfig().getKey()),
                  req.getConfig());
              sendConfigSetResponse(responseSignal);
              break;
          }
          break;
        case GEAR:
          if (request.getOpcode() == Opcode.GEAR_DATA) {
            sendSetTouchModeResponse(responseSignal);
          } else if (request.getOpcode() == Opcode.DEVICEINFO) {
            sendDeviceInfoResponse(responseSignal);
          }
          break;
      }
      // Send out dummy responses.
      return new Subscription();
    });
  }

  private String getConfigKey(int vid, int pid, String key) {
    return vid + "#" + pid + "#" + key;
  }

  private void sendImuSessionData(){
    byte[] data = new byte[MAX_IMU_PACKET_SIZE];
    try {
      File imuSession = new File(this.getClass().getClassLoader()
          .getResource("1627344030.bin").toURI());
      FileInputStream inputStream = new FileInputStream(imuSession);
      int count = 0;
      while (inputStream.read(data) > 0) {
        dataTransport.next(Pair.create(count++, data));
        int available = inputStream.available();
        if (available < MAX_IMU_PACKET_SIZE) {
          data = new byte[available];
        }
      }
    } catch (URISyntaxException | IOException e) {
      e.printStackTrace();
    }
  }

  private void sendImuSamples() {
    // Length = 18
    byte[] sample = new byte[]{-64, 0, 2, 23, 4, 50, 12, 22, -8, -33, 4, 99, -11, 23, -45, -21, 34,
        12};
    rawData.next(sample);
  }

  public Signal<Notification> getNotifySignal() {
    return notificationSignal;
  }

  public void assertCommandFailure(boolean assertFailure) {
    this.assertFailure = assertFailure;
  }

  public void throwError(){
    shouldThrowError = true;
  }

  public void throwErrorDfuStatus() {
    shouldThrowErrorDfuStatus = true;
  }

  public void throwErrorDfuPrepare() {
    shouldThrowErrorDfuPrepare = true;
  }

  public void throwErrorDfuWrite() {
    shouldThrowErrorDfuWrite = true;
  }

  public void setDFUStatusResponse(DFUStatusResponse dfuStatusResponse) {
    this.dfuStatusResponse = Response.newBuilder()
        .setExtension(DFUStatusResponse.dfuStatus, dfuStatusResponse).setId(1).setComponentId(1)
        .setStatus(Status.STATUS_OK).build();
  }

  public void setDfuWriteResponse(DFUWriteResponse dfuWriteResponse) {
    this.dfuWriteResponse = Response.newBuilder()
        .setExtension(DFUWriteResponse.dfuWrite, dfuWriteResponse)
        .setId(1).setComponentId(1).setStatus(Status.STATUS_OK).build();
  }

  @Override
  public void requestConnectionPriority(int priority) {
    //Empty
  }

  public void setBatteryLevel(int batteryLevel) {
    this.batteryLevel = batteryLevel;
  }

  public void setModulePresent(boolean present) {
    isModulePresent = present;
  }

  public void setModuleActive(boolean active) {
    isModuleActive = active;
  }

  private void sendListModuleResponse(Signal<Response> responseSignal) {
    ListModuleResponse.Builder builder = ListModuleResponse.newBuilder();
    if (isModulePresent) {
      builder.addModules(FakeImuModule.getModuleDescriptor(isModuleActive));
    }
    Response response = Response.newBuilder()
        .setExtension(ListModuleResponse.listModules, builder.build()).setId(1)
        .setComponentId(1).setStatus(Status.STATUS_OK).build();
    responseSignal.next(response);
  }

  private void sendLoadModuleResponse(Signal<Response> responseSignal) {
    Response response = Response.newBuilder().setId(3)
        .setComponentId(1).setStatus(Status.STATUS_OK).build();
    responseSignal.next(response);
    sendLoadModuleNotification();
  }

  private void sendDataCollectionGetSessionDataResponse(Request request,
      Signal<Response> responseSignal) {
    DataCollectionTrialDataResponse dataResponse = DataCollectionTrialDataResponse.newBuilder()
        .setDcStatus(DataCollectionStatus.DATA_COLLECTION_IDLE)
        .build();
    Response response = Response.newBuilder()
        .setExtension(DataCollectionTrialDataResponse.trialData, dataResponse).setId(1)
        .setComponentId(1).setStatus(Status.STATUS_OK).build();
    responseSignal.next(response);
  }

  private void sendUnloadModuleResponse(Signal<Response> responseSignal) {
    Response response = Response.newBuilder().setId(3)
        .setComponentId(1).setStatus(Status.STATUS_OK).build();
    responseSignal.next(response);
  }

  private void sendLoadModuleNotification() {
    LoadModuleNotification loadModuleNotification = LoadModuleNotification
        .newBuilder()
        .setModule(FakeImuModule.getModuleDescriptor(false))
        .build();

    Notification notification = Notification.newBuilder().setOpcode(Opcode.ATTACHED)
        .setDomain(Domain.BASE)
        .setExtension(LoadModuleNotification.loadModuleNotif, loadModuleNotification).build();

    getNotifySignal().next(notification);
  }

  private void sendEraseAllImuSessionsResponse(Request request, Signal<Response> responseSignal) {
    if(!request.hasExtension(DataCollectionEraseAllDataRequest.eraseAllData)) {
      throw new IllegalStateException("Invalid request.");
    }
    Response response = Response.newBuilder().setId(3)
        .setComponentId(1).setStatus(Status.STATUS_OK).build();
    responseSignal.next(response);

    sendEraseDoneNotification();
  }

  private void sendEraseImuSessionResponse(Request request, Signal<Response> responseSignal) {
    if(!request.hasExtension(DataCollectionEraseTrialDataRequest.eraseTrialData)) {
      throw new IllegalStateException("Invalid request.");
    }
    Response response = Response.newBuilder().setId(3)
        .setComponentId(1).setStatus(Status.STATUS_OK).build();
    responseSignal.next(response);

    sendEraseDoneNotification();
  }

  private void sendEraseDoneNotification() {
    ErrorNotification errorNotification = ErrorNotification.newBuilder()
        .setDevice(1)
        .setSubsystem(1)
        .setErrorcode(DataCollectionStatus.DATA_COLLECTION_IDLE.getNumber()).build();
    Notification notification = Notification.newBuilder().setOpcode(Opcode.DATA_COLLECTION_STATUS)
        .setDomain(Domain.DATA_COLLECTION)
        .setExtension(ErrorNotification.errorNotification, errorNotification).build();

    getNotifySignal().next(notification);
  }

  private void sendGetImuSessionListResponse(Request request, Signal<Response> responseSignal) {
    if (!request.hasExtension(DataCollectionTrialListRequest.trialList)) {
      throw new IllegalStateException("Invalid request.");
    }
    Response response = Response.newBuilder().setId(3)
        .setExtension(DataCollectionTrialListResponse.trialList, DataCollectionTrialListResponse.getDefaultInstance())
        .setComponentId(1).setStatus(Status.STATUS_OK).build();
    responseSignal.next(response);
    sendImuSessionListNotification();
  }

  private void sendImuSessionListNotification() {
    Notification notification = Notification.newBuilder().setOpcode(DATA_COLLECTION_TRIAL_LIST)
        .setDomain(Domain.BASE)
        .setExtension(DataCollectionTrialListNotification.trialList,
            FakeDCTrialListNotification.getDCTrialListNotification())
        .build();
    getNotifySignal().next(notification);
  }

  private void sendDataCollectionStartResponse(Request request, Signal<Response> responseSignal) {
    if (!request.hasExtension(DataCollectionStartRequest.start)) {
      throw new IllegalStateException("Invalid request.");
    }
    DataCollectionStartResponse startResponse = DataCollectionStartResponse.newBuilder()
        .setDcStatus(DataCollectionStatus.DATA_COLLECTION_LOGGING)
        .build();
    Response response = Response.newBuilder().setId(3)
        .setExtension(DataCollectionStartResponse.start, startResponse)
        .setComponentId(1).setStatus(Status.STATUS_OK).build();
    responseSignal.next(response);
    DataCollectionMode mode = request.getExtension(DataCollectionStartRequest.start).getMetadata().getMode();
    if (mode.equals(DataCollectionMode.DATA_COLLECTION_MODE_STREAMING)) {
      sendImuSamples();
    }
  }

  private void sendDataCollectionStopResponse(Request request, Signal<Response> responseSignal) {
    if (!request.hasExtension(DataCollectionStopRequest.stop)) {
      throw new IllegalStateException("Invalid request.");
    }
    DataCollectionStopResponse stopResponse = DataCollectionStopResponse.newBuilder()
        .setDcStatus(DataCollectionStatus.DATA_COLLECTION_IDLE)
        .build();
    Response response = Response.newBuilder().setId(3)
        .setExtension(DataCollectionStopResponse.stop, stopResponse)
        .setComponentId(1).setStatus(Status.STATUS_OK).build();
    responseSignal.next(response);
  }

  private void sendDataCollectionStatusResponse(Request request, Signal<Response> responseSignal) {
    if (!request.hasExtension(DataCollectionStatusRequest.status)) {
      throw new IllegalStateException("Invalid request.");
    }
    DataCollectionStatusResponse statusResponse = DataCollectionStatusResponse.newBuilder()
        .setDcStatus(dcStatus)
        .build();
    Response response = Response.newBuilder().setId(3)
        .setExtension(DataCollectionStatusResponse.status, statusResponse)
        .setComponentId(1).setStatus(Status.STATUS_OK).build();
    responseSignal.next(response);
  }

  private void sendDfuStatusResponse(Signal<Response> signal, Response response) {
    if (shouldThrowErrorDfuStatus) {
      signal.error(new Exception());
      return;
    }
    signal.next(response);
  }

  private void sendDfuPrepareResponse(Signal<Response> signal) {
    if (shouldThrowErrorDfuPrepare) {
      signal.error(new Exception());
      return;
    }
    Response rr = Response.newBuilder().setId(2).setComponentId(1).setStatus(Status.STATUS_OK)
        .build();
    signal.next(rr);
  }

  private void sendDfuWriteResponse(Signal<Response> signal, Response response) {
    if (shouldThrowErrorDfuWrite){
      signal.error(new Exception());
      return;
    }
    Response rr = null;
    if (response == null) {
      DFUWriteResponse dfuWriteResponse = DFUWriteResponse.newBuilder().setCrc(39686).setOffset(4)
          .build();
      rr = Response.newBuilder()
          .setExtension(DFUWriteResponse.dfuWrite, dfuWriteResponse).setId(3)
          .setComponentId(1).setStatus(Status.STATUS_OK).build();
    } else {
      rr = response;
    }
    signal.next(rr);
  }

  private void sendConfigReadResponse(Signal<Response> signal) {
    UJTConfigResponse ujtConfigResponse = UJTConfigResponse.newBuilder()
        .setBleConfig(BleConfiguration.newBuilder().setCustomAdvName(TAG_RENAME).build())
        .setImuConfig(ImuConfiguration.newBuilder().setAccelRange(ImuAccelRange.IMU_ACCEL_RANGE_16G)
            .setGyroRange(ImuGyroRange.IMU_GYRO_RANGE_2000DPS).build()).build();
    Response rr = Response.newBuilder()
        .setExtension(UJTConfigResponse.configResponse, ujtConfigResponse).setId(1)
        .setComponentId(1).setStatus(
            assertFailure ? Status.ERROR_UNKNOWN : Status.STATUS_OK).build();
    signal.next(rr);
  }

  private void sendConfigSetResponse(Signal<Response> responseSignal) {
    Response rr = Response.newBuilder().setId(1)
        .setComponentId(1).setStatus(Status.STATUS_OK).build();
    responseSignal.next(rr);
  }

  private void sendConfigGetResponse(Request request, Signal<Response> responseSignal) {
    ConfigGetRequest req = request.getExtension(ConfigGetRequest.configGetRequest);
    String key = getConfigKey(req.getVid(), req.getPid(), req.getKey());
    ConfigElement element = configValues.get(key);
    if (element == null) {
      responseSignal.error(new Exception(CommandResponseStatus.ERROR_APP_UNKNOWN.toString()));
    } else {
      ConfigGetResponse getResponse = ConfigGetResponse.newBuilder()
          .setConfig(element)
          .build();
      Response rr = Response.newBuilder()
          .setExtension(ConfigGetResponse.configGetResponse, getResponse).setId(1)
          .setComponentId(1).setStatus(Status.STATUS_OK).build();
      responseSignal.next(rr);
    }
  }

  private void sendSetTouchModeResponse(Signal<Response> signal) {
    Response rr = Response.newBuilder().setId(1)
        .setComponentId(1).setStatus(
            assertFailure ? Status.ERROR_UNKNOWN : Status.STATUS_OK).build();
    signal.next(rr);
  }

  private void sendConfigWriteResponse(Signal<Response> signal) {
    sendConfigReadResponse(signal);
  }

  private void sendDeviceBatteryResponse(Signal<Response> signal) {
    BatteryStatusResponse response = BatteryStatusResponse.newBuilder()
        .setBatteryLevel(batteryLevel)
        .setChargingStatus(
            ChargingStatus.NOT_CHARGING).build();
    Response rr = Response.newBuilder().setId(1)
        .setComponentId(1).setStatus(
            assertFailure ? Status.ERROR_UNKNOWN : Status.STATUS_OK)
        .setExtension(BatteryStatusResponse.batteryStatusResponse, response).build();
    signal.next(rr);
  }

  private void sendDeviceInfoResponse(Signal<Response> signal) {
    if (shouldThrowError) {
      signal.error(new Exception());
      return;
    }
    DeviceInfoResponse deviceInfoResponse = DeviceInfoResponse.newBuilder().setBootloaderMajor(1)
        .setBootloaderMinor(0).setBootloaderPoint(0).setFirmwareMajor(1).setFirmwareMinor(99)
        .setFirmwarePoint(0).setModel("UJT").setProductId(-1973006092).setRevision(5)
        .setUuid("0-07-9A16FLHBN005JM-1910").setVendor("Google Inc.").setVendorId(1957219924)
        .build();
    Response response = Response
        .newBuilder()
        .setComponentId(0)
        .setId(4)
        .setStatus(Status.STATUS_OK)
        .setExtension(DeviceInfoResponse.deviceInfo, deviceInfoResponse)
        .build();
    signal.next(response);
  }
}