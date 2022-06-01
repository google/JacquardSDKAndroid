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
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.datastore.DataProvider;
import com.google.android.jacquard.sdk.model.Gesture;
import com.google.atap.jacquard.protocol.JacquardProtocol;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * Unit test for {@link InnerGestureNotificationSubscription} class.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public final class InnerGestureNotificationSubscriptionTest {
    private InnerGestureNotificationSubscription notificationSubscription;

    @Before
    public void setUp() {
        DataProvider.create(new HashMap<>());
        notificationSubscription = new InnerGestureNotificationSubscription();
    }

    @Test
    public void extract_returnsInvalidNotification() {
        // Act
        Gesture value = notificationSubscription.extract(JacquardProtocol.Notification.newBuilder().setDomain(JacquardProtocol.Domain.GEAR).buildPartial());
        // Assert
        assertNull(value);
    }

    @Test
    public void extract_brushOut_returnsValidNotification() {
        // Arrange
        JacquardProtocol.InferenceData touchData = JacquardProtocol.InferenceData.newBuilder()
                .setEvent(3)
                .build();
        JacquardProtocol.DataChannelNotification channelNotification
                = JacquardProtocol.DataChannelNotification.newBuilder().setInferenceData(touchData).build();
        // Act
        Gesture value = notificationSubscription.extract(JacquardProtocol.Notification.newBuilder()
                .setDomain(JacquardProtocol.Domain.GEAR)
                .setOpcode(JacquardProtocol.Opcode.GESTURE)
                .setExtension(JacquardProtocol.DataChannelNotification.data, channelNotification)
                .build());
        // Assert
        assertThat(value.gestureType()).isEqualTo(Gesture.GestureType.BRUSH_OUT);
    }
}
