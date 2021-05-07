/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sdk.command;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build.VERSION_CODES;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.connection.Result;
import com.google.android.jacquard.sdk.datastore.DataProvider;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.Product;
import com.google.android.jacquard.sdk.model.Product.Capability;
import com.google.android.jacquard.sdk.model.TouchMode;
import com.google.android.jacquard.sdk.model.Vendor;
import com.google.android.jacquard.sdk.util.StringUtils;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataChannelRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.DataStreamState;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.atap.jacquard.protocol.JacquardProtocol.Status;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link SetTouchModeCommand} class.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public class SetTouchModeCommandTest {

  private SetTouchModeCommand command;
  private static TouchMode touchMode;
  private static Component component;

  @Before
  public void setup() {
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    DataProvider.create(getVendors(), StringUtils.getInstance());
    command = new SetTouchModeCommand(component, touchMode);
  }

  @Test
  public void getRequestForGestureTouchMode() {
    // Arrange
    touchMode = TouchMode.GESTURE;
    command = new SetTouchModeCommand(component, touchMode);
    // Act
    Request request = command.getRequest();
    // Assert
    assertThat(request.getComponentId()).isEqualTo(component.componentId());
    assertThat(request.getId()).isEqualTo(/*expected =*/ 0);
    assertThat(request.getOpcode()).isEqualTo(Opcode.GEAR_DATA);
    assertThat(request.getDomain()).isEqualTo(Domain.GEAR);
    assertThat(request.getExtension(DataChannelRequest.data).getTouch())
        .isEqualTo(DataStreamState.DATA_STREAM_DISABLE);
    assertThat(request.getExtension(DataChannelRequest.data).getInference())
        .isEqualTo(DataStreamState.DATA_STREAM_ENABLE);
    assertThat(request.getExtension(DataChannelRequest.data).getTouch().getNumber()).isEqualTo(0);
  }

  @Test
  public void getRequestForContinuousTouchMode() {
    // Arrange
    touchMode = TouchMode.CONTINUOUS;
    command = new SetTouchModeCommand(component, touchMode);
    // Act
    Request request = command.getRequest();
    // Assert
    assertThat(request.getComponentId()).isEqualTo(component.componentId());
    assertThat(request.getId()).isEqualTo(/*expected =*/ 0);
    assertThat(request.getOpcode()).isEqualTo(Opcode.GEAR_DATA);
    assertThat(request.getDomain()).isEqualTo(Domain.GEAR);
    assertThat(request.getExtension(DataChannelRequest.data).getTouch())
        .isEqualTo(DataStreamState.DATA_STREAM_ENABLE);
    assertThat(request.getExtension(DataChannelRequest.data).getInference())
        .isEqualTo(DataStreamState.DATA_STREAM_DISABLE);
    assertThat(request.getExtension(DataChannelRequest.data).getTouch().getNumber()).isEqualTo(1);
  }

  @Test
  public void parseResponse_failureResponse() {
    // Arrange
    Response response = Response.newBuilder()
        .setId(1)
        .setComponentId(1)
        .setStatus(Status.ERROR_APP_UNKNOWN)
        .build();
    // Act
    Result<Response> result = command.parseResponse(response);
    // Assert
    assertThat(result.failure()).isInstanceOf(Throwable.class);
  }

  @Test
  public void parseResponse_successResponse() {
    // Arrange
    Response response = Response.newBuilder()
        .setId(1)
        .setComponentId(component.componentId())
        .setStatus(Status.STATUS_OK)
        .build();
    // Act
    Result<Response> result = command.parseResponse(response);
    // Assert
    assertThat(result.success().getComponentId()).isEqualTo(component.componentId());
    assertThat(result.success().getId()).isEqualTo(1);
    assertThat(result.success().getStatus().getNumber()).isEqualTo(0);
  }

  private static Map<String, Vendor> getVendors() {
    List<Capability> capabilities = new ArrayList<>();
    capabilities.add(Product.Capability.GESTURE);
    capabilities.add(Product.Capability.LED);
    List<Product> products = new ArrayList<>();
    Product product = Product.of("2", "Product 2", "jq_image", capabilities);
    products.add(product);
    Map<String, Vendor> vendors = new HashMap<>();
    Vendor vendor = Vendor.of("1", "Vendor 1", products);
    vendors.put("1", vendor);
    component = Component
        .of(/* componentId= */ 0, vendor, product, capabilities, /* revision= */
            null, /* serialNumber= */ null);
    return vendors;
  }
}
