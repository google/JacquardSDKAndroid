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
import com.google.android.jacquard.sdk.connection.Result;
import com.google.android.jacquard.sdk.datastore.DataProvider;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.Product;
import com.google.android.jacquard.sdk.model.Product.Capability;
import com.google.android.jacquard.sdk.model.Vendor;
import com.google.android.jacquard.sdk.util.StringUtils;
import com.google.atap.jacquard.protocol.JacquardProtocol.DeviceInfoRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.DeviceInfoResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.atap.jacquard.protocol.JacquardProtocol.Status;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link DeviceInfoCommand} class.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public class DeviceInfoCommandTest {

  private DeviceInfoCommand command;
  private static Component component;

  @Before
  public void setup() {
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    DataProvider.create(getVendors());
    command = new DeviceInfoCommand(component.componentId());
  }

  @Test
  public void getRequestForGear() {
    // Act
    Request request = command.getRequest();
    // Assert
    assertThat(request.getComponentId()).isEqualTo(component.componentId());
    assertThat(request.getOpcode()).isEqualTo(Opcode.DEVICEINFO);
    assertThat(request.getDomain()).isEqualTo(Domain.GEAR);
    assertThat(request.getExtension(DeviceInfoRequest.deviceInfo).getComponent()).isNotNull();
  }

  @Test
  public void getRequestForTag() {
    // Arrange
    component = FakeComponent.getTagComponent();
    command = new DeviceInfoCommand(component.componentId());
    // Act
    Request request = command.getRequest();
    // Assert
    assertThat(request.getComponentId()).isEqualTo(component.componentId());
    assertThat(request.getOpcode()).isEqualTo(Opcode.DEVICEINFO);
    assertThat(request.getDomain()).isEqualTo(Domain.BASE);
  }

  @Test
  public void parseResponse_successResponse() {
    // Arrange
    DeviceInfoResponse deviceInfoResponse = DeviceInfoResponse.newBuilder()
        .setProductId(123)
        .setBootloaderMajor(1)
        .setBootloaderMinor(2)
        .setFirmwareMajor(1)
        .setFirmwareMinor(2)
        .setGearId("12345")
        .setModel("test")
        .setVendor("testVendor")
        .setUuid("12345")
        .setRevision(10)
        .setModelBytes(ByteString.copyFromUtf8("test"))
        .build();
    Response response = Response.newBuilder()
        .setExtension(DeviceInfoResponse.deviceInfo, deviceInfoResponse)
        .setId(1)
        .setComponentId(1)
        .setStatus(Status.STATUS_OK)
        .build();
    // Act
    Result<DeviceInfo> responseResult = command.responseErrorCheck(response.toByteArray());
    DeviceInfo result = responseResult.success();
    // Assert
    assertThat(result.bootloaderMajor()).isEqualTo(1);
    assertThat(result.bootloaderMinor()).isEqualTo(2);
    assertThat(result.version().major()).isEqualTo(1);
    assertThat(result.version().minor()).isEqualTo(2);
    assertThat(result.gearId()).isEqualTo("12345");
    assertThat(result.model()).isEqualTo("test");
    assertThat(result.vendor()).isEqualTo("testVendor");
    assertThat(result.productId()).isEqualTo(123);
  }

  private static Map<String, Vendor> getVendors() {
    List<Capability> capabilities = new ArrayList<>();
    capabilities.add(Product.Capability.GESTURE);
    capabilities.add(Product.Capability.LED);
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
