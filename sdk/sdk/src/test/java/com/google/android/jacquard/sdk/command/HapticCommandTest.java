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
package com.google.android.jacquard.sdk.command;

import static com.google.common.truth.Truth.assertThat;

import android.os.Build.VERSION_CODES;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.command.HapticCommand.Frame;
import com.google.android.jacquard.sdk.command.HapticCommand.Pattern;
import com.google.android.jacquard.sdk.connection.Result;
import com.google.android.jacquard.sdk.datastore.DataProvider;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.Product;
import com.google.android.jacquard.sdk.model.Product.Capability;
import com.google.android.jacquard.sdk.model.Vendor;
import com.google.android.jacquard.sdk.util.StringUtils;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.HapticRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.HapticResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.HapticSymbolType;
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
 * Unit tests for {@link HapticCommand} class.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public class HapticCommandTest {

  private HapticCommand command;
  private static Component component;

  @Before
  public void setup() {
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    DataProvider.create(getVendors());
    Frame frame = Frame.builder()
        .setOnMs(10)
        .setOffMs(5)
        .setMaxAmplitudePercent(20)
        .setRepeatNminusOne(2)
        .setPattern(Pattern.HAPTIC_SYMBOL_LINEAR_INCREASE).build();
    command = new HapticCommand(frame, component);
  }

  @Test
  public void getRequest() {
    // Act
    Request request = command.getRequest();
    // Assert
    assertThat(request.getComponentId()).isEqualTo(component.componentId());
    assertThat(request.getOpcode()).isEqualTo(Opcode.GEAR_HAPTIC);
    assertThat(request.getDomain()).isEqualTo(Domain.GEAR);
    assertThat(request.getExtension(HapticRequest.haptic).getFrames().getOnMs()).isEqualTo(10);
    assertThat(request.getExtension(HapticRequest.haptic).getFrames().getOffMs()).isEqualTo(5);
    assertThat(request.getExtension(HapticRequest.haptic).getFrames().getMaxAmplitudePercent())
        .isEqualTo(20);
    assertThat(request.getExtension(HapticRequest.haptic).getFrames().getRepeatNMinusOne())
        .isEqualTo(2);
    assertThat(request.getExtension(HapticRequest.haptic).getFrames().getPattern())
        .isEqualTo(HapticSymbolType.HAPTIC_SYMBOL_LINEAR_INCREASE);
  }

  @Test
  public void parseResponse_failureResponse() {
    // Arrange
    HapticResponse hapticResponse = HapticResponse.newBuilder().build();
    Response response = Response.newBuilder()
        .setExtension(HapticResponse.haptic, hapticResponse)
        .setId(1)
        .setComponentId(1)
        .setStatus(Status.ERROR_APP_UNKNOWN)
        .build();
    // Act
    Result<Boolean> result = command.responseErrorCheck(response.toByteArray());
    // Assert
    assertThat(result.failure()).isInstanceOf(Throwable.class);
  }

  @Test
  public void parseResponse_successResponse() {
    // Arrange
    HapticResponse hapticResponse = HapticResponse.newBuilder().build();
    Response response = Response.newBuilder()
        .setExtension(HapticResponse.haptic, hapticResponse)
        .setId(1)
        .setComponentId(1)
        .setStatus(Status.STATUS_OK)
        .build();
    // Act
    Result<Boolean> result = command.responseErrorCheck(response.toByteArray());
    // Assert
    assertThat(result.success()).isEqualTo(true);
  }

  private static Map<String, Vendor> getVendors() {
    List<Capability> capabilities = new ArrayList<>();
    capabilities.add(Capability.GESTURE);
    capabilities.add(Capability.LED);
    List<Product> products1 = new ArrayList<>();
    Product product = Product.of("00-00-00-02", "Product 1", "jq_image", capabilities);
    products1.add(product);
    Map<String, Vendor> vendors = new HashMap<>();
    Vendor vendor = Vendor.of("00-00-00-01", "Vendor 1", products1);
    vendors.put("00-00-00-01", vendor);
    component = Component
        .of(/* componentId= */ 123, vendor, product, capabilities, /* revision= */
            null, /* serialNumber= */ null);
    return vendors;
  }
}
