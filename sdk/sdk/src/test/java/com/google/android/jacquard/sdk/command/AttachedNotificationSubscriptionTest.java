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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNull;

import android.os.Build.VERSION_CODES;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.datastore.DataProvider;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.GearState;
import com.google.android.jacquard.sdk.model.Product;
import com.google.android.jacquard.sdk.model.Vendor;
import com.google.atap.jacquard.protocol.JacquardProtocol;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * Unit test for {@link AttachedNotificationSubscription} class.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public final class AttachedNotificationSubscriptionTest {
    private AttachedNotificationSubscription notificationSubscription;

    @Before
    public void setUp() {
        PrintLogger.initialize(ApplicationProvider.getApplicationContext());
        DataProvider.create(getVendors());
        notificationSubscription = new AttachedNotificationSubscription();
    }

    @Test
    public void extract_returnsInvalidNotification() {
    // Act
    GearState value =
        notificationSubscription.extract(
            JacquardProtocol.Notification.newBuilder()
                .setDomain(JacquardProtocol.Domain.GEAR)
                .buildPartial()
                .toByteArray());
        // Assert
        assertNull(value);
    }

    @Test
    public void extract_detached_returnsValidNotification() {
    // Act
    GearState value =
        notificationSubscription.extract(
            JacquardProtocol.Notification.newBuilder()
                .setDomain(JacquardProtocol.Domain.GEAR)
                .setOpcode(JacquardProtocol.Opcode.ATTACHED)
                .setComponentId(1)
                .setExtension(
                    JacquardProtocol.AttachedNotification.attached,
                    JacquardProtocol.AttachedNotification.getDefaultInstance())
                .build()
                .toByteArray());
        // Assert
        assertThat(value.getType()).isEqualTo(GearState.Type.DETACHED);
    }

    @Test
    public void extract_attached_returnsValidNotification() {
        // Arrange
        JacquardProtocol.AttachedNotification attachedNotification
                = JacquardProtocol.AttachedNotification.newBuilder()
                .setProductId(2)
                .setVendorId(1)
                .setAttachState(true)
                .build();
    // Act
    GearState value =
        notificationSubscription.extract(
            JacquardProtocol.Notification.newBuilder()
                .setDomain(JacquardProtocol.Domain.GEAR)
                .setOpcode(JacquardProtocol.Opcode.ATTACHED)
                .setExtension(JacquardProtocol.AttachedNotification.attached, attachedNotification)
                .build()
                .toByteArray());
        // Assert
        assertThat(value.getType()).isEqualTo(GearState.Type.ATTACHED);
    }

    private static Map<String, Vendor> getVendors() {
        List<Product.Capability> capabilities = new ArrayList<>();
        capabilities.add(Product.Capability.GESTURE);
        capabilities.add(Product.Capability.LED);
        List<Product> products = new ArrayList<>();
        products.add(Product.of("00-00-00-02", "Product 2", "jq_image", capabilities));
        Map<String, Vendor> vendors = new HashMap<>();
        vendors.put("00-00-00-01", Vendor.of("00-00-00-01", "Vendor 1", products));
        return vendors;
    }
}
