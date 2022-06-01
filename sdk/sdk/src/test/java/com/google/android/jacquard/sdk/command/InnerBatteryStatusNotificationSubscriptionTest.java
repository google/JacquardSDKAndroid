/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.jacquard.sdk.command;

import android.os.Build.VERSION_CODES;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.datastore.DataProvider;
import com.google.android.jacquard.sdk.util.StringUtils;
import com.google.atap.jacquard.protocol.JacquardProtocol;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNull;

/**
 * Unit test for {@link InnerBatteryStatusNotificationSubscription} class.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public final class InnerBatteryStatusNotificationSubscriptionTest {
    private InnerBatteryStatusNotificationSubscription notificationSubscription;

    @Before
    public void setUp() {
        DataProvider.create(new HashMap<>());
        notificationSubscription = new InnerBatteryStatusNotificationSubscription();
    }

    @Test
    public void extract_returnsInvalidNotification() {
        // Act
        BatteryStatus value = notificationSubscription.extract(JacquardProtocol.Notification.newBuilder().setDomain(JacquardProtocol.Domain.BASE).buildPartial());
        // Assert
        assertNull(value);
    }

    @Test
    public void extract_charging_returnsValidNotification() {
        // Act
        BatteryStatus value = notificationSubscription.extract(JacquardProtocol.Notification.newBuilder()
                .setDomain(JacquardProtocol.Domain.BASE)
                .setOpcode(JacquardProtocol.Opcode.BATTERY_STATUS)
                .setExtension(JacquardProtocol.BatteryStatusNotification.batteryStatusNotification, JacquardProtocol.BatteryStatusNotification.getDefaultInstance())
                .build());
        // Assert
        assertThat(value.chargingState()).isEqualTo(BatteryStatus.ChargingState.CHARGING);
    }

    @Test
    public void extract_notCharging_returnsValidNotification() {
        // Arrange
        JacquardProtocol.BatteryStatusNotification batteryStatusNotification =
                JacquardProtocol.BatteryStatusNotification.newBuilder()
                        .setBatteryLevel(80)
                        .setChargingStatus(JacquardProtocol.ChargingStatus.NOT_CHARGING)
                        .build();
        // Act
        BatteryStatus value = notificationSubscription.extract(JacquardProtocol.Notification.newBuilder()
                .setDomain(JacquardProtocol.Domain.BASE)
                .setOpcode(JacquardProtocol.Opcode.BATTERY_STATUS)
                .setExtension(JacquardProtocol.BatteryStatusNotification.batteryStatusNotification, batteryStatusNotification)
                .build());
        // Assert
        assertThat(value.chargingState()).isEqualTo(BatteryStatus.ChargingState.NOT_CHARGING);
        assertThat(value.batteryLevel()).isEqualTo(80);
    }
}
