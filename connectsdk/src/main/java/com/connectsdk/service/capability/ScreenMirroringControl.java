/*
 * ScreenMirroringApi
 * Connect SDK
 *
 * Copyright (c) 2020 LG Electronics.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.connectsdk.service.capability;

import android.app.ActivityManager;
import android.app.Presentation;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.ChecksSdkIntAtLeast;

import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.discovery.DiscoveryManager;


import java.util.ArrayList;
import java.util.List;

/** @noinspection unused*/
public interface ScreenMirroringControl extends CapabilityMethods {
    String Any = "ScreenMirroringControl.Any";
    String ScreenMirroring = "ScreenMirroringControl.ScreenMirroring";
    String[] Capabilities = {ScreenMirroring};


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Static functions

    /// ////////////////////////////////////////////////////////////////////////////////////////////
    static int getSdkVersion(Context context) {
        return -1;
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    static boolean isCompatibleOsVersion() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }

    static boolean isRunning(Context context) {
        return false;
    }

    static boolean isSupportScreenMirroring(String deviceId) {
        ConnectableDevice dvc = DiscoveryManager.getInstance().getDeviceById(deviceId);
        List<String> capabilities = (dvc != null) ? dvc.getCapabilities() : new ArrayList<>();
        return capabilities.contains(ScreenMirroringControl.ScreenMirroring);
    }

    static boolean isUibcEnabled(Context context) {
        String prefString = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return prefString != null && prefString.contains(context.getPackageName() + "/" + "UibcAccessibilityService");
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Interfaces

    /// ////////////////////////////////////////////////////////////////////////////////////////////
    ScreenMirroringControl getScreenMirroringControl();

    void startScreenMirroring(Context context, Intent projectionData, ScreenMirroringStartListener onStartListener);

    /**
     * @noinspection rawtypes
     */
    void startScreenMirroring(Context context, Intent projectionData, Class secondScreenClass, ScreenMirroringStartListener onStartListener);

    void stopScreenMirroring(Context context, ScreenMirroringStopListener stopListener);

    void setErrorListener(Context context, ScreenMirroringErrorListener errorListener);


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Listeners

    /// ////////////////////////////////////////////////////////////////////////////////////////////
    enum ScreenMirroringError {
        ERROR_GENERIC,
        ERROR_CONNECTION_CLOSED,
        ERROR_DEVICE_SHUTDOWN,
        ERROR_RENDERER_TERMINATED,
        ERROR_STOPPED_BY_NOTIFICATION
    }

    interface ScreenMirroringStartListener {
        void onPairing();

        void onStart(boolean result, Presentation secondScreen);
    }

    interface ScreenMirroringStopListener {
        void onStop(boolean result);
    }

    interface ScreenMirroringErrorListener {
        void onError(ScreenMirroringError screenMirroringError);
    }
}