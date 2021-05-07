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
 *
 */

package com.google.android.jacquard.sample.utilities;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import com.google.android.jacquard.sdk.log.PrintLogger;

/**
 * Uses the accelerometer to detect when the user is shaking the phone.
 */
public class ShakeDetector {

  private ShakeListener shakeListener;
  private final SensorManager sensorManager;
  private static final int SHAKE_SLOP_TIME_MS = 500;
  private static final float SENSIBILITY = 2.0f;

  private long shakeTimestamp;

  public ShakeDetector(Context context) {
    sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
  }

  /**
   * Adds listener for shake detector.
   */
  public void addShakeDetector(ShakeListener shakeListener){
    this.shakeListener = shakeListener;
    sensorManager.registerListener(
        sensorListener,
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
        SensorManager.SENSOR_DELAY_NORMAL);
  }

  /**
   * Unregister the sensor event.
   */
  public void removeShakeDetector() {
    if (sensorManager != null) {
      sensorManager.unregisterListener(sensorListener);
    }
    shakeListener = null;
  }

  private final SensorEventListener sensorListener =
      new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {

          float x = sensorEvent.values[0];
          float y = sensorEvent.values[1];
          float z = sensorEvent.values[2];

          float gX = x / SensorManager.GRAVITY_EARTH;
          float gY = y / SensorManager.GRAVITY_EARTH;
          float gZ = z / SensorManager.GRAVITY_EARTH;

          float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

          if (gForce > SENSIBILITY) {
            final long now = System.currentTimeMillis();
            if (shakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
              return;
            }
            shakeTimestamp = now;
            if (shakeListener != null) {
              shakeListener.onShakeDetected();
            }
          }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int arg1) {
          // Do nothing.
        }
      };

  /**
   * Callback Listener on shake detection.
   */
  public interface ShakeListener {
    void onShakeDetected();
  }
}
