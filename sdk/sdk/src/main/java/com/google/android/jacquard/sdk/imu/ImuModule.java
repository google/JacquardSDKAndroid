/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sdk.imu;

import static com.google.android.jacquard.sdk.connection.ConnectionState.Type.CONNECTED;
import static com.google.atap.jacquard.protocol.JacquardProtocol.ImuFilterMode.IMU_FILTER_NORMAL;

import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.google.android.jacquard.sdk.JacquardManager;
import com.google.android.jacquard.sdk.command.DCErrorNotificationSubscription;
import com.google.android.jacquard.sdk.command.DataCollectionStatusCommand;
import com.google.android.jacquard.sdk.command.EraseImuSessionCommand;
import com.google.android.jacquard.sdk.command.GetConfigCommand;
import com.google.android.jacquard.sdk.command.GetImuSessionDataCommand;
import com.google.android.jacquard.sdk.command.ImuSessionListCommand;
import com.google.android.jacquard.sdk.command.ImuSessionListNotification;
import com.google.android.jacquard.sdk.command.SetConfigCommand;
import com.google.android.jacquard.sdk.command.SetConfigCommand.SettingsType;
import com.google.android.jacquard.sdk.command.StartImuSessionCommand;
import com.google.android.jacquard.sdk.command.StartImuStreamingCommand;
import com.google.android.jacquard.sdk.command.StopImuSessionCommand;
import com.google.android.jacquard.sdk.command.UjtReadConfigCommand;
import com.google.android.jacquard.sdk.command.UjtWriteConfigCommand;
import com.google.android.jacquard.sdk.connection.ConnectionState;
import com.google.android.jacquard.sdk.imu.exception.InprogressDCException;
import com.google.android.jacquard.sdk.imu.exception.InvalidStateDCException;
import com.google.android.jacquard.sdk.imu.model.ImuSessionInfo;
import com.google.android.jacquard.sdk.imu.model.ImuStream;
import com.google.android.jacquard.sdk.imu.parser.ImuParser;
import com.google.android.jacquard.sdk.imu.parser.ImuParserException;
import com.google.android.jacquard.sdk.imu.parser.ImuParserImpl;
import com.google.android.jacquard.sdk.imu.parser.ImuSessionData;
import com.google.android.jacquard.sdk.imu.parser.ImuSessionData.ImuSampleCollection;
import com.google.android.jacquard.sdk.imu.parser.JQImuParser;
import com.google.android.jacquard.sdk.lm.LoadableModule;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.DeviceConfigElement;
import com.google.android.jacquard.sdk.model.Module;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTag;
import com.google.android.jacquard.sdk.util.FileLogger;
import com.google.android.jacquard.sdk.util.StringUtils;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionMetadata;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionMode;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataCollectionStatus;
import com.google.atap.jacquard.protocol.JacquardProtocol.ImuAccelRange;
import com.google.atap.jacquard.protocol.JacquardProtocol.ImuAccelSampleRate;
import com.google.atap.jacquard.protocol.JacquardProtocol.ImuConfiguration;
import com.google.atap.jacquard.protocol.JacquardProtocol.ImuGyroRange;
import com.google.atap.jacquard.protocol.JacquardProtocol.ImuGyroSampleRate;
import com.google.atap.jacquard.protocol.JacquardProtocol.Status;
import com.google.atap.jacquard.protocol.JacquardProtocol.UJTConfigResponse;
import com.google.protos.atap.jacquard.core.Jacquard.ImuSample;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * IMU loadable module can be accessed using this class.
 */
public class ImuModule extends LoadableModule {

  /**
   * File extension for downloaded Imu session.
   */
  public static final String SESSION_FILE_EXTENSION = ".bin";
  private static final String TAG = ImuModule.class.getSimpleName();
  private static final String VENDOR_ID = "11-78-30-c8";
  private static final String PRODUCT_ID = "ef-3e-5b-88";
  private static final String MODULE_ID = "3d-0b-e7-53";
  private static final StringUtils stringUtils = StringUtils.getInstance();
  private static final Module IMU_MODULE = Module.create(
      stringUtils.hexStringToInteger(VENDOR_ID), stringUtils.hexStringToInteger(PRODUCT_ID),
      stringUtils.hexStringToInteger(MODULE_ID));
  /**
   * Erasing imu session(s) may take longer time.
   */
  private static final long ERASE_TIMEOUT_DURATION = 60 * 1000; // 60 seconds
  private static final String CURRENT_SESSION_TS = "current_imu_session_ts";
  private static final String DC_MODE = "current_dc_mode";
  private Subscription dataTransportSubscription = null;
  private Subscription disconnectedSubscription = null;
  private final List<Subscription> subscriptions = new ArrayList<>();

