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
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.command.BatteryStatus.ChargingState;
import com.google.android.jacquard.sdk.connection.Result;
import com.google.android.jacquard.sdk.model.Component;
import com.google.atap.jacquard.protocol.JacquardProtocol;
import com.google.atap.jacquard.protocol.JacquardProtocol.BatteryStatusRequest;
import com.google.atap.jacquard.protocol.JacquardProtocol.BatteryStatusResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.ChargingStatus;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Request;
import com.google.atap.jacquard.protocol.JacquardProtocol.Response;
import com.google.atap.jacquard.protocol.JacquardProtocol.Status;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link BatteryStatusCommand} class.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public class BatteryStatusCommandTest {

  private BatteryStatusCommand command;

  @Before
  public void setup() {
    command = new BatteryStatusCommand();
  }

  @Test
  public void getRequest() {
    // Act
    Request request = command.getRequest();
    // Assert
    assertThat(request.getComponentId()).isEqualTo(0);
    assertThat(request.getId()).isEqualTo(0);
    assertThat(request.getOpcode()).isEqualTo(Opcode.BATTERY_STATUS);
    assertThat(request.getDomain()).isEqualTo(Domain.BASE);
    assertThat(
        request.getExtension(BatteryStatusRequest.batteryStatusRequest).getReadBatteryLevel())
        .isTrue();
    assertThat(
        request.getExtension(BatteryStatusRequest.batteryStatusRequest).getReadChargingStatus())
        .isTrue();
  }

  @Test
  public void parseResponse_throwsIllegalStateException() {
    // Arrange
    Response response = Response.newBuilder().setComponentId(Component.TAG_ID).setId(0)
        .setStatus(Status.STATUS_OK).build();
    // Act
    Result<BatteryStatus> batteryStatusResponse = command.parseResponse(response);
    // Assert
    assertThat(batteryStatusResponse.failure()).isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void parseResponse_charging_successResponse() {
    // Arrange
    BatteryStatusResponse batteryStatusResponse = BatteryStatusResponse.newBuilder()
        .setBatteryLevel(80)
        .setChargingStatus(ChargingStatus.CHARGING)
        .build();
    Response response = JacquardProtocol.Response.newBuilder()
        .setExtension(BatteryStatusResponse.batteryStatusResponse, batteryStatusResponse).setId(1)
        .setComponentId(1).setStatus(Status.STATUS_OK).build();
    // Act
    Result<BatteryStatus> result = command.parseResponse(response);
    // Assert
    assertThat(result.success().batteryLevel()).isEqualTo(80);
    assertThat(result.success().chargingState()).isEqualTo(ChargingState.CHARGING);
  }

  @Test
  public void parseResponse_notCharging_successResponse() {
    // Arrange
    BatteryStatusResponse batteryStatusResponse = BatteryStatusResponse.newBuilder()
        .setBatteryLevel(60)
        .setChargingStatus(ChargingStatus.NOT_CHARGING)
        .build();
    Response response = JacquardProtocol.Response.newBuilder()
        .setExtension(BatteryStatusResponse.batteryStatusResponse, batteryStatusResponse).setId(1)
        .setComponentId(1).setStatus(Status.STATUS_OK).build();
    // Act
    Result<BatteryStatus> result = command.parseResponse(response);
    // Assert
    assertThat(result.success().batteryLevel()).isEqualTo(60);
    assertThat(result.success().chargingState()).isEqualTo(ChargingState.NOT_CHARGING);
  }
}
