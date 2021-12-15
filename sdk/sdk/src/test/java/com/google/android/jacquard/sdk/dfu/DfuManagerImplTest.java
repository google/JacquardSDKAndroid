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
package com.google.android.jacquard.sdk.dfu;

import static android.os.Looper.getMainLooper;
import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Build.VERSION_CODES;
import androidx.test.core.app.ApplicationProvider;
import com.google.android.jacquard.sdk.CommonContextStub;
import com.google.android.jacquard.sdk.FakeJacquardManagerImpl;
import com.google.android.jacquard.sdk.JacquardManagerInitialization;
import com.google.android.jacquard.sdk.command.DeviceInfo;
import com.google.android.jacquard.sdk.command.FakeComponent;
import com.google.android.jacquard.sdk.connection.ConnectionState;
import com.google.android.jacquard.sdk.datastore.DataProvider;
import com.google.android.jacquard.sdk.dfu.DFUInfo.UpgradeStatus;
import com.google.android.jacquard.sdk.dfu.execption.InsufficientBatteryException;
import com.google.android.jacquard.sdk.dfu.execption.UpdatedFirmwareNotFoundException;
import com.google.android.jacquard.sdk.dfu.model.FileDescriptor;
import com.google.android.jacquard.sdk.dfu.model.PlatformSettings;
import com.google.android.jacquard.sdk.initialization.FakeTransportImpl;
import com.google.android.jacquard.sdk.initialization.FakeTransportState;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.Component;
import com.google.android.jacquard.sdk.model.FakeImuModule;
import com.google.android.jacquard.sdk.model.FakePeripheral;
import com.google.android.jacquard.sdk.model.JacquardError;
import com.google.android.jacquard.sdk.model.Module;
import com.google.android.jacquard.sdk.model.Product;
import com.google.android.jacquard.sdk.model.Product.Capability;
import com.google.android.jacquard.sdk.model.ProtocolSpec;
import com.google.android.jacquard.sdk.model.Vendor;
import com.google.android.jacquard.sdk.pairing.RequiredCharacteristics;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.tag.ConnectedJacquardTagInitialization;
import com.google.android.jacquard.sdk.util.FakeFragmenter;
import com.google.atap.jacquard.protocol.JacquardProtocol.DFUWriteResponse;
import com.google.atap.jacquard.protocol.JacquardProtocol.DeviceInfoResponse;
import com.google.common.base.Optional;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {VERSION_CODES.P}, manifest = Config.NONE)
public class DfuManagerImplTest {

  private static final String PERIPHERAL_IDENTIFIER = "identifier";

  private final String version = "001053000";
  private final String dfuStatus = "optional";
  private final URI downloadUrl = new URI("https://fake.url/");
  private final String vendorId = "42-f9-a1-e3";
  private final String productId = "73-d0-58-c3";
  private final String moduleId = "a9-46-5b-d3";
  private final String tagSerialNumber = "000006000";
  private Context context;

  public DfuManagerImplTest() throws URISyntaxException {
  }

  @Before
  public void setup() {
    context = Robolectric.setupService(CommonContextStub.class);
    PrintLogger.initialize(context);
    DataProvider.create(getVendors());
  }

  @Test
  public void assignCheckFirmware_actComponentsEmpty_assertError() {

    // Assign
    final String expectedMessage = "Provided component list is empty.";
    final Class expectedExceptionClass = IllegalArgumentException.class;
    JacquardManagerInitialization.initJacquardManager(new FakeJacquardManagerImpl(
        ApplicationProvider.getApplicationContext()));

    AtomicReference<Throwable> atomicError = new AtomicReference<>();
    DfuManagerImpl dfuManager = new DfuManagerImpl(PERIPHERAL_IDENTIFIER);
    Signal<List<DFUInfo>> signal = Signal.create();
    signal.onError(atomicError::set);


    // Act
    dfuManager.checkFirmware(Collections.emptyList(), /* forceUpdate= */false)
        .forward(signal);

    // Assert
    assertThat(atomicError.get()).isInstanceOf(expectedExceptionClass);
    assertThat(atomicError.get().getMessage()).isEqualTo(expectedMessage);
  }

  @Test
  public void assignCheckFirmware_actDisconnectedTag_assertError() {

    // Assign
    final String expectedMessage = "Device is not connected, address: " + PERIPHERAL_IDENTIFIER;
    final Class expectedExceptionClass = IllegalStateException.class;
    FakeJacquardManagerImpl jacquardManager = new FakeJacquardManagerImpl(
        ApplicationProvider.getApplicationContext());
    jacquardManager.setState(ConnectionState
        .ofDisconnected(JacquardError.ofJacquardInitializationError(new Exception(""))));
    JacquardManagerInitialization.initJacquardManager(jacquardManager);

    AtomicReference<Throwable> atomicError = new AtomicReference<>();
    DfuManagerImpl dfuManager = new DfuManagerImpl(PERIPHERAL_IDENTIFIER);
    Signal<List<DFUInfo>> signal = Signal.create();
    signal.onError(atomicError::set);

    // Act
    dfuManager
        .checkFirmware(
            Collections.singletonList(FakeComponent.getTagComponent()), /* forceUpdate= */false)
        .forward(signal);

    // Assert
    assertThat(atomicError.get()).isInstanceOf(expectedExceptionClass);
    assertThat(atomicError.get().getMessage()).isEqualTo(expectedMessage);
  }

