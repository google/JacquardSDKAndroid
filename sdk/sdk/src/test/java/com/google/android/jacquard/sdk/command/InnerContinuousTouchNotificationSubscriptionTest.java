/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import com.google.android.jacquard.sdk.model.TouchData;
import com.google.android.jacquard.sdk.util.StringUtils;
import com.google.atap.jacquard.protocol.JacquardProtocol;
import com.google.protobuf.ByteString;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNull;

/**
 * Unit test for {@link InnerContinuousTouchNotificationSubscription} class.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public final class InnerContinuousTouchNotificationSubscriptionTest {
    private InnerContinuousTouchNotificationSubscription notificationSubscription;

    @Before
    public void setUp() {
        DataProvider.create(new HashMap<>(), StringUtils.getInstance());
        notificationSubscription = new InnerContinuousTouchNotificationSubscription();
    }

    @Test
    public void extract_returnInvalidNotification() {
        // Act
        TouchData value = notificationSubscription.extract(JacquardProtocol.Notification.newBuilder().setDomain(JacquardProtocol.Domain.GEAR).buildPartial());
        // Assert
        assertNull(value);
    }

    @Test
    public void extract_returnsValidNotification(){
        // Arrange
        byte[] message = new byte[]{8, 7, 16, 0, 24, 0, -22, 68, 53, 18, 51, 10, 10};
        JacquardProtocol.TouchData touchData = JacquardProtocol.TouchData.newBuilder()
                .setDiffDataScaled(ByteString.copyFrom(message))
                .setDiffProximity(8)
                .setSequence(2)
                .build();
        JacquardProtocol.DataChannelNotification channelNotification
                = JacquardProtocol.DataChannelNotification.newBuilder().setTouchData(touchData).build();
        // Act
        TouchData value = notificationSubscription.extract(JacquardProtocol.Notification.newBuilder()
                .setDomain(JacquardProtocol.Domain.GEAR)
                .setOpcode(JacquardProtocol.Opcode.GEAR_DATA)
                .setExtension(JacquardProtocol.DataChannelNotification.data, channelNotification)
                .build());
        // Assert
        assertThat(value.lines().size()).isEqualTo(12);
        assertThat(value.proximity()).isEqualTo(8);
        assertThat(value.sequence()).isEqualTo(2);
    }
}
