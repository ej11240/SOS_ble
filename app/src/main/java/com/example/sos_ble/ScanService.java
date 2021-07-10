package com.example.sos_ble;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;


import java.util.List;

public class ScanService {

    private Context mContext;
    private Activity mActivity;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private LeDeviceListAdapter mLeDeviceAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    public boolean mScanning;
    private Handler mHandler;

    private final static int BT_REQUEST_ENABLE = 1;
    private final static int PERMISSION_REQUEST_LOCATION = 2;


    //생성자
    public ScanService(Context context, Activity activity, LeDeviceListAdapter adapter) {
        mContext = context;
        mActivity = activity;

        mLeDeviceAdapter = adapter;
        mHandler = new Handler();

        //블루투스 어댑터 초기화
        mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        //블루투스 상태 확인
        activateBLE();
    }

    //Bluetooth 지원여부 확인
    public boolean checkDeviceState() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    //블루투스 상태 확인
    public void activateBLE() {
        boolean state = checkDeviceState();
        if (state) {
            //블루투스가 안켜져있을 경우 활성화
            if (!mBluetoothAdapter.isEnabled()) {
                Intent intentBluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                mActivity.startActivityForResult(intentBluetoothEnable, BT_REQUEST_ENABLE);
            }
            //위치 권한 확인
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (mContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermission();
                }
            }
        } else {
            Toast.makeText(mContext, "BLE사용불가", Toast.LENGTH_SHORT).show();
            mActivity.finishAffinity();     //어플 종료
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mActivity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_LOCATION);
        }
    }

    //BLE 기기 스캔
    public void scanDevice(final boolean enable) {
        activateBLE();       //블루투스 상태 확인
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() { //10초 스캔 제한
                    mScanning = false;
                    mBluetoothLeScanner.stopScan(mScanCallback);
                }
            }, 10000);

            //스캔 시작
            mScanning = true;
            mBluetoothLeScanner.startScan(mScanCallback);

        } else {    //스캔 종료
            mScanning = false;
            mBluetoothLeScanner.stopScan(mScanCallback);
        }
    }

    //스캔 중지
    public void stopScan() {
        if (mScanning) {
            mBluetoothLeScanner.stopScan(mScanCallback);
            mScanning = false;
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            processResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                processResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }

        private void processResult(final ScanResult result) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    @Nullable String deviceName = result.getDevice().getName();
                    if (deviceName != null && deviceName.equals("SOS-R36")) {
                        mLeDeviceAdapter.addDevice(result.getDevice());
                        mLeDeviceAdapter.notifyDataSetChanged();
                    }
                }
            });
        }
    };
}
