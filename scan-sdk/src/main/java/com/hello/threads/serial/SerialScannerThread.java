package com.hello.threads.serial;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.hello.IScanner;
import com.hello.SerialPortFinder;
import com.hello.scan.BuildConfig;
import com.hello.scan.ScanCallBack;
import com.hello.utils.IoUtil;
import com.hello.utils.SoundUtil;
import com.telpo.tps550.api.DeviceAlreadyOpenException;
import com.telpo.tps550.api.serial.Serial;
import com.telpo.tps550.api.util.StringUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class SerialScannerThread extends Thread implements IScanner {
    private final SerialPortFinder mSpf = new SerialPortFinder();
    private final Handler mHandler;
    public volatile ScanCallBack mScanCallBack;
    private boolean mPlaySound = false;
    private int mBytesAvailable = 0;
    private int mCount = 0;
    private int mBr = 0;
    private String mPath = "/";
    private Serial mSerial;
    private InputStream mInputStream;
    private OutputStream mOutputStream;

    public SerialScannerThread() {
        mHandler = new Handler(Looper.getMainLooper());
    }

    public void setInitParam(int pBr, String pPath) {
        mBr = pBr;
        mPath = pPath;
    }

    @Override
    public void startScan() {
        start();
    }

    public void setCallBack(ScanCallBack pScanCallBack) {
        mScanCallBack = pScanCallBack;
    }

    /**
     * 发送数据
     *
     * @param pBytes
     */
    public void sendData(byte[] pBytes) {
        if (mOutputStream != null) {
            try {
                mOutputStream.write(pBytes);
                mOutputStream.flush();
            } catch (IOException pE) {
                pE.printStackTrace();
            }
        }
    }

    @Override
    public void playSound(boolean pPlay) {
        mPlaySound = pPlay;
    }

    /**
     * 停止接收数据
     */
    public void stopScan() {
        interrupt();
        IoUtil.turnOff();
        mScanCallBack = null;
    }

    @Override
    public void run() {
        //需要上下点的读头，先上下电
        IoUtil.turnOn();
        //第一个循环：检测串口状态，挂掉需要重新拉起扫码头
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    //若是cdc模式，上电后需要等usb设备挂载到系统中
                    if (mPath.contains("ttyACM") && !waitForCdcDevice()) {
                        Log.e("Hello", "没有发现扫码器！");
                        interrupt();
                    }
                    //初始化串口
                    initSerial();
                    //第二个循环：清空串口缓存数据
                    clearSerialCatchData();
                    //第三个循环：实时监听串口数据
                    readSerialData();
                } catch (FileNotFoundException pE) {
                    Log.e("Hello", "打开串口失败，请检查串口地址是否正确：" + mPath);
                    mHandler.post(() -> {
                        if (mScanCallBack != null) mScanCallBack.onInitScan(false);
                    });
                } catch (InterruptedException ignored) {
                    break;
                } catch (Exception pE) {
                    pE.printStackTrace();
                } finally {
                    clearSerial();
                }
                Thread.sleep(300);
            }
        } catch (Exception pE) {
            pE.printStackTrace();
        } finally {
            clearSerial();
        }
        //需要下电的读头，退出循环需要下电
        IoUtil.turnOff();
        if (mHandler != null) mHandler.removeCallbacksAndMessages(null);
        Log.e("Hello", "{退出扫码线程}");
    }

    /**
     * 清空串口缓存数据
     */
    private void clearSerial() {
        IoUtil.close(mInputStream);
        mInputStream = null;
        IoUtil.close(mOutputStream);
        mOutputStream = null;
        if (mSerial != null) mSerial.close();
        mSerial = null;
    }

    /**
     * cdc模式先，等待串口上来
     *
     * @return
     * @throws InterruptedException
     */
    private boolean waitForCdcDevice() throws InterruptedException {
        boolean initCdcModelResult = false;
        for (int i = 0; i < 25; i++) {
            mPath = mSpf.getAcmDevicesPath();
            Log.e("Hello", "Serial Path == > " + mPath);
            if (!TextUtils.isEmpty(mPath)) {
                initCdcModelResult = true;
                break;
            } else {
                Thread.sleep(200);
            }
        }
        if (!initCdcModelResult) {
            mHandler.post(() -> {
                if (mScanCallBack != null) mScanCallBack.onInitScan(false);
            });
        }
        return initCdcModelResult;
    }

    /**
     * 初始化串口
     *
     * @throws IOException
     * @throws DeviceAlreadyOpenException
     */
    private void initSerial() throws IOException, DeviceAlreadyOpenException {
        mSerial = new Serial(mPath, mBr, 0);
        mInputStream = mSerial.getInputStream();
        mOutputStream = mSerial.getOutputStream();
        //初始化成功回调
        mHandler.post(() -> {
            if (mScanCallBack != null) mScanCallBack.onInitScan(true);
        });
    }

    /**
     * 清空串口缓存数据
     *
     * @throws IOException
     */
    private void clearSerialCatchData() throws IOException {
        while (mInputStream.available() > 0) {
            mInputStream.read();
        }
    }

    /**
     * 读取串口数据
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private void readSerialData() throws IOException, InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            int available = mInputStream.available();
            if (available > 0) {
                if (available == mBytesAvailable) {
                    //稳定状态
                    if (++mCount >= getWaitCountWithBr()) {
                        byte[] bytes = new byte[available];
                        mInputStream.read(bytes);
                        postData(bytes);
                        Log.d("Hello", "{scan complete}");
                    }
                } else {
                    //非稳定状态
                    mBytesAvailable = available;
                    mCount = 0;
                }
            } else {
                mBytesAvailable = 0;
                mCount = 0;
            }
                Thread.sleep(2);
        }
    }

    /**
     * 通过串口波特率，设置串口等待稳定时间
     */
    private int getWaitCountWithBr() {
        if (mBr == 115200) {
            return 10;
        } else {
            return 35;
        }
    }

    /**
     * 将数据通过主线程，发送出去
     *
     * @param pBytes
     */
    private void postData(byte[] pBytes) {
        mHandler.post(() -> {
            if (mScanCallBack == null) return;
            String dataStr = new String(pBytes, StandardCharsets.UTF_8);
//            String dataStr = StringUtil.toHexString(pBytes);    //转十六进制
            if (BuildConfig.DEBUG) {
                Log.e("Hello", "SerialScannerThread 原始数据：{" + dataStr + "}");
                Log.e("Hello", "SerialScannerThread 原始数据长度：{" + dataStr.length() + "}");
            }
            //去掉末尾的回车换行符
//            for (int i = 0; i < 2; i++) {
//                if (dataStr.endsWith("\r")) {
//                    Log.e("Hello", "去掉回车");
//                    dataStr = dataStr.substring(0, dataStr.length() - 1);
//                } else if (dataStr.endsWith("\n")) {
//                    Log.e("Hello", "去掉换行");
//                    dataStr = dataStr.substring(0, dataStr.length() - 1);
//                }
//            }
            for (int i = 0; i < 3; i++) {
                if (dataStr.endsWith("\r")) {
                    Log.e("Hello", "去掉回车");
                    dataStr = dataStr.substring(0, dataStr.length() - 1);
                } else if (dataStr.endsWith("\n")) {
                    Log.e("Hello", "去掉换行");
                    dataStr = dataStr.substring(0, dataStr.length() - 1);
                } else if (dataStr.endsWith(" ")) {
                    Log.e("Hello", "去掉换行");
                    dataStr = dataStr.substring(0, dataStr.length() - 1);
                }
            }
            //播放声音
            if (mPlaySound) SoundUtil.get().playSound();
            //回调数据：要是一个回调后释放了，就会有空指针的异常
            if (mScanCallBack != null) mScanCallBack.onScanCallBack(dataStr);
            if (mScanCallBack != null) {
                mScanCallBack.onScanCallBack(dataStr.getBytes(StandardCharsets.UTF_8));
            }
        });
    }
}
