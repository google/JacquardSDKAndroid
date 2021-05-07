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
package com.google.android.jacquard.sdk;

import static com.google.android.jacquard.sdk.util.Lists.unmodifiableListOf;

import android.Manifest.permission;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.companion.AssociationRequest;
import android.companion.BluetoothLeDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.ParcelUuid;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import com.google.android.jacquard.sdk.log.PrintLogger;
import com.google.android.jacquard.sdk.model.JacquardError;
import com.google.android.jacquard.sdk.model.Peripheral;
import com.google.android.jacquard.sdk.rx.Executors;
import com.google.android.jacquard.sdk.rx.Fn;
import com.google.android.jacquard.sdk.rx.Signal;
import com.google.android.jacquard.sdk.rx.Signal.Subscription;
import com.google.android.jacquard.sdk.rx.Signal.SubscriptionFactory;
import com.google.android.jacquard.sdk.tag.AdvertisedJacquardTag;
import com.google.android.jacquard.sdk.tag.AdvertisedJacquardTagImpl;
import com.google.android.jacquard.sdk.util.BluetoothSig;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/** Wrapper around {@link BluetoothAdapter}. */
class BleAdapter {

  private static final String TAG = BleAdapter.class.getSimpleName();
  private final Context context;
  private final Signal<Integer> bleStateSignal = Signal.create();
  private final BroadcastReceiver bluetoothReceiver;

  private static void stopScan(ScanCallback callback) {
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    if (bluetoothAdapter != null) {
      BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
      if (scanner != null) {
        try {
          scanner.stopScan(callback);
        } catch (IllegalStateException e) {
          PrintLogger.e(TAG, "Failed to stop scan", e);
        }
      }
    } else {
      PrintLogger.d(TAG, "Bluetooth adapter is null");
    }
  }

  /**
   * Creates a new instance of BleAdapter/
   * @param context an Android context used to get access to the {@link BluetoothAdapter}.
   */
  public BleAdapter(Context context) {
    this.context = context;
    bluetoothReceiver = registerBleReceiver(context);
  }

  /**
   * Starts scanning for Jacquard tags. Scanning will stop when the signal is unsubscribed.
   *
   * @return a {@link Signal} emitting {@link AdvertisedJacquardTag} when found.
   */
  public Signal<AdvertisedJacquardTag> startScan() {
    List<AdvertisedJacquardTag> tags = new ArrayList<>();
    return internalStartScan().map(scanResult -> {
      AdvertisedJacquardTag tag = checkIfExists(tags, scanResult);
      if (tag != null) {
        tag.rssiSignal().next(scanResult.getRssi());
        return tag;
      }
      PrintLogger.d(TAG, "New tag found.");
      tag = AdvertisedJacquardTagImpl.of(scanResult);
      tags.add(tag);
      return tag;
    });
  }

  private AdvertisedJacquardTag checkIfExists(List<AdvertisedJacquardTag> tags, ScanResult scanResult) {
    for (AdvertisedJacquardTag advertisedJacquardTag : tags) {
      if (advertisedJacquardTag.bluetoothDevice().getAddress()
              .equals(scanResult.getDevice().getAddress())) {
        return advertisedJacquardTag;
      }
    }
    return null;
  }

  /** Releases all allocated resources. */
  public void destroy() {
    context.unregisterReceiver(bluetoothReceiver);
  }

