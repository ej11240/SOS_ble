package com.example.sos_ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;


public class BLEUtil {

    private static final String TAG = "SERVICE.BLEUTIL";

    //장치 연결
    public static void startConnectService(Context context, ConnectResult connectResult, BluetoothDevice device) {
        ConnectService.mConnectResult = connectResult;
        Intent intent = new Intent(context, ConnectService.class);
        intent.putExtra("deviceName", device);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("TEST", "util start");
            context.startForegroundService(intent);
        } else {
            Log.d("TEST", "util start");
            context.startService(intent);
        }
    }

    //장치 해제
    public static void stopConnectService(Context context) {
        Intent intent = new Intent(context, ConnectService.class);
        context.stopService(intent);
    }

    // 등록 장치 획득 API
    public static BluetoothDevice getRemoteDevice(Context context, String address) {
        try {
            if (address.isEmpty() || address == null) {
                return null;
            }
            BluetoothManager mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();

            if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                return null;
            }
            return mBluetoothAdapter.getRemoteDevice(address);
        } catch (Exception e) {
            Log.e(TAG, "Get remote service error", e);
        }
        return null;
    }
}
