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

import androidx.annotation.NonNull;

import com.google.android.jacquard.sdk.JqExtensionRegistry;
import com.google.android.jacquard.sdk.connection.Result;
import com.google.atap.jacquard.protocol.JacquardProtocol;
import com.google.atap.jacquard.protocol.JacquardProtocol.ConfigElement;
import com.google.atap.jacquard.protocol.JacquardProtocol.ConfigGetRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.ConfigGetResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.protobuf.GeneratedMessageLite;
import com.google.protobuf.InvalidProtocolBufferException;

/** Reads {@link ConfigElement} from tag. */
public class GetConfigCommand extends ProtoCommandRequest<Object> {

  private final int vid;
  private final int pid;
  private final String key;

  public GetConfigCommand(int vid, int pid, @NonNull String key) {
    this.vid = vid;
    this.pid = pid;
    this.key = key;
  }

  @Override
  public GeneratedMessageLite.GeneratedExtension<Response, ConfigGetResponse> getExtension() {
    return ConfigGetResponse.configGetResponse;
  }

  /**
   * Parses the raw response received from the tag into a {@link Result} object.
   *
   * @param respByte the response received from the tag
   * @return a Result with either the parsed data or an error.
   */
  @Override
  public Result<Object> parseResponse(byte[] respByte) {
    try {
      JacquardProtocol.Response response =
          JacquardProtocol.Response.parseFrom(respByte, JqExtensionRegistry.instance);
      ConfigGetResponse configGetResponse = response.getExtension(getExtension());
      return Result.ofSuccess(fetchConfigFromResponse(configGetResponse));
    } catch (InvalidProtocolBufferException e) {
      return Result.ofFailure(e);
    }
  }

  /** Returns the request to be sent to the tag. */
  @Override
  public Request getRequest() {
    ConfigGetRequest request =
        ConfigGetRequest.newBuilder().setVid(vid).setPid(pid).setKey(key).build();
    return Request.newBuilder()
        .setComponentId(0)
        .setId(getId())
        .setOpcode(Opcode.CONFIG_GET)
        .setDomain(Domain.BASE)
        .setExtension(ConfigGetRequest.configGetRequest, request)
        .build();
  }

  private Object fetchConfigFromResponse(ConfigGetResponse response) {
    ConfigElement config = response.getConfig();
    if (config.hasBoolVal()) {
      return config.getBoolVal();
    } else if (config.hasDoubleVal()) {
      return config.getDoubleVal();
    } else if (config.hasFloatVal()) {
      return config.getFloatVal();
    } else if (config.hasInt32Val()) {
      return config.getInt32Val();
    } else if (config.hasInt64Val()) {
      return config.getInt64Val();
    } else if (config.hasUint32Val()) {
      return config.getUint32Val();
    } else if (config.hasUint64Val()) {
      return config.getUint64Val();
    } else if (config.hasStringVal()) {
      return config.getStringVal();
    }
    return null;
  }
}
