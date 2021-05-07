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
import com.google.atap.jacquard.protocol.JacquardProtocol.ModuleDescriptor;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.atap.jacquard.protocol.JacquardProtocol.Status;
import com.google.atap.jacquard.protocol.JacquardProtocol.UnloadModuleRequest;

/**
 * Ujt command to unload/deactivate the loadable module.
 */
public class UnloadModuleCommand implements CommandRequest<Boolean> {

  private final ModuleDescriptor moduleDescriptor;

  public UnloadModuleCommand(@NonNull Module module) {
    StringUtils utils = StringUtils.getInstance();
    moduleDescriptor = ModuleDescriptor.newBuilder()
        .setVendorId(utils.hexStringToInteger(module.vendorId()))
        .setProductId(utils.hexStringToInteger(module.productId()))
        .setModuleId(utils.hexStringToInteger(module.moduleId()))
        .build();
  }

  @Override
  public Result<Boolean> parseResponse(Response response) {
    if (response.getStatus() != Status.STATUS_OK) {
      Throwable error = CommandResponseStatus.from(response.getStatus().getNumber());
      return Result.ofFailure(error);
    }
    return Result.ofSuccess(true);
  }

  @Override
  public Request getRequest() {
    UnloadModuleRequest request = UnloadModuleRequest.newBuilder()
        .setModule(moduleDescriptor)
        .build();
    return Request.newBuilder()
        .setComponentId(Component.TAG_ID)
        .setId(0)
        .setOpcode(Opcode.UNLOAD_MODULE)
        .setDomain(Domain.BASE)
        .setExtension(UnloadModuleRequest.unloadModule, request)
        .build();
  }
}