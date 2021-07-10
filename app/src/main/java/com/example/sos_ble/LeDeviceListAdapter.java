package com.example.sos_ble;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


import com.example.rescueone.R;

import java.util.ArrayList;

public class LeDeviceListAdapter extends BaseAdapter {
    private ArrayList<BluetoothDevice> bleDevices;

    public LeDeviceListAdapter(){
        super();
        bleDevices = new ArrayList<BluetoothDevice>();
    }

    //스캔한 디바이스 추가
    public void addDevice(BluetoothDevice dv){
        if(!bleDevices.contains(dv)){
            bleDevices.add(dv);
        }
    }

    //디바이스 선택
    public BluetoothDevice getDevice(int position){
        return bleDevices.get(position);
    }

    //목록 초기화
    public void clear(){
        bleDevices.clear();
    }

    @Override
    public int getCount() {
        return bleDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return bleDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        //리스트뷰 최적화
        if(view == null){
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.bledevice_item,viewGroup,false);
            viewHolder = new ViewHolder();
            viewHolder.deviceName = (TextView)view.findViewById(R.id.itemName);
            viewHolder.deviceAddress = (TextView)view.findViewById(R.id.itemAddress);
            view.setTag(viewHolder);
        }
        else{
            viewHolder = (ViewHolder)view.getTag();
        }

        //스캔한 BLE기기명 리스트로 출력
        BluetoothDevice device = bleDevices.get(i);
        String devicename = device.getName();
        String deviceAddress;
        if(device.getUuids() != null) {
            deviceAddress = device.getUuids()[0].getUuid().toString();
        }
        else{
            deviceAddress = device.getAddress();
        }
        if (devicename != null && devicename.length()>0) {
            viewHolder.bind(devicename, deviceAddress);
        }
        else {
            viewHolder.bind("Unknown Device", deviceAddress);
        }
        return view;
    }

    static class ViewHolder{
        public TextView deviceName;
        public TextView deviceAddress;

        public void bind(String name, String address){
            deviceName.setText(name);
            deviceAddress.setText(address);
        }
    }
}