  public ImuModule(ConnectedJacquardTag tag) {
    super(tag);
  }

  /**
   * Starts IMU Session.
   *
   * @return sessionId if successful.
   */
  public Signal<String> startImuSession() {
    return preconditionToStartImuCollection(null)
        .flatMap(ignore -> enqueue(new StartImuSessionCommand()))
        .flatMap(sessionId ->
            setCurrentSessionIdToTag(sessionId).map(config -> sessionId))
        .flatMap(sessionId -> setDCMode(DataCollectionMode.DATA_COLLECTION_MODE_STORE.getNumber())
            .map(config -> sessionId));
  }

  /**
   * Reads current session id stored on the tag. Returns empty string if no session id found.
   */
  public Signal<String> getCurrentSessionId() {
    return Signal.create(signal -> {
      getConfig(CURRENT_SESSION_TS).observe(value -> signal.next(value.toString()), error -> {
        if (error != null) {
          PrintLogger.w(TAG, "Current Session Id not found");
          signal.next("0");
        }
      });
      return new Subscription();
    });
  }

  /**
   * Returns current {@link DataCollectionMode} and null if mode is not found or error occurred.
   */
  public Signal<DataCollectionMode> getCurrentDataCollectionMode() {
    return Signal.create(signal -> {
      getConfig(DC_MODE).observe(value -> {
        if (value != null) {
          signal.next(DataCollectionMode.forNumber(Integer.parseInt(value.toString())));
        } else {
          signal.next(null);
        }
      }, error -> {
        if (error != null) {
          signal.next(null);
        }
      });
      return new Subscription();
    });
  }

  /**
   * Starts collecting Imu samples.
   *
   * @return Stream of {@link ImuStream}
   */
  public Signal<ImuStream> startImuStreaming() {
    return startImuStreaming(getMetadata(), null);
  }

  /**
   * Starts collecting Imu samples with provided {@link DataCollectionMetadata}
   *
   * @return Stream of {@link ImuStream}
   */
  public Signal<ImuStream> startImuStreaming(@NonNull DataCollectionMetadata metadata,
                                             @Nullable ImuConfiguration imuConfig) {
    return Signal.create(
        signal -> {
          disconnectedSubscription =
                  getTagDisconnectionSignal().onNext(
                  disconnected -> {
                    signal.error(new IllegalStateException("Tag disconnected."));
                    disconnectedSubscription.unsubscribe();
                  });
          subscriptions.add(disconnectedSubscription);

          String userId = String.format(Locale.US, "u-%d", System.currentTimeMillis());
          // Trigger
          Subscription outer =
              isImuStreamingInProgress()
                  .flatMap(
                      isInProgress -> {
                        if (isInProgress) {
                          return getImuStream(metadata, userId);
                        } else {
                          return preconditionToStartImuCollection(imuConfig)
                              .flatMap(ignore -> enqueue(new StartImuStreamingCommand(metadata)))
                              .filter(result -> result)
                              .flatMap(
                                  ignore ->
                                      setDCMode(
                                          DataCollectionMode.DATA_COLLECTION_MODE_STREAMING
                                              .getNumber()))
                              .flatMap(ignore -> getImuStream(metadata, userId));
                        }
                      })
                  .forward(signal);

          return new Subscription() {
            @Override
            protected void onUnsubscribe() {
              PrintLogger.d(TAG, "Unsubscribe Imu Streaming #");
              if (outer != null) {
                outer.unsubscribe();
              }
              if (disconnectedSubscription != null) {
                disconnectedSubscription.unsubscribe();
              }
            }
          };
        });
  }

  /**
   * Stops imu streaming.
   */
  public Signal<Boolean> stopImuStreaming() {
    return stopImuCollection();
  }

  /** Stops currently active imu session. */
  public Signal<Boolean> stopImuSession() {
    // DC LM 0.17.0, STOP DC can also be used to stop downloading of Imu session.
    return stopImuCollection();
  }

