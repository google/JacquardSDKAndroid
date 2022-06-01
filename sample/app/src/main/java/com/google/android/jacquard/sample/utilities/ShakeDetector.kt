/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.jacquard.sample.utilities

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Uses the accelerometer to detect when the user is shaking the phone.
 */
class ShakeDetector(private val sensorManager: SensorManager) {

    /**
     * Callback Listener on shake detection.
     */
    interface ShakeListener {
        fun onShakeDetected()
    }

    private var shakeListener: ShakeListener? = null
    private var shakeTimestamp: Long = 0

    companion object {
        private const val SHAKE_SLOP_TIME_MS = 500
        private const val SENSIBILITY = 2.0f
    }

    /**
     * Adds listener for shake detector.
     */
    fun addShakeDetector(shakeListener: ShakeListener) {
        this.shakeListener = shakeListener
        sensorManager.registerListener(
            sensorListener,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    /**
     * Unregister the sensor event.
     */
    fun removeShakeDetector() {
        sensorManager.unregisterListener(sensorListener)
        shakeListener = null
    }

    private val sensorListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(sensorEvent: SensorEvent) {
            val x = sensorEvent.values[0]
            val y = sensorEvent.values[1]
            val z = sensorEvent.values[2]
            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH
            val gForce = Math.sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()
            if (gForce > SENSIBILITY) {
                val now = System.currentTimeMillis()
                if (shakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                    return
                }
                shakeTimestamp = now
                shakeListener?.onShakeDetected()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, arg1: Int) {
            // Do nothing.
        }
    }

}