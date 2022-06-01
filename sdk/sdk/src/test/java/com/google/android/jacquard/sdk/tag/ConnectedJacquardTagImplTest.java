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

package com.google.android.jacquard.sdk.tag;

import static com.google.android.jacquard.sdk.command.FakeComponent.UUID;
import static com.google.common.truth.Truth.assertThat;

import android.os.Build.VERSION_CODES;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.jacquard.sdk.JacquardManager;
import com.google.android.jacquard.sdk.JacquardManagerInitialization;
import com.google.android.jacquard.sdk.command.DeviceInfo;
import com.google.android.jacquard.sdk.command.FakeComponent;
import com.google.android.jacquard.sdk.command.GetConfigCommand;
import com.google.android.jacquard.sdk.command.ListModulesCommand;
import com.google.android.jacquard.sdk.command.LoadModuleCommand;
import com.google.android.jacquard.sdk.command.LoadModuleNotificationSubscription;
import com.google.android.jacquard.sdk.command.RenameTagCommand;
import com.google.android.jacquard.sdk.command.SetConfigCommand;
import com.google.android.jacquard.sdk.command.SetConfigCommand.SettingsType;
import com.google.android.jacquard.sdk.command.UnloadModuleCommand;
import com.google.android.jacquard.sdk.datastore.DataProvider;
import com.google.android.jacquard.sdk.initialization.FakeTransportImpl;
import com.google.android.jacquard.sdk.initialization.FakeTransportState;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.DeviceConfigElement;
import com.google.android.jacquard.sdk.model.FakeImuModule;
import com.google.android.jacquard.sdk.model.FakePeripheral;
import com.google.android.jacquard.sdk.model.GearState.Type;
import com.google.android.jacquard.sdk.model.Product;
import com.google.android.jacquard.sdk.model.Product.Capability;
import com.google.android.jacquard.sdk.model.ProtocolSpec;
import com.google.android.jacquard.sdk.model.SdkConfig;
import com.google.android.jacquard.sdk.model.TouchMode;
import com.google.android.jacquard.sdk.model.Vendor;
import com.google.android.jacquard.sdk.pairing.RequiredCharacteristics;
import com.google.android.jacquard.sdk.remote.FakeLocalRemoteFunction;
import com.google.android.jacquard.sdk.remote.RemoteFunctionInitialization;
import com.google.android.jacquard.sdk.util.FakeFragmenter;
import com.google.atap.jacquard.protocol.JacquardProtocol.AttachedNotification;
import com.google.atap.jacquard.protocol.JacquardProtocol.DeviceInfoResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.Domain;
import com.google.atap.jacquard.protocol.JacquardProtocol.Notification;
import com.google.atap.jacquard.protocol.JacquardProtocol.Opcode;
import com.google.atap.jacquard.protocol.JacquardProtocol.Status;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link ConnectedJacquardTagImpl}.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public class ConnectedJacquardTagImplTest {

  private final static String VENDOR_ID = "74-a8-ce-54";
  private final static String PRODUCT_ID = "8a-66-50-f4";
  private final static int VID = 1957219924;
  private final static int PID = -1973006092;
  private static final String TAG_RENAME = "sample-sdk-ujt";
  private final static String CLIENT_ID = "aa-aa-aa-aa";
  private final static String API_KEY = "api key";
  private final FakeFragmenter commandFragmenter = new FakeFragmenter("commandFragmenter",
      ProtocolSpec.VERSION_2.getMtuSize());
  private final FakeFragmenter notificationFragmenter = new FakeFragmenter("notificationFragmenter",
      ProtocolSpec.VERSION_2.getMtuSize());
  FakeFragmenter dataFragmenter = new FakeFragmenter("dataFragmenter",
      ProtocolSpec.VERSION_2.getMtuSize());
  private final FakeTransportState transportState = new FakeTransportState(commandFragmenter,
      notificationFragmenter, dataFragmenter);

  private final FakePeripheral peripheral = new FakePeripheral(null);
  private final RequiredCharacteristics requiredCharacteristics = new RequiredCharacteristics();
  private FakeTransportImpl transport = null;
  private ConnectedJacquardTagImpl connectedJacquardTag;

  @Before
  public void setUp() {
    PrintLogger.initialize(ApplicationProvider.getApplicationContext());
    DataProvider.create(getVendors());
    RemoteFunctionInitialization.initRemoteFunction(
        new FakeLocalRemoteFunction(ApplicationProvider.getApplicationContext().getResources()));
    JacquardManagerInitialization.initJacquardManager();
    JacquardManager.getInstance()
        .init(SdkConfig.of(CLIENT_ID, API_KEY, /* cloudEndpointUrl= */ null));
    transport = new FakeTransportImpl(peripheral, requiredCharacteristics, transportState);
    transport.setModulePresent(true);
    transport.assertCommandFailure(false);
    connectedJacquardTag = new ConnectedJacquardTagImpl(transport, getDeviceInfo());
  }

  @Test
  public void rssiSignal() {
    // Assign
    List<Integer> rssiValueList = new ArrayList<>();
    // Act
    connectedJacquardTag.rssiSignal().onNext(rssiValueList::add);
    transport.onRSSIValueUpdated(-50);
    transport.onRSSIValueUpdated(-60);
    // Assert
    assertThat(rssiValueList.size()).isEqualTo(2);
  }

  @Test
  public void destroy() {
    // Assign
    List<Integer> rssiValueList = new ArrayList<>();
    connectedJacquardTag.rssiSignal().onNext(rssiValueList::add);
    transport.onRSSIValueUpdated(-50);
    // Act
    connectedJacquardTag.destroy();
    transport.onRSSIValueUpdated(-60);
    // Assert
    assertThat(rssiValueList.size()).isEqualTo(1);
  }

  @Test
  public void serialNumber() {
    // Act
    String serialNumber = connectedJacquardTag.serialNumber();
    // Assert
    assertThat(serialNumber).isEqualTo(FakeComponent.SERIAL_NUMBER);
  }

  @Test
  public void tagRename() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    connectedJacquardTag.enqueue(new RenameTagCommand(TAG_RENAME)).onNext(name -> {
      if (name.equals(TAG_RENAME)) {
        latch.countDown();
      }
    });
    boolean countReached = latch.await(5, TimeUnit.SECONDS);
    assertThat(countReached).isTrue();
  }

  @Test
  public void getTagCustomAdvName() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    connectedJacquardTag.getCustomAdvName().filter(name -> TAG_RENAME.equals(name))
        .onNext(ignore -> latch.countDown()
        );
    boolean countReached = latch.await(5, TimeUnit.SECONDS);
    assertThat(countReached).isTrue();
  }

  @Test
  public void tagRename_failure() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    transport.assertCommandFailure(true);
    connectedJacquardTag.enqueue(new RenameTagCommand(TAG_RENAME)).onError(error -> {
      if (error != null) {
        latch.countDown();
      }
    });
    boolean countReached = latch.await(5, TimeUnit.SECONDS);
    assertThat(countReached).isTrue();
  }

  @Test
  public void setGestureTouchMode() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    connectedJacquardTag.setTouchMode(getGearDeviceInfo(), TouchMode.GESTURE)
        .filter(result -> result).onNext(ignore -> latch.countDown());
    boolean countReached = latch.await(5, TimeUnit.SECONDS);
    assertThat(countReached).isTrue();
  }

  @Test
  public void observeAttachState() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    connectedJacquardTag.getConnectedGearSignal()
        .filter(gearState -> gearState.getType() == Type.ATTACHED).onNext(ignore ->
        latch.countDown());
    sendAttachNotification();
    boolean countReached = latch.await(5, TimeUnit.SECONDS);
    assertThat(countReached).isTrue();
  }

  @Test
  public void setContinuousTouchMode() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    connectedJacquardTag.setTouchMode(getGearDeviceInfo(), TouchMode.CONTINUOUS)
        .filter(result -> result).onNext(ignore -> latch.countDown());
    boolean countReached = latch.await(5, TimeUnit.SECONDS);
    assertThat(countReached).isTrue();
  }

  @Test
  public void getTagDefaultDisplayName() {
    assertThat(connectedJacquardTag.displayName()).isEqualTo("Fake-Jacquard Tag");
  }

  @Test
  public void getTagIdentifier() {
    assertThat(connectedJacquardTag.address()).isEqualTo("C2:04:1C:6F:02:BA");
  }

  @Test
  public void testListModulesCommand() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    connectedJacquardTag.enqueue(new ListModulesCommand())
        .onNext(list -> {
          if (list != null) {
            if (list.get(0).equals(FakeImuModule.getImuModule())) {
              latch.countDown();
            }
          }
        });
    boolean countReached = latch.await(2, TimeUnit.SECONDS);
    assertThat(countReached).isTrue();
  }

  @Test
  public void testLoadModuleCommand() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    connectedJacquardTag.enqueue(new LoadModuleCommand(FakeImuModule.getImuModule()))
        .onNext(response -> connectedJacquardTag.subscribe(new LoadModuleNotificationSubscription())
            .onNext(module -> {
              if (module != null) {
                if (module.equals(FakeImuModule.getImuModule())) {
                  latch.countDown();
                }
              }
            }));
    boolean countReached = latch.await(2, TimeUnit.SECONDS);
    assertThat(countReached).isTrue();
  }

  @Test
  public void testUnloadModuleCommand() throws InterruptedException{
    CountDownLatch latch = new CountDownLatch(1);
    connectedJacquardTag.enqueue(new UnloadModuleCommand(FakeImuModule.getImuModule()))
        .filter(unloaded -> unloaded)
        .onNext(unloaded -> latch.countDown());
    boolean countReached = latch.await(2, TimeUnit.SECONDS);
    assertThat(countReached).isTrue();
  }

  @Test
  public void testSetConfig() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    connectedJacquardTag.enqueue(
        new SetConfigCommand(DeviceConfigElement.create(1, 1, "key", SettingsType.STRING, "12345")))
        .filter(set -> set)
        .onNext(set -> latch.countDown());
    boolean countReached = latch.await(2, TimeUnit.SECONDS);
    assertThat(countReached).isTrue();
  }

  @Test
  public void testGetConfig() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    DeviceConfigElement element = DeviceConfigElement
        .create(123, 456, "key", SettingsType.STRING, "testString");
    connectedJacquardTag.enqueue(new SetConfigCommand(element))
        .flatMap(ignore ->
            connectedJacquardTag.enqueue(
                new GetConfigCommand(123, 456, "key")))
        .filter(value -> value.toString().equals("testString"))
        .onNext(value -> latch.countDown());
    boolean countReached = latch.await(2, TimeUnit.SECONDS);
    assertThat(countReached).isTrue();
  }

  private void sendAttachNotification() {
    AttachedNotification attachedNotification = AttachedNotification.newBuilder()
        .setAttachState(true).setComponentId(1).setVendorId(VID)
        .setProductId(PID)
        .build();

    Notification notification = Notification.newBuilder().setOpcode(Opcode.ATTACHED)
        .setDomain(Domain.GEAR)
        .setExtension(AttachedNotification.attached, attachedNotification).build();

    transport.getNotifySignal().next(notification.toByteArray());
  }

  private Map<String, Vendor> getVendors() {
    List<Capability> capabilities = new ArrayList<>();
    capabilities.add(Product.Capability.GESTURE);
    capabilities.add(Product.Capability.LED);
    List<Product> products = new ArrayList<>();
    products.add(Product.of(PRODUCT_ID, "Product", "jq_image", capabilities));
    Map<String, Vendor> vendors = new HashMap<>();
    vendors.put(VENDOR_ID, Vendor.of(VENDOR_ID, "Vendor", products));
    return vendors;
  }

  private static DeviceInfo getDeviceInfo(){
    return DeviceInfo.ofTag(
        DeviceInfoResponse.newBuilder().setGearId("").setSkuId("").setVendorId(VID)
            .setProductId(PID).setUuid(UUID).setVendor("").setRevision(0).setModel("")
            .setMlVersion("").setBootloaderMajor(0).setBootloaderMinor(0)
            .setFirmwarePoint(0).setFirmwareMinor(0).setFirmwareMajor(0)
            .build());
  }

  private static Component getGearDeviceInfo() {
    List<Capability> capabilities = new ArrayList<>();
    List<Product> products = new ArrayList<>();
    Product product = Product.of(PRODUCT_ID, "Product 1", "jq_image", capabilities);
    products.add(product);
    Vendor vendor = Vendor.of(VENDOR_ID, "Vendor 1", products);
    return Component
        .of(/* componentId= */ 1, vendor, product, capabilities, /* revision= */
            null, /* serialNumber= */ null);
  }
}