  /**
   * Gives you current imu data collection status.
   */
  public Signal<DataCollectionStatus> getDataCollectionStatus() {
    return enqueue(new DataCollectionStatusCommand());
  }

  /**
   * Returns a list of imu sessions present on ujt.
   */
  public Signal<List<ImuSessionInfo>> getImuSessionsList() {
    return Signal.create(signal -> {
      List<ImuSessionInfo> sessionList = new ArrayList<>();
      // Subscribe to notifications.
      Subscription subscription = getMyJacquardTag()
          .flatMap(jacquardTag -> jacquardTag.subscribe(new ImuSessionListNotification()))
          .onNext(notification -> {
            PrintLogger.d(TAG,
                "Total Sessions # " + notification.getTrialIndex() + " / " + notification
                    .getTotalTrials());
            if (notification.getTotalTrials() != 0) {
              sessionList.add(ImuSessionInfo.of(notification));
            }
            if (notification.getTotalTrials() == 0
                || notification.getTrialIndex() + 1 == notification.getTotalTrials()) {
              PrintLogger.d(TAG, "End of IMU session list.");
              signal.next(sessionList);
              signal.complete();
            }
          });

      // Trigger
      enqueue(new ImuSessionListCommand())
          .map(isSuccess -> {
            if (!isSuccess) {
              return Signal
                  .empty(new IllegalStateException("ImuSessionListCommand failed."));
            }
            return true;
          })
          .onError(error -> {
            if (error != null) {
              signal.error(error);
            }
          });

      return new Subscription() {
        @Override
        protected void onUnsubscribe() {
          PrintLogger.d(TAG, "getImuSessionsList ## unsubscribe ");
          subscription.unsubscribe();
          signal.complete();
        }
      };
    });
  }

  /**
   * Starts downloading of imu session.
   *
   * @return Pair<Progress, File> pair.first : Download progress. pair.second : File object when
   * progress is 100%
   */
  public Signal<Pair<Integer, File>> downloadImuData(String sessionId) {
    return getImuSession(sessionId).flatMap(sessionInfo -> {
      if (sessionInfo == null) {
        return Signal
            .empty(new IllegalStateException("Imu Session not found. Session id # " + sessionId));
      }
      return downloadImuData(sessionInfo);
    });
  }

  /**
   * Starts downloading of imu session.
   *
   * @return Pair<Progress, File> pair.first : Download progress. pair.second : File object when
   * progress is 100%
   */
  public Signal<Pair<Integer, File>> downloadImuData(ImuSessionInfo info) {
    PrintLogger.d(TAG, "Requesting Trial Data for # " + info.toString());
    Signal<Pair<Integer, byte[]>> transporter = Signal.create();
    Signal<Pair<Integer, File>> progress = Signal.create(signal -> new Subscription() {
      @Override
      protected void onUnsubscribe() {
        PrintLogger.d(TAG, "onUnsubscribe # download # ");
        if (dataTransportSubscription != null) {
          dataTransportSubscription.unsubscribe();
          dataTransportSubscription = null;
        }
        if (disconnectedSubscription != null) {
          disconnectedSubscription.unsubscribe();
        }
        setConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED);
      }
    });

    FileLogger dataFile = new FileLogger(
        getContext().getCacheDir().getAbsolutePath() + "/Sessions/" + tagSerialNumber() + "/",
        info.imuSessionId() + SESSION_FILE_EXTENSION);

    if (Objects.requireNonNull(dataFile.getFile()).length() == info.imuSize()) {
      return Signal.just(Pair.create(100, dataFile.getFile()));
    }
    disconnectedSubscription = getTagDisconnectionSignal()
        .onNext(disconnected -> {
          PrintLogger.d(TAG, "Ujt disconnected # downloaded till # " + dataFile.getFile().length());
          progress.error(new IllegalStateException("Tag disconnected."));
          disconnectedSubscription.unsubscribe();
        });
    subscriptions.add(disconnectedSubscription);
    // Trigger
    checkIfActiveSession().flatMap(activeSessionFound -> {
      if (activeSessionFound) {
        throw new IllegalStateException("Ujt has active imu session. Can't proceed.");
      }
      return Signal.from(false);
    }).flatMap(ignore ->
        getMyJacquardTag().flatMap(tag -> {
          dataTransportSubscription = tag.getDataTransport().forward(transporter);
          tag.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
          subscriptions.add(dataTransportSubscription);
          // Given some extra time to dc lm to finish the process if running.
          return Signal.from(1).delay(3000).flatMap(_ignore -> tag.enqueue(
              new GetImuSessionDataCommand(info, (int) dataFile.getFile().length())));
        })).tapError(progress::error)
        .consume();

