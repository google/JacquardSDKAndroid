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
package com.google.android.jacquard.sdk.command;

import androidx.annotation.Nullable;
import com.google.android.jacquard.sdk.connection.CommandResponseStatus;
import com.google.android.jacquard.sdk.connection.Result;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.Component;
import com.google.atap.jacquard.protocol.JacquardProtocol.BleConfiguration;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.ImuConfiguration;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.atap.jacquard.protocol.JacquardProtocol.Status;
import com.google.atap.jacquard.protocol.JacquardProtocol.UJTConfigWriteRequest;

/**
 * Command for writing {@link BleConfiguration} to the tag.
 * <p>
 * A command can be send to a connected tag via {@link com.google.android.jacquard.sdk.tag.ConnectedJacquardTag#enqueue(CommandRequest)}
 */
public class UjtWriteConfigCommand implements CommandRequest<Response> {

  private static final String TAG = UjtWriteConfigCommand.class.getSimpleName();

  private final BleConfiguration bleConfiguration;
  private final ImuConfiguration imuConfiguration;

  /**
   * Creates a new instance of UjtWriteConfigCommand and when executed will apply the provided
   * {@link BleConfiguration} to the tag.
   *
   * @param configuration the configuration to write to the tag.
   */
  public UjtWriteConfigCommand(BleConfiguration configuration) {
    this(configuration, null);
  }

  public UjtWriteConfigCommand(ImuConfiguration imuConfiguration) {
    this(null, imuConfiguration);
  }

  /**
   * Creates a new instance of UjtWriteConfigCommand and when executed will apply the provided
   * {@link BleConfiguration} to the tag.
   *
   * @param bleConfiguration the configuration to write to the tag.
   */
  public UjtWriteConfigCommand(@Nullable BleConfiguration bleConfiguration,
      @Nullable ImuConfiguration imuConfiguration) {
    this.bleConfiguration = bleConfiguration;
    this.imuConfiguration = imuConfiguration;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Result<Response> parseResponse(Response response) {
    if (response.getStatus() != Status.STATUS_OK) {
      Throwable error = CommandResponseStatus.from(response.getStatus().getNumber());
      return Result.ofFailure(error);
    }
    return Result.ofSuccess(response);
  }

  @Override
  public Request getRequest() {
    if (bleConfiguration == null && imuConfiguration == null) {
      PrintLogger.w(TAG, "No configuration specified to write.");
    }
    UJTConfigWriteRequest.Builder builder = UJTConfigWriteRequest.newBuilder();
    if (bleConfiguration != null) {
      builder.setBleConfig(bleConfiguration);
    }
    if (imuConfiguration != null) {
      builder.setImuConfig(imuConfiguration);
    }
    return Request
        .newBuilder()
        .setComponentId(Component.TAG_ID)
        .setId(0)
        .setOpcode(Opcode.CONFIG_WRITE)
        .setDomain(Domain.BASE)
        .setExtension(UJTConfigWriteRequest.configWrite, builder.build())
        .build();
  }
}
