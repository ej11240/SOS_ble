package com.example.sos_ble;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;


import com.example.R;
import com.example.MainActivity;
import com.example.FakeCallReceiver;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.UUID;

public class ConnectService extends Service {

    private static final String TAG = "SERVICE.CONNECT";

    public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID RX_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID TX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public static ConnectResult mConnectResult;

    private BluetoothGatt mBluetoothGatt;
    private BluetoothDevice mDevice;
    private int mConnectionState = STATE_DISCONNECTED;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"Service create");
        buildNotification();
        Toast.makeText(this,"실행",Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"Service start command");
        if(intent == null) {
            Log.d(TAG,"Intent is null");
            return START_NOT_STICKY;
        }
        else{
            Log.d(TAG,"Get and connect device");
            BluetoothDevice device = intent.getParcelableExtra("deviceName");
            connect(device);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"Service destroy");
        try{
            if(mBluetoothGatt != null && mBluetoothGatt.connect()){
                mBluetoothGatt.disconnect();
                mBluetoothGatt.close();
                mBluetoothGatt = null;
            }
            super.onDestroy();
        }
        catch(Exception e){
            Log.e(TAG,"Service destroy error", e);
            super.onDestroy();
        }
    }

    //BLE 연결
    public void connect(@NonNull BluetoothDevice device){
        mDevice = device;
        if(mBluetoothGatt == null){
            mBluetoothGatt = device.connectGatt(this,true, mGattCallback);
            mConnectionState = STATE_CONNECTING;
        }
        else{
            if(mBluetoothGatt.connect()){
                mConnectionState = STATE_CONNECTING;
                return;

            }
        }
    }

    public void disconnect() {
        if(mBluetoothGatt == null) {
            return;
        }
        Log.d(TAG,"Disconnect GATT Server");
        mBluetoothGatt.disconnect();
    }

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        Log.d(TAG,"Close GATT Server");
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        /*연결상태 변경 시 호출*/
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if(newState == BluetoothProfile.STATE_CONNECTED){
                Log.d(TAG,"Trying to discover service");
                mConnectionState = STATE_CONNECTED;
                gatt.discoverServices();
                new Handler(Looper.getMainLooper()).post(() -> {
                    mConnectResult.onConnectResult(true, mDevice);
                });
            }
            else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                Log.d(TAG,"Disconnected from GATT Server");
                mConnectionState = STATE_DISCONNECTED;
                new Handler(Looper.getMainLooper()).post(() -> {
                    mConnectResult.onConnectResult(false, mDevice);
                });
            }
            else {
                Log.d(TAG,"GATT State:"+ newState);
                new Handler(Looper.getMainLooper()).post(() -> {
                    mConnectResult.onConnectResult(false, mDevice);
                });
            }
        }

        /*연결된 BLE의 GATT서비스 발견시 호출*/
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            try {
                if (status == BluetoothGatt.GATT_FAILURE)
                    return;
                BluetoothGattService RxService = gatt.getService(RX_SERVICE_UUID);
                BluetoothGattCharacteristic TxCharacteristic = RxService.getCharacteristic(TX_CHAR_UUID);
                gatt.setCharacteristicNotification(TxCharacteristic, true);
                BluetoothGattDescriptor descriptor = TxCharacteristic.getDescriptor(CCCD);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
                Log.d(TAG,"Service Discovery success");
            }
            catch (Exception e){
                Log.e(TAG,"onServicesDiscovered Error",e);
            }
        }

        /*GATT 서비스의 특성을 읽었을 때 호출*/
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                readCharacteristic(characteristic);
            }
        }

        /*GATT 서비스의 특성 값이 바뀔 때 호출*/
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            readCharacteristic(characteristic);
        }
    };

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        Log.d(TAG,"Read Characteristic");
        if(characteristic.getUuid().equals(TX_CHAR_UUID)){
            try {
                String key = new String(characteristic.getValue(),"UTF-8");
                Log.d(TAG, key);
                if(key.indexOf("S")==0 && key !=null){
                    Log.d(TAG,"나는 숏");
                    mConnectResult.onEmergencyShort(key);
                }else{
                    Log.d(TAG,"나는 롱이다");
                    mConnectResult.onEmergencyLong(key);
                }
                //new Handler(Looper.getMainLooper()).post(() -> {


            } catch (UnsupportedEncodingException e) {
                Log.e(TAG,"Encoding error",e);
                e.printStackTrace();
            }
        }
    }
//    private void setFakeCall() {
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTimeInMillis(System.currentTimeMillis()+1000);  //1초후 울림
//
//        Intent intent = new Intent(getApplicationContext(), FakeCallReceiver.class);
//        AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplication(),0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
//
//        if(Build.VERSION.SDK_INT > 23) {
//            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
//        }
//        else if(Build.VERSION.SDK_INT > 19) {
//            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
//        }
//    }



    /*OREO이상 백그라운드 서비스 실행을 위하여 notification 필수*/
    private void buildNotification() {
        String CHANNEL_ID = "RESCUE ONE";
        int NOTIFICATION_ID = 101;

        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(this, MainActivity.class);    //알림 클릭시 실행 액티비티(메인)
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0, notificationIntent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);

        //OREO API 26 이상
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder.setSmallIcon(R.drawable.ic_launcher_foreground); //mipmap 사용시 Oreo 이상에서 시스템 UI 에러남
            String channelName ="RESCUE ONE";
            int importance = NotificationManager.IMPORTANCE_LOW;

            //채널
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channelName, importance);

            if (notificationManager != null) {
                //채널을 시스템에 설정
                notificationManager.createNotificationChannel(channel);
            }
        }
        else {
            builder.setSmallIcon(R.mipmap.ic_launcher) // Oreo 이하에서 mipmap 없으면 에러발생
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round));   //Oreo 이하에서만 큰 배지 아이콘 띄움
        }

        builder.setAutoCancel(false)
                .setContentTitle("RESCUE ONE이 실행 중 입니다.")
                .setContentIntent(pendingIntent);

        Log.d("TEST", "noti start");
        // 노티피케이션 실행
        startForeground(NOTIFICATION_ID,builder.build());
    }
}
