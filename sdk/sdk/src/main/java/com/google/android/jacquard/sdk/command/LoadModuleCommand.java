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
import com.google.android.jacquard.sdk.connection.CommandResponseStatus;
import com.google.android.jacquard.sdk.connection.Result;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.Module;
import com.google.android.jacquard.sdk.util.StringUtils;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.LoadModuleRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.ModuleDescriptor;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.atap.jacquard.protocol.JacquardProtocol.Status;

/**
 * Ujt command to load/activate the loadable module. When module is loaded, you will be notified as
 * {@link LoadModuleNotificationSubscription}.
 */
public class LoadModuleCommand implements CommandRequest<Response> {

  private final ModuleDescriptor moduleDescriptor;
  private final StringUtils utils = StringUtils.getInstance();

  public LoadModuleCommand(@NonNull Module module) {
    moduleDescriptor = ModuleDescriptor.newBuilder()
        .setVendorId(utils.hexStringToInteger(module.vendorId()))
        .setProductId(utils.hexStringToInteger(module.productId()))
        .setModuleId(utils.hexStringToInteger(module.moduleId()))
        .build();
  }

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
    LoadModuleRequest request = LoadModuleRequest.newBuilder()
        .setModule(moduleDescriptor)
        .build();
    return Request.newBuilder()
        .setComponentId(Component.TAG_ID)
        .setId(0)
        .setOpcode(Opcode.LOAD_MODULE)
        .setDomain(Domain.BASE)
        .setExtension(LoadModuleRequest.loadModule, request)
        .build();
  }
}
