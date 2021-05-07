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
package com.google.android.jacquard.sdk.dfu;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import com.google.android.jacquard.sdk.BuildConfig;
import com.google.android.jacquard.sdk.JacquardManager;
import com.google.android.jacquard.sdk.dfu.DFUInfo.UpgradeStatus;
import com.google.android.jacquard.sdk.dfu.model.FileDescriptor;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.SdkConfig;
import com.google.android.jacquard.sdk.rx.Executors;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.util.StringUtils;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;
import com.squareup.moshi.Moshi;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.HttpException;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

class DFUChecker {

  static final String ENDPOINTBASE = BuildConfig.BASE_URL;
  private static final String TAG = DFUChecker.class.getSimpleName();

  CloudManager cloudManager;
  DownloadManager downloadManager;
  private final CacheRepository cacheRepository;
  private final String countryCode;
  private String apiKey;
  private String clientId;

  DFUChecker() {
    JacquardManager jacquardManager = JacquardManager.getInstance();
    downloadManager = new DownloadManagerImpl();
    cacheRepository = new CacheRepositoryImpl(jacquardManager.getApplicationContext());
    cloudManager = createRetrofit().create(CloudManager.class);
    countryCode = getCountryCode(jacquardManager.getApplicationContext());
    SdkConfig sdkConfig = jacquardManager.getSdkConfig();
    if (sdkConfig != null) {
      apiKey = sdkConfig.apiKey();
      clientId = sdkConfig.clientId();
    }
  }

  /**
   * Responsible to download the dfu update information, Download the firmware, cache the dfu update
   * information and Firmware file to internal memory.
   *
   * @param checkUpdateParamsList {@link CheckUpdateParams} parameters Pojo.
   * @return Signal which contains dfu information {@link DFUInfo}.
   */
  Signal<List<DFUInfo>> checkUpdate(List<CheckUpdateParams> checkUpdateParamsList,
      boolean forceUpdate) {
    return Signal.create(signal -> {
      checkUpdate(checkUpdateParamsList, 0, signal, new ArrayList<>(), forceUpdate);
      return new Subscription();
    });
  }

  /**
   * Call checkUpdate(CheckUpdateParams checkUpdateParams) method recursively and notify the update
   * on provided signal.
   */
  private void checkUpdate(List<CheckUpdateParams> checkUpdateParamsList, int indexPos,
      Signal<List<DFUInfo>> signal,
      List<DFUInfo> dfuInfos, boolean forceUpdate) {
    checkUpdate(checkUpdateParamsList.get(indexPos), forceUpdate).tapError(signal::error)
        .onNext(dfuInfo -> {
          dfuInfos.add(dfuInfo);
          if (indexPos + 1 == checkUpdateParamsList.size()) {
            signal.next(dfuInfos);
            signal.complete();
          } else {
            checkUpdate(checkUpdateParamsList, indexPos + 1, signal, dfuInfos, forceUpdate);
          }
        });
  }

  /** Check firmware update from cloud and download the firmware if available. */
  Signal<DFUInfo> checkUpdate(CheckUpdateParams checkUpdateParams, boolean forceUpdate) {
    if (TextUtils.isEmpty(apiKey) || TextUtils.isEmpty(clientId)) {
      return Signal.empty(new IllegalArgumentException(
          "Need to initialise SdkConfig with required data by calling JacquardManager#init"));
    }

    Optional<DFUInfo> optionalDfu = cacheRepository
        .getUpdateInformation(checkUpdateParams.vendorId(),
            checkUpdateParams.productId(),
            checkUpdateParams.moduleId(),
            checkUpdateParams.componentSerialNumber());

    if (!forceUpdate
        && optionalDfu.isPresent()) {
      PrintLogger.d(TAG, "DFU from cache");
      return Signal.just(optionalDfu.get());
    }

    return Signal.create(signal -> {
      Call<RemoteDfuInfo> call = cloudManager
          .getDeviceFirmware(clientId, checkUpdateParams.vendorId(), checkUpdateParams.productId(),
              checkUpdateParams.moduleId(), checkUpdateParams.componentVersion(),
              checkUpdateParams.tagVersion(), getObfuscatedComponentId(checkUpdateParams.moduleId(),
                  checkUpdateParams.componentSerialNumber()), "android", countryCode,
              BuildConfig.SDK_VERSION);

      asSignal(call)
          .observeOn(Executors.mainThreadExecutor())
          .map(DFUInfo::create)
          .tapError(signal::error)
          .onNext(dfu -> {
            if (dfu.dfuStatus().equals(UpgradeStatus.NOT_AVAILABLE)) {
              cacheRepository.cacheUpdateInformation(dfu,
                  checkUpdateParams.vendorId(),
                  checkUpdateParams.productId(),
                  checkUpdateParams.moduleId(),
                  checkUpdateParams.componentSerialNumber(),
                  Calendar.getInstance().getTimeInMillis());
              signal.next(dfu);
              signal.complete();
              return;
            }
            downloadFirmware(dfu)
                .observeOn(Executors.mainThreadExecutor())
                .map(ignore -> {
                  cacheRepository.cacheUpdateInformation(dfu,
                      checkUpdateParams.vendorId(),
                      checkUpdateParams.productId(),
                      checkUpdateParams.moduleId(),
                      checkUpdateParams.componentSerialNumber(),
                      Calendar.getInstance().getTimeInMillis());
                  signal.next(dfu);
                  signal.complete();
                  return true;
                }).tapError(signal::error).consume();
          });
      return new Subscription();
    });
  }

