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

package com.google.android.jacquard.sdk.command;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.google.android.jacquard.sdk.connection.CommandResponseStatus;
import com.google.android.jacquard.sdk.connection.Result;
import com.google.android.jacquard.sdk.model.DeviceConfigElement;
import com.google.atap.jacquard.protocol.JacquardProtocol.ConfigElement;
import com.google.atap.jacquard.protocol.JacquardProtocol.ConfigSetRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.atap.jacquard.protocol.JacquardProtocol.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Writes Key-Value pair to tag.
 */
public class SetConfigCommand implements CommandRequest<Boolean> {

  private final DeviceConfigElement config;

  public SetConfigCommand(@NonNull DeviceConfigElement config) {
    this.config = config;
  }

  /**
   * Parses the raw response received from the tag into a {@link Result} object.
   *
   * @param response the response received from the tag
   * @return a Result with either the parsed data or an error.
   */
  @Override
  public Result<Boolean> parseResponse(Response response) {
    if (response.getStatus() != Status.STATUS_OK) {
      Throwable error = CommandResponseStatus.from(response.getStatus().getNumber());
      return Result.ofFailure(error);
    }
    return Result.ofSuccess(true);
  }

  /**
   * Returns the request to be sent to the tag.
   */
  @Override
  public Request getRequest() {
    return Request
        .newBuilder()
        .setComponentId(0)
        .setId(0)
        .setOpcode(Opcode.CONFIG_SET)
        .setDomain(Domain.BASE)
        .setExtension(ConfigSetRequest.configSetRequest, getConfigSetRequest())
        .build();
  }

  private ConfigSetRequest getConfigSetRequest() {
    validate();
    ConfigElement.Builder builder = ConfigElement.newBuilder().setKey(config.key());
    switch (SettingsType.valueOf(config.type().name().toUpperCase(Locale.ENGLISH))) {
      case BOOL:
        builder.setBoolVal(Boolean.parseBoolean(config.value()));
        break;
      case UINT32:
        builder.setUint32Val(Integer.parseInt(config.value()));
        break;
      case UINT64:
        builder.setUint64Val(Long.parseLong(config.value()));
        break;
      case INT32:
        builder.setInt32Val(Integer.parseInt(config.value()));
        break;
      case INT64:
        builder.setInt64Val(Long.parseLong(config.value()));
        break;
      case FLOAT:
        builder.setFloatVal(Float.parseFloat(config.value()));
        break;
      case DOUBLE:
        builder.setDoubleVal(Double.parseDouble(config.value()));
        break;
      case STRING:
        builder.setStringVal(config.value());
        break;
    }
    return
        ConfigSetRequest.newBuilder()
            .setConfig(builder.build())
            .setVid(config.vendorId())
            .setPid(config.productId())
            .build();
  }

  private void validate() {
    List<String> errors = new ArrayList<>();
    if (TextUtils.isEmpty(config.key())) {
      errors.add("Key");
    }
    if (TextUtils.isEmpty(config.value())) {
      errors.add("Value");
    }
    if (!errors.isEmpty()) {
      throw new IllegalArgumentException(
          TextUtils.join(", ", errors).concat(" can not be empty or null"));
    }
  }

  /**
   * Data types for tag config values.
   */
  public enum SettingsType {
    BOOL,
    UINT32,
    UINT64,
    INT32,
    INT64,
    FLOAT,
    DOUBLE,
    STRING
  }
}