  /**
   * Starts scanning for Jacquard tags.
   * @return a {@link Signal} emitting {@link ScanResult} when devices are found.
   */
  private Signal<ScanResult> internalStartScan() {
    return Signal.create(new SubscriptionFactory<ScanResult>() {
      @NonNull
      @Override
      public Subscription onSubscribe(@NonNull Signal<ScanResult> signal) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
        ScanSettings settings =
            new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        final ScanCallback callback = new BleScanCallback(signal);

        List<ScanFilter> scanFilters = unmodifiableListOf(
            new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(BluetoothSig.JQ_SERVICE_2))
                .build());

        scanner.startScan(scanFilters, settings, callback);

        return new Subscription() {
          @Override
          protected void onUnsubscribe() {
            stopScan(callback);
            super.onUnsubscribe();
          }
        };
      }
    }).observeOn(Executors.mainThreadExecutor());
  }

  /**
   * Connects to the provided Bluetooth device.
   * @param bluetoothDevice the device to connect to.
   * @return a {@link Signal} emitting {@link ConnectState}.
   */
  public Signal<ConnectState> connect(Context activityContext, BluetoothDevice bluetoothDevice,
      Fn<IntentSender, Signal<Boolean>> senderHandler) {
    // We need to bond the device before calling connectGatt.
    // Else it will show Pair Request dialog twice.
    List<Subscription> subscriptions = new ArrayList<>();
    return Signal.create(signal -> {
      subscriptions.add(associateAndPair(activityContext, bluetoothDevice, senderHandler)
          .filter(isBonded -> isBonded)
          .onNext(isBonded -> subscriptions
              .add(createConnectSignal(bluetoothDevice).forward(signal))));
      return new Subscription() {
        @Override
        protected void onUnsubscribe() {
          PrintLogger.d(TAG, "connect onUnsubscribe");
          for (Subscription subscription : subscriptions) {
            subscription.unsubscribe();
          }
          subscriptions.clear();
          super.onUnsubscribe();
        }
      };
    });
  }

  private Signal<Boolean> createBond(BluetoothDevice bluetoothDevice) {
    return Signal.<Boolean>create(signal -> {
      if (isAlreadyBonded(bluetoothDevice)) {
        signal.next(/* isBonded= */true);
        return new Subscription();
      }
      registerBondStateChangeReceiver(bluetoothDevice).forward(signal);
      bluetoothDevice.createBond();
      return new Subscription();
    }).observeOn(Executors.mainThreadExecutor());
  }

  /**
   * Automatically associates a companion device (on android 10+) and pairs it.
   *
   * <p>Prior to android 10, the behavior is the same.
   *
   * @return success or failure, failure can occur if the user dismisses or cancels the dialog.
   */
  private Signal<Boolean> associateAndPair(Context activityContext,
      final @NonNull BluetoothDevice bluetoothDevice,
      Fn<IntentSender, Signal<Boolean>> senderHandler) {
    if (Build.VERSION.SDK_INT < VERSION_CODES.Q) {
      return createBond(bluetoothDevice);
    }
    return Signal.from(isAlreadyAssociated(bluetoothDevice.getAddress()))
        .flatMap(
            associated -> {
              PrintLogger.d(TAG, "Tag association state " + associated);
              if (associated) {
                return isAlreadyBonded(bluetoothDevice) ? Signal.from(true)
                    : createBond(bluetoothDevice);
              } else {
                return associateDevice(activityContext, bluetoothDevice, senderHandler);
              }
            });
  }

  /**
   * Returns {@link BluetoothDevice} with the provided address.
   * Returns null if the device is not found.
   * @param address the bluetooth address to look for.
   * @return The matching {@link BluetoothDevice} or null if not found.
   */
  @RequiresPermission(permission.BLUETOOTH)
  @Nullable
  public BluetoothDevice getDevice(String address) {
    try {
      return BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
    } catch (Exception e) {
      return null;
    }
  }

  private BroadcastReceiver registerBleReceiver(Context context) {
    IntentFilter filter = new IntentFilter();
    filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

    BroadcastReceiver receiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        bleStateSignal
            .next(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR));
      }
    };

    context.registerReceiver(receiver, filter);
    return receiver;
  }

  private Signal<ConnectState> createConnectSignal(BluetoothDevice bluetoothDevice) {
    return Signal.<ConnectState>create(signal -> {

      // Queue for synchronising BLE operations.
      BleQueue bleQueue = new BleQueue();

      BluetoothGattCallback callback = new BleGattCallback(signal, bleQueue, bluetoothDevice);
      PrintLogger.d(TAG, "createConnectSignal #");
      AtomicReference<BluetoothGatt> gattReference = new AtomicReference<>(
          bluetoothDevice.connectGatt(context, true, callback));
      Subscription bleSubscription = bleStateSignal.onNext(state -> {
        if (state == BluetoothAdapter.STATE_OFF) {
          signal.next(ConnectState
              .ofDisconnected(new Peripheral(gattReference.get(), bleQueue),
                  JacquardError.ofBluetoothOffError()));
          if (gattReference.get() != null) {
            gattReference.get().close();
            gattReference.set(null);
          }
        }
      });
      return new Subscription() {
        @Override
        protected void onUnsubscribe() {
          PrintLogger.d(TAG, "createConnectSignal onUnsubscribe #");
          if (gattReference.get() != null) {
            gattReference.get().close();
            gattReference.set(null);
          }
          bleSubscription.unsubscribe();
        }
      };
    }).observeOn(Executors.mainThreadExecutor());
  }

  private Signal<Boolean> registerBondStateChangeReceiver(BluetoothDevice device) {

    return Signal.create(signal -> {

      IntentFilter filter = new IntentFilter();
      filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);

      BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          BluetoothDevice extraDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

          if (extraDevice == null) {
            PrintLogger.d(TAG, "extraDevice null");
            return;
          }

          if (!extraDevice.getAddress().equalsIgnoreCase(device.getAddress())) {
            PrintLogger.d(TAG, "extraDevice is different");
            return;
          }

          int bondState = intent
              .getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
          if (bondState == BluetoothDevice.BOND_NONE) {
            signal.next(/* isBonded= */false);
            context.unregisterReceiver(this);
            PrintLogger.d(TAG, "Bond state BOND_NONE for address: " + device.getAddress());
          } else if (bondState == BluetoothDevice.BOND_BONDED) {
            signal.next(/* isBonded= */true);
            context.unregisterReceiver(this);
          }
        }
      };

      context.registerReceiver(receiver, filter);

      return new Subscription();
    });
  }

  private boolean isAlreadyBonded(BluetoothDevice bluetoothDevice) {
    Set<BluetoothDevice> bondedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
    boolean isDeviceInBondedList = bondedDevices != null && bondedDevices.contains(bluetoothDevice);
    boolean isDeviceBonded = bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED;
    if (isDeviceInBondedList != isDeviceBonded) {
      PrintLogger.d(TAG, String.format(
          "Inconsistent device pairing state found address=%s isDeviceInBondedList=%s isDeviceBonded=%s",
          bluetoothDevice.getAddress(), isDeviceInBondedList, isDeviceBonded));
    }
    return isDeviceInBondedList && isDeviceBonded;
  }

  /**
   * Checks whether this device has already been associated with {@link CompanionDeviceManager}.
   *
   * @return true if before android 10, otherwise the state of association.
   */
  private boolean isAlreadyAssociated(final String address) {
    if (Build.VERSION.SDK_INT < VERSION_CODES.Q) {
      return true;
    }
    CompanionDeviceManager cdm = context.getSystemService(CompanionDeviceManager.class);
    List<String> associations = cdm.getAssociations();
    boolean alreadyAssociated = false;
    PrintLogger.d(TAG, "already associated devices: " + associations);
    for (String addr : associations) {
      alreadyAssociated = address.equalsIgnoreCase(addr);
      if (alreadyAssociated) {
        break;
      }
    }
    return alreadyAssociated;
  }

  @RequiresApi(api = VERSION_CODES.O)
  private Signal<Boolean> associateDevice(Context activityContext,
      final @NonNull BluetoothDevice bluetoothDevice,
      Fn<IntentSender, Signal<Boolean>> senderHandler) {
    return Signal.create(
        signal -> {
          CompanionDeviceManager cdm = activityContext
              .getSystemService(CompanionDeviceManager.class);
          AssociationRequest request =
              new AssociationRequest.Builder()
                  .addDeviceFilter(
                      new BluetoothLeDeviceFilter.Builder()
                          .setScanFilter(
                              new ScanFilter.Builder()
                                  .setDeviceAddress(bluetoothDevice.getAddress()).build())
                          .build())
                  .setSingleDevice(true)
                  .build();

          CompanionDeviceManager.Callback cb =
              new CompanionDeviceManager.Callback() {
                @Override
                public void onDeviceFound(IntentSender deviceChooser) {
                  if (senderHandler == null) {
                    PrintLogger.d(TAG, "can not show device acceptance prompt");
                    return;
                  }
                  PrintLogger.d(TAG, "showing device acceptance prompt");
                  senderHandler
                      .apply(deviceChooser)
                      .flatMap(
                          result -> result ? createBond(bluetoothDevice) : Signal.from(false))
                      .observe(new Signal.ForwarderObserver<>(signal));
                }

                @Override
                public void onFailure(CharSequence error) {
                  PrintLogger.e(TAG, "Failed to find a device to associate: " + error);
                  signal.next(false);
                  signal.complete();
                }
              };
          PrintLogger.d(TAG,
              "Requesting companion device association with: " + bluetoothDevice.getAddress());
          cdm.associate(request, cb, null);
          return new Signal.Subscription();
        });
  }
}
