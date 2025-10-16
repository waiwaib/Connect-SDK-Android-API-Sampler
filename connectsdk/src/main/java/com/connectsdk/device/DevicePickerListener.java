package com.connectsdk.device;

public interface DevicePickerListener {
    /**
     * Called when the user selects a device.
     * @param device Connectable device
     */
    void onPickDevice(ConnectableDevice device);

    /**
     * Called when the picker fails or was cancelled by the user.
     * @param canceled if picker was canceled by user, false if due to error
     */
    void onPickDeviceFailed(boolean canceled);
}