  @Test
  public void assignCheckFirmware_actCheckUpdate_assertDataCorrect()
      throws IOException {

    // Assign
    // Create file.
    byte[] outBytes = "abcdefghijklmnopqrstuvwxyz".getBytes(StandardCharsets.UTF_8);
    InputStream in = new ByteArrayInputStream(outBytes);
    File file = new File(/* pathname= */context.getFilesDir() + "/fakeFile");
        DfuUtil.inputStreamToFile(in, file, in.available()).consume();

    Component component = FakeComponent.getTagComponent();
    RemoteDfuInfo remoteDfuInfo = RemoteDfuInfo.create(version,
        dfuStatus, downloadUrl.toString(), component.vendor().id(), component.product().id(), moduleId);
    JacquardManagerInitialization.initJacquardManager(new FakeJacquardManagerImpl(
        ApplicationProvider.getApplicationContext()));

    DFUChecker dfuChecker = new DummyDFUChecker<>(remoteDfuInfo, /* success= */true)
        .createDownloadManager(file);
    DfuManagerImpl dfuManager = new DfuManagerImpl(PERIPHERAL_IDENTIFIER, dfuChecker,
        new FirmwareUpdateStateMachine(), Signal.empty());
    CacheRepository cacheRepository = new CacheRepositoryImpl(context);
    AtomicReference<List<DFUInfo>> atomicReference = new AtomicReference<>();

    Signal<List<DFUInfo>> signal = Signal.create();
    signal.observe(atomicReference::set, error -> {
      if (error == null) {
        return;
      }
      assertThat(error).isNull();
    });

    // Act
    dfuManager
        .checkFirmware(Arrays.asList(component), /* forceUpdate= */false)
        .forward(signal);
    shadowOf(getMainLooper()).idleFor(Duration.ofSeconds(5));

    // Assert
    assertThat(atomicReference.get().size()).isEqualTo(1);
    for (DFUInfo dfu : atomicReference.get()) {
      Optional<DFUInfo> optional = cacheRepository.getUpdateInformation(component.vendor().id(),
          component.product().id(), /* mid= */"", component.serialNumber());
          Optional<FileDescriptor> fileDescriptorOptional = cacheRepository.getDescriptor(dfu);

      assertThat(optional.isPresent()).isTrue();
      assertThat(fileDescriptorOptional.isPresent()).isTrue();
    }
  }

  @Test
  public void assignCheckModuleUpdate_actEmptyModule_assertError() {

    // Assign
    final String expectedMessage = "Provided Module is empty.";
    final Class expectedExceptionClass = IllegalArgumentException.class;
    JacquardManagerInitialization.initJacquardManager(new FakeJacquardManagerImpl(
        ApplicationProvider.getApplicationContext()));
    AtomicReference<Throwable> atomicError = new AtomicReference<>();
    DfuManagerImpl dfuManager = new DfuManagerImpl(PERIPHERAL_IDENTIFIER);
    Signal<DFUInfo> signal = Signal.create();
    signal.onError(atomicError::set);

    // Act
    dfuManager.checkModuleUpdate(null).forward(signal);

    // Assert
    assertThat(atomicError.get()).isInstanceOf(expectedExceptionClass);
    assertThat(atomicError.get().getMessage()).isEqualTo(expectedMessage);
  }

  @Test
  public void assignCheckModuleUpdate_actDisconnectedTag_assertError() {

    // Assign
    final String expectedMessage = "Device is not connected, address: " + PERIPHERAL_IDENTIFIER;
    final Class expectedExceptionClass = IllegalStateException.class;
    FakeJacquardManagerImpl jacquardManager = new FakeJacquardManagerImpl(
        ApplicationProvider.getApplicationContext());
    jacquardManager.setState(ConnectionState
        .ofDisconnected(JacquardError.ofJacquardInitializationError(new Exception(""))));
    JacquardManagerInitialization.initJacquardManager(jacquardManager);

    AtomicReference<Throwable> atomicError = new AtomicReference<>();
    DfuManagerImpl dfuManager = new DfuManagerImpl(PERIPHERAL_IDENTIFIER);
    Signal<List<DFUInfo>> signal = Signal.create();
    signal.onError(atomicError::set);

    // Act
    dfuManager
        .checkFirmware(
            Collections.singletonList(FakeComponent.getTagComponent()), /* forceUpdate= */ false)
        .forward(signal);

    // Assert
    assertThat(atomicError.get()).isInstanceOf(expectedExceptionClass);
    assertThat(atomicError.get().getMessage()).isEqualTo(expectedMessage);
  }

  @Test
  public void assignCheckModuleUpdate_actCheckUpdate_assertDataCorrect()
      throws IOException {

    // Assign
    // Create file.
    byte[] outBytes = "abcdefghijklmnopqrstuvwxyz".getBytes(StandardCharsets.UTF_8);
    InputStream in = new ByteArrayInputStream(outBytes);
    File file = new File(/* pathname= */ context.getFilesDir() + "/fakeFile");
        DfuUtil.inputStreamToFile(in, file, in.available()).consume();

    Module module = FakeImuModule.getImuModule();
    RemoteDfuInfo remoteDfuInfo = RemoteDfuInfo.create(version,
        dfuStatus, downloadUrl.toString(), module.vendorId(), module.productId(), module.moduleId());
    JacquardManagerInitialization.initJacquardManager(new FakeJacquardManagerImpl(
        ApplicationProvider.getApplicationContext()));

    DFUChecker dfuChecker = new DummyDFUChecker<>(remoteDfuInfo, /* success= */true)
        .createDownloadManager(file);
    DfuManagerImpl dfuManager = new DfuManagerImpl(PERIPHERAL_IDENTIFIER, dfuChecker,
        new FirmwareUpdateStateMachine(), Signal.empty());
    CacheRepository cacheRepository = new CacheRepositoryImpl(context);
    AtomicReference<DFUInfo> atomicReference = new AtomicReference<>();
    Signal<DFUInfo> signal = Signal.create();
    signal.observe(atomicReference::set, error -> {
      if (error == null) {
        return;
      }
      assertThat(error).isNull();
    });

    // Act
    dfuManager.checkModuleUpdate(module).forward(signal);
    shadowOf(getMainLooper()).idleFor(Duration.ofSeconds(5));

    // Assert
    assertThat(atomicReference.get()).isNotNull();
    DFUInfo dfu = atomicReference.get();
    Optional<DFUInfo> optional = cacheRepository.getUpdateInformation(module.vendorId(),
        module.productId(), module.moduleId(), module.moduleId());
    Optional<FileDescriptor> fileDescriptorOptional = cacheRepository.getDescriptor(dfu);

    assertThat(optional.isPresent()).isTrue();
    assertThat(fileDescriptorOptional.isPresent()).isTrue();
  }

