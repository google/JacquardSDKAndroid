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
package com.google.android.jacquard.sample

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.jacquard.sdk.log.PrintLogger

/**
 * Receiver responsible to listen to Bluetooth state change. This receiver needs to be
 * register/unregister programmatically by App with action [ ][BluetoothAdapter.ACTION_STATE_CHANGED].
 */
class BluetoothStateChangeReceiver : BroadcastReceiver() {

    companion object {
        private val TAG = BluetoothStateChangeReceiver::class.java.simpleName
    }

    override fun onReceive(context: Context, intent: Intent) {
        val bleState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
        PrintLogger.d(TAG, "State Changed Received # $bleState")
        if (bleState == BluetoothAdapter.STATE_ON) {
            onBleEnabled(context)
        }
    }

    private fun onBleEnabled(context: Context) {
        val resourceLocator = (context.applicationContext as SampleApplication).resourceLocator
        resourceLocator.preferences.knownTags.forEach {
            PrintLogger.d(TAG,  /* message= */"onBleEnabled # Connect to # " + it.address())
            resourceLocator.connectivityManager.connect(it.address()).consume()
        }
    }
}