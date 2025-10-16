package com.connectsdk.device;

public interface SimpleDevicePickerListener extends DevicePickerListener {
    /**
     * Called when the user selects a device.
     * This callback can be used to prepare the device (request permissions, etc)
     * just before attempting to connect.
     *
     * @param device Connectable Device
     */
    void onPrepareDevice(ConnectableDevice device);

    /**
     * Called when device is ready to use (requested permissions approved).
     *
     * @param device Connectable Device
     */
    void onPickDevice(ConnectableDevice device);

    /**
     * Called when the picker is canceled by the user or if pairing
     * was unsuccessful.
     *
     * @param canceled if picker was canceled by user, false if due to error
     */
    void onPickDeviceFailed(boolean canceled);
}