package com.hello.threads.cdc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hello.scan.BuildConfig;

import java.util.ArrayList;

public class CdcUsbDeviceReceiver extends BroadcastReceiver {

    private  IFoundTargetDeviceCallback mIFoundTargetDeviceCallback;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (usbDevice == null) return;

        if (BuildConfig.DEBUG) {
            Log.e("Hello", "UsbDevice Info == > " + usbDevice);
        }

        if (mIFoundTargetDeviceCallback != null) {
             mIFoundTargetDeviceCallback.onFoundTargetDevice(usbDevice);
        }
    }


    public void setFoundDeviceCallback(IFoundTargetDeviceCallback callback) {
        mIFoundTargetDeviceCallback = callback;
    }

    public interface IFoundTargetDeviceCallback {
        void onFoundTargetDevice(UsbDevice usbDevice);
    }
}
