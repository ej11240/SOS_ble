package com.example.sos_ble;

import android.bluetooth.BluetoothDevice;

public interface ConnectResult {
    void onConnectResult(boolean result, BluetoothDevice device);
    void onEmergencyLong(String key);
    void onEmergencyShort(String key);
}