  /**
   * Returns FileDescriptor from cache.
   *
   * @param dfuInfo {@link DFUInfo} to fetch a file name.
   * @return {@link Optional<FileDescriptor>}.
   */
  Optional<FileDescriptor> getFirmwareStream(DFUInfo dfuInfo) {
    return cacheRepository.getDescriptor(dfuInfo);
  }

  void removeFirmware(DFUInfo dfuInfo, String componentSerialNumber) {
    cacheRepository
        .removeUpdateInformation(dfuInfo.vendorId(), dfuInfo.productId(), dfuInfo.moduleId(),
            componentSerialNumber);
  }

  private Signal<Boolean> downloadFirmware(DFUInfo dfu) {
    return downloadManager.download(dfu.downloadUrl().toString(), apiKey)
        .flatMap(fileDescriptor -> cacheRepository.cacheDescriptor(dfu, fileDescriptor));
  }

  private <T> Signal<T> asSignal(Call<T> call) {

    return Signal.create(signal -> {
      PrintLogger.d(TAG, "Consuming DFU API");
      call.enqueue(new Callback<T>() {
        @Override
        public void onResponse(Call<T> call, Response<T> response) {
          T body = response.body();

          if (body == null
              || !response.isSuccessful()) {
            HttpException httpException = new HttpException(response);
            signal.error(httpException);
            return;
          }

          signal.next(body);
          signal.complete();
        }

        @Override
        public void onFailure(Call<T> call, Throwable t) {
          signal.error(t);
        }
      });

      return new Subscription();
    });
  }

  private String getObfuscatedComponentId(String moduleId, String serialNumber) {

    StringUtils stringUtils = StringUtils.getInstance();

    // If moduleId is empty then component must be one of the Interposer or Tag.
    if (TextUtils.isEmpty(moduleId)) {
      // If moduleId exists then component must be loadable module.
      // Obfuscated component id for Tag.
      return stringUtils.sha1(serialNumber);
    }

    // Obfuscated component id for Module.
    return stringUtils.sha1("m:" + serialNumber + ":" + moduleId);
  }

  private Retrofit createRetrofit() {
    OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
    httpClient.addInterceptor(
        chain -> {
          Request original = chain.request();
          Request request =
              original
                  .newBuilder()
                  .header("X-Goog-Api-Key", apiKey)
                  .method(original.method(), original.body())
                  .build();
          return chain.proceed(request);
        });

    Moshi moshi = new com.squareup.moshi.Moshi.Builder().build();
    return new Retrofit.Builder()
        .baseUrl(ENDPOINTBASE)
        .client(httpClient.build())
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build();
  }

  /** Pojo class to hold Params for {@link DFUChecker}. */
  @AutoValue
  static abstract class CheckUpdateParams {

    static Builder builder() {
      return new AutoValue_DFUChecker_CheckUpdateParams.Builder();
    }

    abstract String vendorId();

    abstract String productId();

    @Nullable
    abstract String moduleId();

    abstract String componentSerialNumber();

    abstract String componentVersion();

    abstract String tagVersion();

    @AutoValue.Builder
    abstract static class Builder {

      abstract Builder vendorId(String vendorId);

      abstract Builder productId(String productId);

      abstract Builder moduleId(String moduleId);

      abstract Builder componentSerialNumber(String componentSerialNumber);

      abstract Builder componentVersion(String componentVersion);

      abstract Builder tagVersion(String tagVersion);

      abstract CheckUpdateParams build();
    }
  }

  private String getCountryCode(Context context) {
    Configuration configuration = context.getResources().getConfiguration();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      return configuration.getLocales().get(0).getCountry();
    } else {
      return configuration.locale.getCountry();
    }
  }
}
