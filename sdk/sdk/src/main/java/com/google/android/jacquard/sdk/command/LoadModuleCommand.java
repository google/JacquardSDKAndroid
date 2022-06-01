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

import com.google.android.jacquard.sdk.connection.Result;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.Module;
import com.google.android.jacquard.sdk.util.StringUtils;
import com.google.atap.jacquard.protocol.JacquardProtocol;

/**
 * Ujt command to load/activate the loadable module. When module is loaded, you will be notified as
 * {@link LoadModuleNotificationSubscription}.
 */
public class LoadModuleCommand extends ProtoCommandRequest<Boolean> {

  private final JacquardProtocol.ModuleDescriptor moduleDescriptor;

  public LoadModuleCommand(@NonNull Module module) {
    StringUtils utils = StringUtils.getInstance();
    moduleDescriptor =
        JacquardProtocol.ModuleDescriptor.newBuilder()
            .setVendorId(utils.hexStringToInteger(module.vendorId()))
            .setProductId(utils.hexStringToInteger(module.productId()))
            .setModuleId(utils.hexStringToInteger(module.moduleId()))
            .build();
  }

  @Override
  public JacquardProtocol.Request getRequest() {
    JacquardProtocol.LoadModuleRequest request =
        JacquardProtocol.LoadModuleRequest.newBuilder().setModule(moduleDescriptor).build();
    return JacquardProtocol.Request.newBuilder()
        .setComponentId(Component.TAG_ID)
        .setId(getId())
        .setOpcode(JacquardProtocol.Opcode.LOAD_MODULE)
        .setDomain(JacquardProtocol.Domain.BASE)
        .setExtension(JacquardProtocol.LoadModuleRequest.loadModule, request)
        .build();
  }

  @Override
  public Result<Boolean> parseResponse(byte[] protoResponse) {
    return Result.ofSuccess(true);
  }
}