    // Process
    int expectedBytes = info.imuSize();
    AtomicInteger received = new AtomicInteger((int) dataFile.getFile().length());
    transporter
        .onNext(
            rawImuBytes -> {
              dataFile.log(rawImuBytes.second);
              received.addAndGet(rawImuBytes.second.length);
              PrintLogger
                  .d(TAG, "Data Received till now # " + received.get() + " / " + expectedBytes);
              progress
                  .next(Pair.create(received.get() * 100 / expectedBytes, dataFile.getFile()));
              if (received.get() == expectedBytes) {
                dataFile.done();
                progress.complete();
                transporter.complete();
                // Reset Connection Priority
                setConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED);
              }
            });
    return progress;
  }

  /**
   * Parses raw imu samples file.
   */
  public static Signal<ImuSessionData> parseImuData(@NonNull final String path) {
    PrintLogger.d(TAG, "Parsing # " + path);
    return Signal.create(signal -> {
      JQImuParser reader = new JQImuParser(new ImuParserImpl());
      try {
        ImuSessionData trialData = reader.parseImuData(path);
        String id = trialData.getImuSessionId();
        PrintLogger.d(TAG, "Parsed Trial id # " + id);
        List<ImuSampleCollection> samples = trialData.getImuSampleCollections();
        PrintLogger.d(TAG, "Parsed ImuSampleCollections # " + samples.size());
        for (ImuSampleCollection g : samples) {
          List<ImuSample> imuSamples = g.getImuSamples();
          PrintLogger.d(TAG, "Parsed Imu Samples # " + imuSamples.size());
          for (ImuSample s : imuSamples) {
            PrintLogger.d(TAG, "Imu # " + s);
          }
        }
        signal.next(trialData);
      } catch (IOException | ImuParserException e) {
        e.printStackTrace();
        signal.error(e);
      }
      return new Subscription();
    });
  }

  /**
   * Erase imu session.
   */
  public Signal<Boolean> erase(@NonNull ImuSessionInfo selectedTrialData) {
      return Signal.create(
              signal -> {
              AtomicReference<Subscription> notification = new AtomicReference<>();
              Subscription command =
                      checkIfActiveSession()
                      .flatMap(
                          activeSessionFound -> {
                              if (activeSessionFound) {
                                  return Signal.empty(
                                      new IllegalStateException(
                                              "Ujt has active imu session. Can't proceed."));
                              }
                              return Signal.from(false);
                          })
                      .flatMap(
                          ignore -> {
                              Subscription sub =
                                      getMyJacquardTag()
                                      .flatMap(tag ->
                                              tag.subscribe(new DCErrorNotificationSubscription()))
                                      .first()
                                      .map(dcStatus ->
                                         dcStatus.equals(DataCollectionStatus.DATA_COLLECTION_IDLE))
                                      .forward(signal);
                              notification.set(sub);
                              return enqueue(
                                      new EraseImuSessionCommand(selectedTrialData),
                                      /* retries= */ 0,
                                      ERASE_TIMEOUT_DURATION);
                          })
                          .onError(signal::error);
              return new Subscription() {
                  @Override
                  protected void onUnsubscribe() {
                      if (notification.get() != null) {
                          notification.get().unsubscribe();
                      }
                      if (command != null) {
                          command.unsubscribe();
                      }
                  }
              };
          });
  }

  /**
   * Erase imu session.
   */
  public Signal<Boolean> erase(@NonNull String sessionId) {
    return getImuSession(sessionId).flatMap(sessionInfo -> {
      if (sessionInfo == null) {
        return Signal
            .empty(new IllegalStateException("Imu Session not found. Session id # " + sessionId));
      }
      return erase(sessionInfo);
    });
  }

  /**
   * Erases all imu sessions present on ujt.
   */
  public Signal<Boolean> eraseAll() {
    return Signal.create(signal -> {
      Subscription notification = getMyJacquardTag()
          .flatMap(tag -> tag.subscribe(new DCErrorNotificationSubscription()))
          .first()
          .map(dcStatus -> dcStatus.equals(DataCollectionStatus.DATA_COLLECTION_IDLE))
          .forward(signal);

      Subscription command = checkIfActiveSession().flatMap(activeSessionFound -> {
        if (activeSessionFound) {
          return Signal
              .empty(new IllegalStateException("Ujt has active imu session. Can't proceed."));
        }
        return Signal.from(false);
      }).flatMap(
          ignore -> enqueue(new EraseImuSessionCommand(/* trialData= */null), /* retries= */ 0,
              ERASE_TIMEOUT_DURATION))
          .onError(signal::error);
      return new Subscription() {
        @Override
        protected void onUnsubscribe() {
          if (notification != null) {
            notification.unsubscribe();
          }
          if (command != null) {
            command.unsubscribe();
          }
        }
      };
    });
  }

  /**
   * Release resources.
   */
  public void destroy() {
    if (disconnectedSubscription != null) {
      disconnectedSubscription.unsubscribe();
    }
    for (Subscription s : subscriptions) {
      if (s != null) {
        s.unsubscribe();
      }
    }
    subscriptions.clear();
  }

  @Override
  public Module getVidPidMid() {
     return IMU_MODULE;
  }

  private Signal<Boolean> setCurrentSessionIdToTag(String sessionId) {
    return setConfig(CURRENT_SESSION_TS, sessionId);
  }

  private Signal<Boolean> resetFlags() {
    return setCurrentSessionIdToTag("0").flatMap(ignore -> setDCMode(-1));
  }

  private Signal<Boolean> setDCMode(int mode) {
    return setConfig(DC_MODE, String.valueOf(mode));
  }

  private Signal<ImuConfiguration> getImuConfig() {
    return enqueue(new UjtReadConfigCommand())
        .map(UJTConfigResponse::getImuConfig);
  }

  private Signal<Boolean> isImuStreamingInProgress() {
    return getDataCollectionStatus().flatMap(status -> {
      if (!DataCollectionStatus.DATA_COLLECTION_LOGGING.equals(status)) {
        return Signal.from(false);
      } else {
        return getCurrentDataCollectionMode()
            .map(DataCollectionMode.DATA_COLLECTION_MODE_STREAMING::equals);
      }
    });
  }

  private Signal<ImuStream> getImuStream(DataCollectionMetadata metadata, String userId) {
    return Signal.create(
        signal -> {
          ImuParser parser = new ImuParserImpl();
          AtomicReference<ImuConfiguration> imuConfig = new AtomicReference<>();
          Subscription outer =
              getImuConfig()
                  .flatMap(
                      imuConfiguration -> {
                        imuConfig.set(imuConfiguration);
                        return getMyJacquardTag();
                      })
                  .flatMap(ConnectedJacquardTag::getRawData)
                  .onNext(
                      data -> {
                        byte[] raw = new byte[data.length - 2];
                        System.arraycopy(data, 2, raw, 0, data.length - 2);
                        for (ImuSample sample : parser.parseImuSamples(raw)) {
                          signal.next(
                              ImuStream.of(
                                  sample,
                                  imuConfig.get(),
                                  Sensors.forId(data[1]),
                                  metadata,
                                  userId));
                        }
                      });
          return new Subscription() {
            @Override
            protected void onUnsubscribe() {
              outer.unsubscribe();
            }
          };
        });
  }

  private Signal<Boolean> setConfig(String key, String config) {
    PrintLogger.d(TAG, "SetConfig # " + key + " => " + config);
    DeviceConfigElement element = DeviceConfigElement
        .create(stringUtils.hexStringToInteger(VENDOR_ID),
            stringUtils.hexStringToInteger(PRODUCT_ID), key,
            SettingsType.STRING, config);
    return enqueue(new SetConfigCommand(element))
        .tap(result -> PrintLogger.d(TAG, "SetConfig Result # " + key + " => " + result));
  }

  private Signal<Object> getConfig(String key) {
    PrintLogger.d(TAG, "getConfig # " + key);
    return enqueue(new GetConfigCommand(stringUtils.hexStringToInteger(VENDOR_ID)
        , stringUtils.hexStringToInteger(PRODUCT_ID), key))
        .tap(result -> PrintLogger.d(TAG, "GetConfig Result # " + key + " => " + result))
        .tapError(Throwable::printStackTrace);
  }

  @Nullable
  private Signal<ImuSessionInfo> getImuSession(String sessionId) {
    return getImuSessionsList().map(sessionList -> {
      PrintLogger.d(TAG, "Session List Fetched  # " + sessionList.size());
      ImuSessionInfo target = null;
      for (ImuSessionInfo session : sessionList) {
        if (TextUtils.equals(session.imuSessionId(), sessionId)) {
          target = session;
          break;
        }
      }
      return target;
    });
  }

  private Signal<Boolean> stopImuCollection() {
    PrintLogger.d(TAG, "stopImuCollection #");
    return enqueue(new StopImuSessionCommand())
        .flatMap(ignore -> resetFlags());
  }

  private Signal<Boolean> checkIfActiveSession() {
    return getDataCollectionStatus()
        .map(status -> status.equals(DataCollectionStatus.DATA_COLLECTION_LOGGING));
  }

  private Signal<Boolean> preconditionToStartImuCollection(ImuConfiguration config) {
    return getDataCollectionStatus()
        .map(status -> {
          boolean isTagReady = status.equals(DataCollectionStatus.DATA_COLLECTION_IDLE)
              || status.getNumber() > DataCollectionStatus.DATA_COLLECTION_INVALID_STATE_VALUE;
          PrintLogger.d(TAG,
              "preconditionToStartImuSession # status # " + status + ", isTagReady for DC ? "
                  + isTagReady);
          return Pair.create(status, isTagReady); // DATA_COLLECTION_IDLE
        }).map(pair -> {
          DataCollectionStatus status = pair.first;
          boolean isTagReady = pair.second;
          if (isTagReady) {
            return true;
          } else {
            if (status.equals(DataCollectionStatus.DATA_COLLECTION_LOGGING)) {
              return getCurrentDataCollectionMode().map(mode -> {
                throw new InprogressDCException(status, mode);
              });
            } else {
              throw new InvalidStateDCException(status);
            }
          }
        })
        .flatMap(ignore -> setImuConfig(config == null ? getDefaultImuConfig() : config));
  }

  private ImuConfiguration getDefaultImuConfig() {
    return ImuConfiguration.newBuilder()
        .setAccelLowPowerMode(false)
        .setGyroLowPowerMode(false)
        .setAccelFilterMode(IMU_FILTER_NORMAL)
        .setGyroFilterMode(IMU_FILTER_NORMAL)
        .setAccelSampleRate(ImuAccelSampleRate.ACCEL_ODR_25_HZ)
        .setGyroSampleRate(ImuGyroSampleRate.GYRO_ODR_25_HZ)
        .setAccelRange(ImuAccelRange.IMU_ACCEL_RANGE_16G)
        .setGyroRange(ImuGyroRange.IMU_GYRO_RANGE_2000DPS)
        .setSensorId(Sensors.IMU.id()) // 0 = IMU, 1 = GEAR
        .build();
  }

  // Enqueue UjtWriteConfigCommand
  private Signal<Boolean> setImuConfig(ImuConfiguration config) {
    UjtWriteConfigCommand configCommand = new UjtWriteConfigCommand(config);
    return enqueue(configCommand)
            .map(response -> response.getStatus() == Status.STATUS_OK);
  }

  private Signal<ConnectionState> getTagDisconnectionSignal() {
    return JacquardManager.getInstance().getConnectionStateSignal(identifier)
        .dropWhile(state -> state.getType() == CONNECTED)
        .distinctUntilChanged();
  }

  private Context getContext() {
    return JacquardManager.getInstance().getApplicationContext();
  }

  private DataCollectionMetadata getMetadata() {
    String sessionId = String.valueOf(System.currentTimeMillis() / 1000);
    return DataCollectionMetadata.newBuilder()
        .setMode(DataCollectionMode.DATA_COLLECTION_MODE_STREAMING)
        .setCampaignId("jqCampaign")
        .setSessionId("jqSession")
        .setTrialId(sessionId)
        .setSubjectId("jqSession")
        .setProductId("jqProduct")
        .addSensorIds(Sensors.IMU.id()) // 0 : IMU
        .setSensorIds(0, Sensors.IMU.id())
        .setActionId(0)
        .build();
  }
}
