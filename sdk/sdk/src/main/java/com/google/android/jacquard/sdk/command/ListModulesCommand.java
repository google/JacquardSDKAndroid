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

import com.google.android.jacquard.sdk.JqExtensionRegistry;
import com.google.android.jacquard.sdk.connection.Result;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.Module;
import com.google.atap.jacquard.protocol.JacquardProtocol;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.ListModuleResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.ModuleDescriptor;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.protobuf.GeneratedMessageLite;
import com.google.protobuf.InvalidProtocolBufferException;

import java.util.ArrayList;
import java.util.List;

/**
 * Ujt command to fetch all {@link Module} present.
 */
public class ListModulesCommand extends ProtoCommandRequest<List<Module>> {


  @Override
  public GeneratedMessageLite.GeneratedExtension<Response, ListModuleResponse> getExtension() {
    return ListModuleResponse.listModules;
  }

  @Override
  public Result<List<Module>> parseResponse(byte[] respByte) {
    try {
      JacquardProtocol.Response response =
          JacquardProtocol.Response.parseFrom(respByte, JqExtensionRegistry.instance);
      ListModuleResponse listModuleResponse = response.getExtension(getExtension());
      List<Module> modules = new ArrayList<>();
      for (ModuleDescriptor descriptor : listModuleResponse.getModulesList()) {
        modules.add(Module.create(descriptor));
      }
      return Result.ofSuccess(modules);
    } catch (InvalidProtocolBufferException e) {
      return Result.ofFailure(e);
    }
  }

  @Override
  public Request getRequest() {
    return Request.newBuilder()
        .setComponentId(Component.TAG_ID)
        .setId(getId())
        .setOpcode(Opcode.LIST_MODULES)
        .setDomain(Domain.BASE)
        .build();
  }
}