  @Test
  public void assignApplyUpdatesAutoExecuteFalse_actIllegalState_assertError()
      throws InterruptedException {

    // Assign
    CountDownLatch countDownLatch = new CountDownLatch(1);
    Component component = FakeComponent.getTagComponent();
    RemoteDfuInfo remoteDfuInfo = RemoteDfuInfo.create(version,
        dfuStatus, downloadUrl.toString(), component.vendor().id(), component.product().id(),
        moduleId);
    JacquardManagerInitialization.initJacquardManager(new FakeJacquardManagerImpl(
        ApplicationProvider.getApplicationContext()));
    DFUChecker dfuChecker = new DummyDFUChecker<>(remoteDfuInfo, /* success= */true);
    FirmwareUpdateStateMachine fwStateMachine = new FirmwareUpdateStateMachine();
    Signal<FirmwareUpdateState> signal = Signal.create();
    DfuManagerImpl dfuManager = new DfuManagerImpl(PERIPHERAL_IDENTIFIER, dfuChecker,
        fwStateMachine, signal);
    AtomicReference<Throwable> atomicError = new AtomicReference<>();
    signal.filter(state -> state.getType().equals(FirmwareUpdateState.Type.ERROR))
        .onNext(state -> {
          atomicError.set(state.error());
          countDownLatch.countDown();
        });

    // Act
    fwStateMachine.getState().next(FirmwareUpdateState.ofPreparingToTransfer());
    dfuManager.applyUpdates(Collections.emptyList(), false);
    countDownLatch.await(5, TimeUnit.SECONDS);

    // Assert
    assertThat(atomicError.get()).isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void assignApplyUpdatesAutoExecuteFalse_actEmptyDfuList_assertError()
      throws InterruptedException {

    // Assign
    CountDownLatch countDownLatch = new CountDownLatch(1);
    Component component = FakeComponent.getTagComponent();
    RemoteDfuInfo remoteDfuInfo = RemoteDfuInfo.create(component.version().toZeroString(),
        dfuStatus, downloadUrl.toString(), component.vendor().id(), component.product().id(),
        moduleId);
    JacquardManagerInitialization.initJacquardManager(new FakeJacquardManagerImpl(
        ApplicationProvider.getApplicationContext()));
    DFUChecker dfuChecker = new DummyDFUChecker<>(remoteDfuInfo, /* success= */true);
    FirmwareUpdateStateMachine fwStateMachine = new FirmwareUpdateStateMachine();
    Signal<FirmwareUpdateState> signal = Signal.create();
    DfuManagerImpl dfuManager = new DfuManagerImpl(PERIPHERAL_IDENTIFIER, dfuChecker,
        fwStateMachine, signal);
    AtomicReference<Throwable> atomicError = new AtomicReference<>();
    signal.filter(state -> state.getType().equals(FirmwareUpdateState.Type.ERROR))
        .onNext(state -> {
          atomicError.set(state.error());
          countDownLatch.countDown();
        });

    // Act
    fwStateMachine.getState().next(FirmwareUpdateState.ofIdle());
    dfuManager.applyUpdates(Collections.emptyList(), false);
    countDownLatch.await(5, TimeUnit.SECONDS);

    // Assert
    assertThat(atomicError.get()).isInstanceOf(IllegalStateException.class);
    assertThat(atomicError.get().getMessage()).isEqualTo("DfuInfo list is empty.");
  }

  @Test
  public void assignApplyUpdatesAutoExecuteFalse_actApplyFirmware_assertStateCorrect()
      throws InterruptedException, IOException {

    // Assign
    Signal<FirmwareUpdateState> signal = Signal.create();
    Component component = FakeComponent.getGearComponent(/* componentId= */1);
    RemoteDfuInfo remoteDfuInfo = RemoteDfuInfo.create(version,
        dfuStatus, downloadUrl.toString(), component.vendor().id(), component.product().id(),
        "");
    DFUInfo dfuInfo = DFUInfo.create(remoteDfuInfo);
    CacheRepository cacheRepository = new CacheRepositoryImpl(context);

    // Create file.
    File file = createFile("abcd", cacheRepository, dfuInfo);

    // ConnectedTag with FakeTransport
    FakeTransportImpl fakeTransport = getTransport(20);
    FakeJacquardManagerImpl fakeJacquardManager = new FakeJacquardManagerImpl(
        ApplicationProvider.getApplicationContext());
    JacquardManagerInitialization.initJacquardManager(fakeJacquardManager);

    // DfuManager
    DFUChecker dfuChecker = new DummyDFUChecker<>(remoteDfuInfo, /* success= */true)
        .createDownloadManager(file);
    FirmwareUpdateStateMachine fwStateMachine = new FirmwareUpdateStateMachine();
    DfuManagerImpl dfuManager = new DfuManagerImpl(PERIPHERAL_IDENTIFIER, dfuChecker,
        fwStateMachine, signal);

    // Fake Connected Tag
    fakeJacquardManager.setState(ConnectionState
        .ofConnected(ConnectedJacquardTagInitialization.createConnectedJacquardTag(fakeTransport,
            getDeviceInfo(), dfuManager)));

    List<FirmwareUpdateState> list = new ArrayList<>();
    CountDownLatch countDownLatch = new CountDownLatch(1);
    fwStateMachine.getState().distinctUntilChanged().onNext(state -> {
      list.add(state);
      if (state.getType().equals(FirmwareUpdateState.Type.TRANSFER_PROGRESS)) {
        fakeTransport.setDfuWriteResponse(getDfuWriteResponse(43062, 4));
      }

      if (state.getType().equals(FirmwareUpdateState.Type.TRANSFERRED)) {
        countDownLatch.countDown();
      }
    });
    fakeTransport.setDfuWriteResponse(getDfuWriteResponse(29951, 2));

    // Act
    fwStateMachine.getState().next(FirmwareUpdateState.ofIdle());
    dfuManager.applyUpdates(Collections.singletonList(dfuInfo), false);
    countDownLatch.await(5, TimeUnit.SECONDS);

    // Assert
    assertThat(list.size()).isEqualTo(5);
    assertThat(list.get(0).getType()).isEqualTo(FirmwareUpdateState.Type.IDLE);
    assertThat(list.get(1).getType()).isEqualTo(FirmwareUpdateState.Type.PREPARING_TO_TRANSFER);
    assertThat(list.get(2).getType()).isEqualTo(FirmwareUpdateState.Type.TRANSFER_PROGRESS);
    assertThat(list.get(3).getType()).isEqualTo(FirmwareUpdateState.Type.TRANSFER_PROGRESS);
    assertThat(list.get(4).getType()).isEqualTo(FirmwareUpdateState.Type.TRANSFERRED);
  }

  @Test
  public void assignApplyUpdatesAutoExecuteFalse_actApplyFirmware_stop()
      throws InterruptedException, IOException {

    // Assign
    Signal<FirmwareUpdateState> signal = Signal.create();
    Component component = FakeComponent.getGearComponent(/* componentId= */1);
        RemoteDfuInfo remoteDfuInfo = RemoteDfuInfo.create(version,
        dfuStatus, downloadUrl.toString(), component.vendor().id(), component.product().id(),
        "");
    DFUInfo dfuInfo = DFUInfo.create(remoteDfuInfo);
    CacheRepository cacheRepository = new CacheRepositoryImpl(context);

    // Create file.
    File file = createFile("abcd", cacheRepository, dfuInfo);

    // ConnectedTag with FakeTransport
    FakeTransportImpl fakeTransport = getTransport(20);
    FakeJacquardManagerImpl fakeJacquardManager = new FakeJacquardManagerImpl(
        ApplicationProvider.getApplicationContext());
    JacquardManagerInitialization.initJacquardManager(fakeJacquardManager);

    // DfuManager
    DFUChecker dfuChecker = new DummyDFUChecker<>(remoteDfuInfo, /* success= */true)
        .createDownloadManager(file);
    FirmwareUpdateStateMachine fwStateMachine = new FirmwareUpdateStateMachine();
    DfuManagerImpl dfuManager = new DfuManagerImpl(PERIPHERAL_IDENTIFIER, dfuChecker,
        fwStateMachine, signal);

    // Fake Connected Tag
    fakeJacquardManager.setState(ConnectionState
        .ofConnected(ConnectedJacquardTagInitialization.createConnectedJacquardTag(fakeTransport,
            getDeviceInfo(), dfuManager)));

    List<FirmwareUpdateState> list = new ArrayList<>();
    CountDownLatch countDownLatch = new CountDownLatch(1);
    fwStateMachine.getState().distinctUntilChanged().onNext(state -> {
      list.add(state);
      if (state.getType().equals(FirmwareUpdateState.Type.TRANSFER_PROGRESS)) {
        dfuManager.stop();
      }

      if (state.getType().equals(FirmwareUpdateState.Type.ERROR)) {
        countDownLatch.countDown();
      }
    });
    fakeTransport.setDfuWriteResponse(getDfuWriteResponse(29951, 2));

    // Act
    fwStateMachine.getState().next(FirmwareUpdateState.ofIdle());
    dfuManager.applyUpdates(Collections.singletonList(dfuInfo), false);
    countDownLatch.await(5, TimeUnit.SECONDS);

    // Assert
    assertThat(list.size()).isEqualTo(5);
    assertThat(list.get(0).getType()).isEqualTo(FirmwareUpdateState.Type.IDLE);
    assertThat(list.get(1).getType()).isEqualTo(FirmwareUpdateState.Type.PREPARING_TO_TRANSFER);
    assertThat(list.get(2).getType()).isEqualTo(FirmwareUpdateState.Type.TRANSFER_PROGRESS);
    assertThat(list.get(3).getType()).isEqualTo(FirmwareUpdateState.Type.ERROR);
    assertThat(list.get(4).getType()).isEqualTo(FirmwareUpdateState.Type.IDLE);
  }

  @Test
  public void assignApplyUpdatesAutoExecuteTrue_actApplyFirmware_assertStateCorrect()
      throws InterruptedException, IOException {

    // Assign
    Signal<FirmwareUpdateState> signal = Signal.create();
    Component component = FakeComponent.getGearComponent(/* componentId= */1);
        RemoteDfuInfo remoteDfuInfo = RemoteDfuInfo.create(version,
        dfuStatus, downloadUrl.toString(), component.vendor().id(), component.product().id(),
        "");
    DFUInfo dfuInfo = DFUInfo.create(remoteDfuInfo);
    CacheRepository cacheRepository = new CacheRepositoryImpl(context);

    /* Create file. */
    File file = createFile("abcd", cacheRepository, dfuInfo);

    //ConnectedTag with FakeTransport
    FakeTransportImpl fakeTransport = getTransport(20);
    FakeJacquardManagerImpl fakeJacquardManager = new FakeJacquardManagerImpl(
        ApplicationProvider.getApplicationContext());
    JacquardManagerInitialization.initJacquardManager(fakeJacquardManager);

    /* DfuManager */
    DFUChecker dfuChecker = new DummyDFUChecker<>(remoteDfuInfo, /* success= */true)
        .createDownloadManager(file);
    FirmwareUpdateStateMachine fwStateMachine = new FirmwareUpdateStateMachine();
    DfuManagerImpl dfuManager = new DfuManagerImpl(PERIPHERAL_IDENTIFIER, dfuChecker,
        fwStateMachine, signal);

    /* Fake Connected Tag*/
    fakeJacquardManager.setState(ConnectionState
        .ofConnected(ConnectedJacquardTagInitialization.createConnectedJacquardTag(fakeTransport,
            getDeviceInfo(), dfuManager)));

    List<FirmwareUpdateState> list = new ArrayList<>();
    CountDownLatch countDownLatch = new CountDownLatch(1);
    fwStateMachine.getState().distinctUntilChanged().onNext(state -> {
      list.add(state);
      if (state.getType().equals(FirmwareUpdateState.Type.TRANSFER_PROGRESS)) {
        fakeTransport.setDfuWriteResponse(getDfuWriteResponse(43062, 4));
      }

      if (state.getType().equals(FirmwareUpdateState.Type.EXECUTING)) {
        fakeJacquardManager.setSendConnectedTwiceForExecute(true);
      }

      if (state.getType().equals(FirmwareUpdateState.Type.COMPLETED)) {
        countDownLatch.countDown();
      }
    });
    fakeTransport.setDfuWriteResponse(getDfuWriteResponse(29951, 2));

    // Act
    fwStateMachine.getState().next(FirmwareUpdateState.ofIdle());
    dfuManager.applyUpdates(Collections.singletonList(dfuInfo), true);
    countDownLatch.await(5, TimeUnit.SECONDS);

    // Assert
    assertThat(list.size()).isEqualTo(7);
    assertThat(list.get(0).getType()).isEqualTo(FirmwareUpdateState.Type.IDLE);
    assertThat(list.get(1).getType()).isEqualTo(FirmwareUpdateState.Type.PREPARING_TO_TRANSFER);
    assertThat(list.get(2).getType()).isEqualTo(FirmwareUpdateState.Type.TRANSFER_PROGRESS);
    assertThat(list.get(3).getType()).isEqualTo(FirmwareUpdateState.Type.TRANSFER_PROGRESS);
    assertThat(list.get(4).getType()).isEqualTo(FirmwareUpdateState.Type.TRANSFERRED);
    assertThat(list.get(5).getType()).isEqualTo(FirmwareUpdateState.Type.EXECUTING);
    assertThat(list.get(6).getType()).isEqualTo(FirmwareUpdateState.Type.COMPLETED);
  }

  @Test
  public void assignApplyModuleUpdates_actApplyFirmware_assertStateCorrect()
      throws InterruptedException, IOException {

    // Assign
    Signal<FirmwareUpdateState> signal = Signal.create();
    Component component = FakeComponent.getGearComponent(/* componentId= */1);
        RemoteDfuInfo remoteDfuInfo = RemoteDfuInfo.create(version,
        dfuStatus, downloadUrl.toString(), component.vendor().id(), component.product().id(),
        moduleId);
    DFUInfo dfuInfo = DFUInfo.create(remoteDfuInfo);
    CacheRepository cacheRepository = new CacheRepositoryImpl(context);

    //Create file.
    File file = createFile("abcd", cacheRepository, dfuInfo);

    //ConnectedTag with FakeTransport
    FakeTransportImpl fakeTransport = getTransport(20);
    FakeJacquardManagerImpl fakeJacquardManager = new FakeJacquardManagerImpl(
        ApplicationProvider.getApplicationContext());
    JacquardManagerInitialization.initJacquardManager(fakeJacquardManager);

    // DfuManager
    DFUChecker dfuChecker = new DummyDFUChecker<>(remoteDfuInfo, /* success= */true)
        .createDownloadManager(file);
    FirmwareUpdateStateMachine fwStateMachine = new FirmwareUpdateStateMachine();
    DfuManagerImpl dfuManager = new DfuManagerImpl(PERIPHERAL_IDENTIFIER, dfuChecker,
        fwStateMachine, signal);

    // Fake Connected Tag
    fakeJacquardManager.setState(ConnectionState
        .ofConnected(ConnectedJacquardTagInitialization.createConnectedJacquardTag(fakeTransport,
            getDeviceInfo(), dfuManager)));

    List<FirmwareUpdateState> list = new ArrayList<>();
    CountDownLatch countDownLatch = new CountDownLatch(1);
    fwStateMachine.getState().distinctUntilChanged().onNext(state -> {
      list.add(state);
      if (state.getType().equals(FirmwareUpdateState.Type.TRANSFER_PROGRESS)) {
        fakeTransport.setDfuWriteResponse(getDfuWriteResponse(43062, 4));
      }

      if (state.getType().equals(FirmwareUpdateState.Type.COMPLETED)) {
        countDownLatch.countDown();
      }
    });
    fakeTransport.setDfuWriteResponse(getDfuWriteResponse(29951, 2));

    // Act
    fwStateMachine.getState().next(FirmwareUpdateState.ofIdle());
    dfuManager.applyModuleUpdate(dfuInfo);
    countDownLatch.await(5, TimeUnit.SECONDS);

    // Assert
    assertThat(list.size()).isEqualTo(6);
    assertThat(list.get(0).getType()).isEqualTo(FirmwareUpdateState.Type.IDLE);
    assertThat(list.get(1).getType()).isEqualTo(FirmwareUpdateState.Type.PREPARING_TO_TRANSFER);
    assertThat(list.get(2).getType()).isEqualTo(FirmwareUpdateState.Type.TRANSFER_PROGRESS);
    assertThat(list.get(3).getType()).isEqualTo(FirmwareUpdateState.Type.TRANSFER_PROGRESS);
    assertThat(list.get(4).getType()).isEqualTo(FirmwareUpdateState.Type.TRANSFERRED);
    assertThat(list.get(5).getType()).isEqualTo(FirmwareUpdateState.Type.COMPLETED);
  }

  @Test
  public void assignApplyUpdatesAutoExecuteFalse_actApplyFirmware_fileNotFound()
      throws InterruptedException, IOException {

    // Assign
    Signal<FirmwareUpdateState> signal = Signal.create();
    Component component = FakeComponent.getGearComponent(/* componentId= */1);
        RemoteDfuInfo remoteDfuInfo = RemoteDfuInfo.create(version,
        dfuStatus, downloadUrl.toString(), component.vendor().id(), component.product().id(),
        "");
    DFUInfo dfuInfo = DFUInfo.create(remoteDfuInfo);
    CacheRepository cacheRepository = new CacheRepositoryImpl(context);

    // Create file.
    File file = createFile("", cacheRepository, dfuInfo);

    // ConnectedTag with FakeTransport
    FakeTransportImpl fakeTransport = getTransport(20);
    FakeJacquardManagerImpl fakeJacquardManager = new FakeJacquardManagerImpl(
        ApplicationProvider.getApplicationContext());
    JacquardManagerInitialization.initJacquardManager(fakeJacquardManager);

    // DfuManager
    DFUChecker dfuChecker = new DummyDFUChecker<>(remoteDfuInfo, /* success= */true)
        .createDownloadManager(file);
    FirmwareUpdateStateMachine fwStateMachine = new FirmwareUpdateStateMachine();
    DfuManagerImpl dfuManager = new DfuManagerImpl(PERIPHERAL_IDENTIFIER, dfuChecker,
        fwStateMachine, signal);

    // Fake Connected Tag
    fakeJacquardManager.setState(ConnectionState
        .ofConnected(ConnectedJacquardTagInitialization.createConnectedJacquardTag(fakeTransport,
            getDeviceInfo(), dfuManager)));

    List<FirmwareUpdateState> list = new ArrayList<>();
    CountDownLatch countDownLatch = new CountDownLatch(1);
    fwStateMachine.getState().distinctUntilChanged().onNext(state -> {
      list.add(state);
      if (state.getType().equals(FirmwareUpdateState.Type.ERROR)) {
        countDownLatch.countDown();
      }
    });
    fakeTransport.setDfuWriteResponse(getDfuWriteResponse(29951, 2));

    // Act
    fwStateMachine.getState().next(FirmwareUpdateState.ofIdle());
    dfuManager.applyUpdates(Collections.singletonList(dfuInfo), false);
    countDownLatch.await(5, TimeUnit.SECONDS);

    // Assert
    assertThat(list.size()).isEqualTo(3);
    assertThat(list.get(0).getType()).isEqualTo(FirmwareUpdateState.Type.IDLE);
    assertThat(list.get(1).getType()).isEqualTo(FirmwareUpdateState.Type.PREPARING_TO_TRANSFER);
    assertThat(list.get(2).getType()).isEqualTo(FirmwareUpdateState.Type.ERROR);
    assertThat(list.get(2).error()).isInstanceOf(FileNotFoundException.class);
  }

  @Test
  public void assignExecuteFirmwareTag_actExecuteFirmware_assertCorrect()
      throws IOException, InterruptedException {

    // Assign
    CountDownLatch countDownLatch = new CountDownLatch(1);
    Component component = FakeComponent.getTagComponent();
    RemoteDfuInfo remoteDfuInfo = RemoteDfuInfo.create(version,
        dfuStatus, downloadUrl.toString(), component.vendor().id(), component.product().id(),
        "");
    DFUInfo dfuInfo = DFUInfo.create(remoteDfuInfo);
    CacheRepository cacheRepository = new CacheRepositoryImpl(context);

    // Create file.
    File file = createFile("abcd", cacheRepository, dfuInfo);


    // ConnectedTag with FakeTransport
    FakeTransportImpl fakeTransport = getTransport(20);
    FakeJacquardManagerImpl fakeJacquardManager = new FakeJacquardManagerImpl(
        ApplicationProvider.getApplicationContext());
    JacquardManagerInitialization.initJacquardManager(fakeJacquardManager);
    fakeTransport.setDfuWriteResponse(getDfuWriteResponse(43062, 4));

    // DfuManager
    Signal<FirmwareUpdateState> signal = Signal.create();
    DFUChecker dfuChecker = new DummyDFUChecker<>(remoteDfuInfo, /* success= */true)
        .createDownloadManager(file);
    FirmwareUpdateStateMachine fwStateMachine = new FirmwareUpdateStateMachine();
    DfuManagerImpl dfuManager = new DfuManagerImpl(PERIPHERAL_IDENTIFIER, dfuChecker,
        fwStateMachine, signal);

    // Fake Connected Tag
    fakeJacquardManager.setState(ConnectionState
        .ofConnected(ConnectedJacquardTagInitialization.createConnectedJacquardTag(fakeTransport,
            getDeviceInfo(), dfuManager)));

    List<FirmwareUpdateState> list = new ArrayList<>();
    fwStateMachine.getState().distinctUntilChanged().filter(
        state -> state.getType().equals(FirmwareUpdateState.Type.EXECUTING) || state.getType()
            .equals(FirmwareUpdateState.Type.COMPLETED)).onNext(state -> {
      list.add(state);
      if (state.getType().equals(FirmwareUpdateState.Type.EXECUTING)) {
        fakeJacquardManager.setSendConnectedTwiceForExecute(true);
      }
      if (state.getType().equals(FirmwareUpdateState.Type.COMPLETED)) {
        countDownLatch.countDown();
      }
    });

    // Act
    fwStateMachine.getState().next(FirmwareUpdateState.ofIdle());
    signal.first().onNext(state -> {
      if (state.getType().equals(FirmwareUpdateState.Type.TRANSFERRED)) {
        dfuManager.executeUpdates();
      }
    });
    dfuManager.applyUpdates(Collections.singletonList(dfuInfo), false);


    countDownLatch.await(/* timeout= */5, TimeUnit.SECONDS);

    // Assert
    assertThat(list.size()).isEqualTo(2);
    assertThat(list.get(0).getType()).isEqualTo(FirmwareUpdateState.Type.EXECUTING);
    assertThat(list.get(1).getType()).isEqualTo(FirmwareUpdateState.Type.COMPLETED);
  }

  @Test
  public void assignExecuteFirmware_actNonTransferredState_assertError()
      throws IOException, InterruptedException {

    // Assign
    CountDownLatch countDownLatch = new CountDownLatch(1);
    Component component = FakeComponent.getGearComponent(/* componentId= */1);
        RemoteDfuInfo remoteDfuInfo = RemoteDfuInfo.create(version,
        dfuStatus, downloadUrl.toString(), component.vendor().id(), component.product().id(),
        "");
    DFUInfo dfuInfo = DFUInfo.create(remoteDfuInfo);
    CacheRepository cacheRepository = new CacheRepositoryImpl(context);
    JacquardManagerInitialization.initJacquardManager();

    // Create file.
    File file = createFile("abcd", cacheRepository, dfuInfo);

    // DfuManager
    Signal<FirmwareUpdateState> signal = Signal.create();
    DFUChecker dfuChecker = new DummyDFUChecker<>(remoteDfuInfo, /* success= */true)
        .createDownloadManager(file);
    FirmwareUpdateStateMachine fwStateMachine = new FirmwareUpdateStateMachine();
    DfuManagerImpl dfuManager = new DfuManagerImpl(PERIPHERAL_IDENTIFIER, dfuChecker,
        fwStateMachine, signal);
    AtomicReference<Throwable> atomicReference = new AtomicReference<>();
    signal.filter(state -> state.getType().equals(FirmwareUpdateState.Type.ERROR))
        .onNext(state -> {
          countDownLatch.countDown();
          atomicReference.set(state.error());
        });

    // Act
    fwStateMachine.getState().next(FirmwareUpdateState.ofIdle());
    dfuManager.executeUpdates();
    countDownLatch.await(2, TimeUnit.SECONDS);

    // Assert
    assertThat(atomicReference.get()).isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void assignApplyFirmware_actUpdateNotAvailable_assertStopStateMachine()
      throws IOException {

    // Assign
    AtomicReference<Throwable> atomicReference = new AtomicReference<>();
    Signal<FirmwareUpdateState> signal = Signal.create();
    Component component = FakeComponent.getGearComponent(/* componentId= */1);
        RemoteDfuInfo remoteDfuInfo = RemoteDfuInfo.create(version,
        UpgradeStatus.NOT_AVAILABLE.toString(), downloadUrl.toString(), component.vendor().id(),
        component.product().id(),
        "");
    DFUInfo dfuInfo = DFUInfo.create(remoteDfuInfo);
    CacheRepository cacheRepository = new CacheRepositoryImpl(context);

    // Create file.
    File file = createFile("abcd", cacheRepository, dfuInfo);

    //ConnectedTag with FakeTransport
    FakeTransportImpl fakeTransport = getTransport(20);
    FakeJacquardManagerImpl fakeJacquardManager = new FakeJacquardManagerImpl(
        ApplicationProvider.getApplicationContext());
    JacquardManagerInitialization.initJacquardManager(fakeJacquardManager);

    // DfuManager
    DFUChecker dfuChecker = new DummyDFUChecker<>(remoteDfuInfo, /* success= */true)
        .createDownloadManager(file);
    FirmwareUpdateStateMachine fwStateMachine = new FirmwareUpdateStateMachine();
    DfuManagerImpl dfuManager = new DfuManagerImpl(PERIPHERAL_IDENTIFIER, dfuChecker,
        fwStateMachine, signal);
    fwStateMachine.getState().filter(state -> state.getType() == FirmwareUpdateState.Type.ERROR)
        .onNext(state -> atomicReference.set(state.error()));

    // Fake Connected Tag
    fakeJacquardManager.setState(ConnectionState
        .ofConnected(ConnectedJacquardTagInitialization.createConnectedJacquardTag(fakeTransport,
            getDeviceInfo(), dfuManager)));

    // Act
    fwStateMachine.getState().next(FirmwareUpdateState.ofIdle());
    dfuManager.applyUpdates(Collections.singletonList(dfuInfo), false);

    // Assert
    assertThat(atomicReference.get()).isInstanceOf(UpdatedFirmwareNotFoundException.class);
    assertThat(atomicReference.get().getMessage()).isEqualTo("Does not have any update.");
  }

  @Test
  public void assignApplyFirmware_actIsufficientBattery_assertError()
      throws IOException {

    // Assign
    AtomicReference<Throwable> atomicReference = new AtomicReference<>();
    Signal<FirmwareUpdateState> signal = Signal.create();
    final PlatformSettings SETTINGS = new PlatformSettings();
    Component component = FakeComponent.getGearComponent(/* componentId= */1);
        RemoteDfuInfo remoteDfuInfo = RemoteDfuInfo.create(version,
        dfuStatus, downloadUrl.toString(), component.vendor().id(),
        component.product().id(),
        "");
    DFUInfo dfuInfo = DFUInfo.create(remoteDfuInfo);
    CacheRepository cacheRepository = new CacheRepositoryImpl(context);

    // Create file.
    File file = createFile("abcd", cacheRepository, dfuInfo);

    //ConnectedTag with FakeTransport
    FakeTransportImpl fakeTransport = getTransport(6);
    FakeJacquardManagerImpl fakeJacquardManager = new FakeJacquardManagerImpl(
        ApplicationProvider.getApplicationContext());
    JacquardManagerInitialization.initJacquardManager(fakeJacquardManager);

    // DfuManager
    DFUChecker dfuChecker = new DummyDFUChecker<>(remoteDfuInfo, /* success= */true)
        .createDownloadManager(file);
    FirmwareUpdateStateMachine fwStateMachine = new FirmwareUpdateStateMachine();
    DfuManagerImpl dfuManager = new DfuManagerImpl(PERIPHERAL_IDENTIFIER, dfuChecker,
        fwStateMachine, signal);
    fwStateMachine.getState().filter(state -> state.getType() == FirmwareUpdateState.Type.ERROR)
        .onNext(state -> atomicReference.set(state.error()));

    // Fake Connected Tag
    fakeJacquardManager.setState(ConnectionState
        .ofConnected(ConnectedJacquardTagInitialization.createConnectedJacquardTag(fakeTransport,
            getDeviceInfo(), dfuManager)));

    // Act
    fwStateMachine.getState().next(FirmwareUpdateState.ofIdle());
    dfuManager.applyUpdates(Collections.singletonList(dfuInfo), false);

    // Assert
    assertThat(atomicReference.get()).isInstanceOf(InsufficientBatteryException.class);
    assertThat(atomicReference.get().getMessage()).isEqualTo(
        "UJT has 6% battery. Minimum " + SETTINGS.getMinimumBatteryDFU()
            + "% battery is required to proceed.");

  }

  private File createFile(String data, CacheRepository cacheRepository, DFUInfo dfuInfo)
      throws IOException {
    byte[] outBytes = data.getBytes(StandardCharsets.UTF_8);
    InputStream in = new ByteArrayInputStream(outBytes);
    File file = new File(/* pathname= */
        cacheRepository.createParentDir(context.getFilesDir() + File.separator + "DfuImages")
            + File.separator +
            cacheRepository.descriptorFileName(dfuInfo));
    DfuUtil.inputStreamToFile(in, file, in.available()).consume();
    return file;
  }

  private static FakeTransportImpl getTransport(int batteryLevel) {
    FakePeripheral peripheral = new FakePeripheral(/* bleQueue= */ null);
    FakeFragmenter commandFragmenter = new FakeFragmenter("commandFragmenter",
        ProtocolSpec.VERSION_2.getMtuSize());
    FakeFragmenter notificationFragmenter = new FakeFragmenter("notificationFragmenter",
        ProtocolSpec.VERSION_2.getMtuSize());
    FakeFragmenter dataFragmenter = new FakeFragmenter("dataFragmenter",
        ProtocolSpec.VERSION_2.getMtuSize());
    FakeTransportState transportState = new FakeTransportState(commandFragmenter,
        notificationFragmenter, dataFragmenter);
    RequiredCharacteristics requiredCharacteristics = new RequiredCharacteristics();
    FakeTransportImpl transport = new FakeTransportImpl(peripheral, requiredCharacteristics,
        transportState);
    transport.setBatteryLevel(batteryLevel);
    return transport;
  }

  private DFUWriteResponse getDfuWriteResponse(int crc, int offSet) {
    return DFUWriteResponse.newBuilder().setCrc(crc).setOffset(offSet).build();
  }

  private static DeviceInfo getDeviceInfo() {
    return DeviceInfo.ofTag(
        DeviceInfoResponse.newBuilder().setGearId("").setSkuId("").setVendorId(1957219924)
            .setProductId(-1973006092).setUuid(FakeComponent.UUID).setVendor("").setRevision(0)
            .setModel("").setMlVersion("").setBootloaderMajor(0).setBootloaderMinor(0)
            .setFirmwarePoint(0).setFirmwareMinor(0).setFirmwareMajor(0).build());
  }

  private Map<String, Vendor> getVendors() {
    List<Capability> capabilities = new ArrayList<>();
    capabilities.add(Product.Capability.GESTURE);
    capabilities.add(Product.Capability.LED);
    List<Product> products = new ArrayList<>();
    products.add(Product.of(productId, "Product", "jq_image", capabilities));
    Map<String, Vendor> vendors = new HashMap<>();
    vendors.put(vendorId, Vendor.of(vendorId, "Vendor", products));
    return vendors;
  }
}
