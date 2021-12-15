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
import com.google.android.jacquard.sdk.command.PlayLedPatternCommand.Color;
import com.google.android.jacquard.sdk.command.PlayLedPatternCommand.Frame;
import com.google.android.jacquard.sdk.command.PlayLedPatternCommand.LedPatternType;
import com.google.android.jacquard.sdk.command.PlayLedPatternCommand.PlayLedPatternCommandBuilder;
import com.google.android.jacquard.sdk.command.PlayLedPatternCommand.PlayType;
import com.google.android.jacquard.sdk.connection.Result;
import com.google.android.jacquard.sdk.datastore.DataProvider;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.Product;
import com.google.android.jacquard.sdk.model.Product.Capability;
import com.google.android.jacquard.sdk.model.Vendor;
import com.google.android.jacquard.sdk.util.StringUtils;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.LedPatternRequest;
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
 * Unit tests for {@link PlayLedPatternCommand} class.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public class PlayLedPatternCommandTest {

  private PlayLedPatternCommand command;
  private PlayLedPatternCommandBuilder commandBuilder;
  private static Component component;

  @Before
  public void setup() {
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    DataProvider.create(getVendors());
    List<Frame> frames = new ArrayList<>();
    frames.add(Frame.of(Color.of(100, 200, 300), 1000));
    frames.add(Frame.of(Color.of(200, 0, 0), 1000));
    commandBuilder = PlayLedPatternCommand.newBuilder()
        .setComponent(component)
        .setDurationInMs(5000)
        .setHaltAll(false)
        .setIntensityLevel(10)
        .setResumable(true)
        .setPlayType(PlayType.PLAY)
        .setLedPatternType(LedPatternType.PATTERN_TYPE_BREATHING)
        .setFrames(frames)
        .setStringUtils(StringUtils.getInstance());
  }

  @Test
  public void getRequestForGear() {
    // Arrange
    component = FakeComponent.getGearComponent(/* componentId= */10);
    command = commandBuilder.setComponent(component).build();
    // Act
    Request request = command.getRequest();
    // Assert
    assertThat(request.getComponentId()).isEqualTo(component.componentId());
    assertThat(request.getId()).isEqualTo(0);
    assertThat(request.getOpcode()).isEqualTo(Opcode.GEAR_LED);
    assertThat(request.getDomain()).isEqualTo(Domain.GEAR);
    assertThat(request.getExtension(LedPatternRequest.ledPatternRequest).getFramesCount())
        .isEqualTo(2);
    assertThat(request.getExtension(LedPatternRequest.ledPatternRequest).getDurationMs())
        .isEqualTo(5000);
    assertThat(request.getExtension(LedPatternRequest.ledPatternRequest).getHaltAll()).isFalse();
    assertThat(request.getExtension(LedPatternRequest.ledPatternRequest).getResumable()).isTrue();
    assertThat(request.getExtension(LedPatternRequest.ledPatternRequest).getIntensityLevel())
        .isEqualTo(10);
    assertThat(
        request.getExtension(LedPatternRequest.ledPatternRequest).getPatternType().getNumber())
        .isEqualTo(LedPatternType.PATTERN_TYPE_BREATHING.ordinal());
    assertThat(
        request.getExtension(LedPatternRequest.ledPatternRequest).getPlayPauseToggle().getNumber())
        .isEqualTo(PlayType.PLAY.ordinal());
  }

  @Test
  public void getRequestForTag() {
    // Arrange
    component = FakeComponent.getTagComponent();
    command = commandBuilder.setComponent(component).build();
    // Act
    Request request = command.getRequest();
    // Assert
    assertThat(request.getComponentId()).isEqualTo(component.componentId());
    assertThat(request.getId()).isEqualTo(0);
    assertThat(request.getOpcode()).isEqualTo(Opcode.LED_PATTERN);
    assertThat(request.getDomain()).isEqualTo(Domain.BASE);
    assertThat(request.getExtension(LedPatternRequest.ledPatternRequest).getFramesCount())
        .isEqualTo(2);
    assertThat(request.getExtension(LedPatternRequest.ledPatternRequest).getDurationMs())
        .isEqualTo(5000);
    assertThat(request.getExtension(LedPatternRequest.ledPatternRequest).getHaltAll()).isFalse();
    assertThat(request.getExtension(LedPatternRequest.ledPatternRequest).getResumable()).isTrue();
    assertThat(request.getExtension(LedPatternRequest.ledPatternRequest).getIntensityLevel())
        .isEqualTo(10);
    assertThat(
        request.getExtension(LedPatternRequest.ledPatternRequest).getPatternType().getNumber())
        .isEqualTo(LedPatternType.PATTERN_TYPE_BREATHING.ordinal());
    assertThat(
        request.getExtension(LedPatternRequest.ledPatternRequest).getPlayPauseToggle().getNumber())
        .isEqualTo(PlayType.PLAY.ordinal());
  }

  @Test
  public void parseResponse_failureResponse() {
    // Arrange
    command = commandBuilder.build();
    Response response = Response.newBuilder()
        .setId(1)
        .setComponentId(1)
        .setStatus(Status.ERROR_APP_UNKNOWN)
        .build();
    command = commandBuilder.build();
    // Act
    Result<Boolean> result = command.parseResponse(response);
    // Assert
    assertThat(result.failure()).isInstanceOf(Throwable.class);
  }

  @Test
  public void parseResponse_successResponse() {
    // Arrange
    command = commandBuilder.build();
    Response response = Response.newBuilder()
        .setId(1)
        .setComponentId(1)
        .setStatus(Status.STATUS_OK)
        .build();
    // Act
    Result<Boolean> result = command.parseResponse(response);
    // Assert
    assertThat(result.success()).isEqualTo(true);
  }

  private static Map<String, Vendor> getVendors() {
    List<Capability> capabilities = new ArrayList<>();
    capabilities.add(Capability.GESTURE);
    capabilities.add(Capability.LED);
    List<Product> products = new ArrayList<>();
    Product product = Product.of("00-00-00-02", "Product 1", "jq_image", capabilities);
    products.add(product);
    Map<String, Vendor> vendors = new HashMap<>();
    Vendor vendor = Vendor.of("00-00-00-01", "Vendor 1", products);
    vendors.put("00-00-00-01", vendor);
    component = Component
        .of(/* componentId= */ 0, vendor, product, capabilities, /* revision= */
            null, /* serialNumber= */ null);
    return vendors;
  }
}
