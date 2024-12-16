package com.hello.threads.cdc;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.hello.IScanner;
import com.hello.scan.BuildConfig;
import com.hello.scan.ScanCallBack;
import com.hello.utils.IoUtil;
import com.hello.utils.SoundUtil;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class CdcScannerThread extends Thread implements IScanner, CdcUsbDeviceReceiver.IFoundTargetDeviceCallback {


    public static final List<String> sSupportDevice = new ArrayList<>();

    static {
        //"|"线的左边是vid，右边是pid
        sSupportDevice.add("7851|7430");
        sSupportDevice.add("1659|57841");
        sSupportDevice.add("7851|13574");
        sSupportDevice.add("7851|13318");
    }

    private final CdcUsbDeviceReceiver mReceiver = new CdcUsbDeviceReceiver();
    private final PostDataTask mPostDataTask = new PostDataTask();
    private final StringBuilder mStrBuilder = new StringBuilder();
    private final WeakReference<Context> mContext;
    private final Object mLock = new Object();
    private final Handler mHandler;
    private UsbInterface mUsbInterface;
    private UsbDeviceConnection mUsbDeviceCon;
    private UsbEndpoint mWriteEndpoint;
    private UsbEndpoint mReadEndpoint;
    private UsbRequest mUsbRequest;
    private boolean mPlaySoundFlag = false;
    private volatile ScanCallBack mScanCallBack;
    private ByteBuffer mByteBuffer;

    public CdcScannerThread(Context context) {
        mContext = new WeakReference<>(context.getApplicationContext());
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public synchronized void startScan() {
        UsbDevice targetDevice = findTargetDevice(mContext.get());
//        Log.d("hello", "run: inside?:"+targetDevice);
        if (targetDevice == null) {
            mReceiver.setFoundDeviceCallback(this);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            mContext.get().registerReceiver(mReceiver, intentFilter);
            //没有做上电动作，直接返回false，初始化失败
            if (!IoUtil.turnOn()) {
                if (mScanCallBack != null) mScanCallBack.onInitScan(false);
            }
        } else {
            Log.d("hello", "startScan: "+mScanCallBack);
            if (connectTargetDevice(targetDevice)) startLoop();
        }
    }

    private void initStartScan(){

    }

    private void startLoop() {
        Log.e("Hello", "CdcScannerThread.startLoop == > call");
        mUsbRequest = new UsbRequest();
        if (mUsbDeviceCon == null || mReadEndpoint == null) return;
        if (!mUsbRequest.initialize(mUsbDeviceCon, mReadEndpoint)) return;
        mByteBuffer = ByteBuffer.allocate(mReadEndpoint.getMaxPacketSize());
        if (!isAlive()) start();
    }

    @Override
    public void setCallBack(ScanCallBack pScanCallBack) {
        Log.d("hello", "setCallBack: 看看是否为空"+pScanCallBack);
        mScanCallBack = pScanCallBack;
    }

    @Override
    public void sendData(byte[] pBytes) {
        if (mWriteEndpoint != null && mUsbDeviceCon != null) {
            mUsbDeviceCon.bulkTransfer(mWriteEndpoint, pBytes, pBytes.length, 50);
        }
    }

    @Override
    public void playSound(boolean pPlay) {
        mPlaySoundFlag = pPlay;
    }

    @Override
    public synchronized void stopScan() {
        Log.e("Hello", "CdcScannerThread.stopScan == > call");
        interrupt();
        mScanCallBack = null;
        if (mUsbRequest != null) mUsbRequest.cancel();
        synchronized (mLock) {
            Log.e("Hello", "CdcScannerThread.stopScan == > 同步块内");
            if (mUsbRequest != null) {
                mUsbRequest.close();
                mUsbRequest = null;
            }
            try {
                mReceiver.setFoundDeviceCallback(null);
                mContext.get().unregisterReceiver(mReceiver);
            } catch (Exception ignored) {
            }
            mReadEndpoint = null;
            mWriteEndpoint = null;
            if (mUsbDeviceCon != null) {
                if (mUsbInterface != null) mUsbDeviceCon.releaseInterface(mUsbInterface);
                mUsbDeviceCon.close();
                mUsbDeviceCon = null;
            }
        }
        IoUtil.turnOff();
    }

    @Override
    public void run() {
        mHandler.post(() -> {
            if (mScanCallBack != null) mScanCallBack.onInitScan(true);
        });
        while(true){
            byte[] bytes = new byte[mReadEndpoint.getMaxPacketSize()];
            int baData = this.mUsbDeviceCon.bulkTransfer(this.mReadEndpoint, bytes, bytes.length, 500);
            if (baData < 0) break;
        }
        while (!isInterrupted()) {
            synchronized (mLock) {
                try {
                    if (mUsbRequest == null) break;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        mUsbRequest.queue(mByteBuffer);
                    } else {
                        mUsbRequest.queue(mByteBuffer, mReadEndpoint.getMaxPacketSize());
                    }



                    if (mUsbDeviceCon.requestWait() != mUsbRequest) continue;

                    int position = mByteBuffer.position();


                    if (position <= 0 || mScanCallBack == null) continue;
                    byte[] bytes = Arrays.copyOfRange(mByteBuffer.array(), 0, position);


                    String data = new String(bytes, StandardCharsets.UTF_8).trim();
                    mStrBuilder.append(data);
                    Log.d("Hello", "{cdc scan complete}");
                    postData();
                    Thread.sleep(1);

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    mByteBuffer.clear();
                }
            }
        }
//        mStrBuilder.delete(0,mStrBuilder.length());
        mHandler.removeCallbacksAndMessages(null);
        Log.e("Hello", "CdcScannerThread.run == > 退出循环");
    }

    private void postData() {
        mHandler.removeCallbacks(mPostDataTask);
        mHandler.postDelayed(mPostDataTask, 15);    //延迟15毫秒后运行Runnable对象mPostDataTask
    }

    private UsbDevice findTargetDevice(Context context) {
        Log.e("Hello", "CdcScannerThread.findTargetDevice == > call");
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        Log.d("hello", "run: inside?"+usbManager);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        for (String key : deviceList.keySet()) {
            UsbDevice usbDevice = deviceList.get(key);
            Log.e("Hello", "findTargetDevice: " +  usbDevice );
            if (usbDevice == null) continue;
            String pidNVid = usbDevice.getVendorId() + "|" + usbDevice.getProductId();
            Log.d("hello", "run: inside!!!!!"+usbDevice.getVendorId() + "|" + usbDevice.getProductId());
                if (sSupportDevice.contains(pidNVid)) {
                return usbDevice;
            }
        }
        return null;
    }

    private boolean connectTargetDevice(UsbDevice usbDevice) {
        Log.e("Hello", "CdcScannerThread.connectTargetDevice == > call");
        boolean connectResult = false;
        for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
            mUsbInterface = usbDevice.getInterface(i);
            int endpointCount = mUsbInterface.getEndpointCount();
            if (endpointCount < 2) continue;
            for (int j = 0; j < endpointCount; j++) {
                UsbEndpoint endpoint = mUsbInterface.getEndpoint(j);
                if (endpoint.getType() != UsbConstants.USB_ENDPOINT_XFER_BULK) continue;
                int direction = endpoint.getDirection();
                if (direction == UsbConstants.USB_DIR_IN) {
                    mReadEndpoint = endpoint;
                } else if (direction == UsbConstants.USB_DIR_OUT) {
                    mWriteEndpoint = endpoint;
                } else if (mReadEndpoint != null && mWriteEndpoint != null) {
                    break;
                }
            }
            if (mReadEndpoint == null || mWriteEndpoint == null) continue;
            UsbManager usbManager = (UsbManager) mContext.get().getSystemService(Context.USB_SERVICE);
            if (!usbManager.hasPermission(usbDevice)) {
                boolean b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
                int flag = b ? PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;
                Intent intent = new Intent(mContext.get().getPackageName() + ".USB_PERMISSION");
                PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext.get(), 0, intent, flag);
                usbManager.requestPermission(usbDevice, pendingIntent);
            }
            mUsbDeviceCon = usbManager.openDevice(usbDevice);
            if (mUsbDeviceCon == null || !mUsbDeviceCon.claimInterface(mUsbInterface, true)) break;
            connectResult = true;
            break;
        }
        return connectResult;
    }

    @Override
    public void onFoundTargetDevice(UsbDevice usbDevice) {
        if (usbDevice == null) return;
        Log.e("Hello", "pid == > " + usbDevice.getProductId());
        Log.e("Hello", "vid == > " + usbDevice.getVendorId());
        String pidNVid = usbDevice.getVendorId() + "|" + usbDevice.getProductId();
        if (sSupportDevice.contains(pidNVid) && !isAlive()) {
            if (connectTargetDevice(usbDevice)) startLoop();
        }
    }

    public class PostDataTask implements Runnable {

        @Override
        public void run() {
            if (mScanCallBack == null) return;
            String s = mStrBuilder.toString();
            if (BuildConfig.DEBUG) {
                Log.e("Hello", "CdcScannerThread 原始数据：{" + s + "}");
            }
            //去掉回车换行符
            for (int i = 0; i < 2; i++) {
                if (s.endsWith("\r")) {
                    s = s.substring(0, s.length() - 1);
                } else if (s.endsWith("\n")) {
                    s = s.substring(0, s.length() - 1);
                }
            }

            mScanCallBack.onScanCallBack(s);
            mScanCallBack.onScanCallBack(s.getBytes(StandardCharsets.UTF_8));
            if (mPlaySoundFlag) SoundUtil.get().playSound();
            mStrBuilder.setLength(0);
        }
    }
}
