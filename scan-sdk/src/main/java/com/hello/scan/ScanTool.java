package com.hello.scan;

import android.content.Context;
import android.util.Log;

import com.hello.IScanner;
import com.hello.threads.cdc.CdcScannerThread;
import com.hello.threads.serial.SerialScannerThread;
import com.hello.utils.SoundUtil;

/**
 * Create by Hello on 2022-11-28 22:07
 * Description: 描述信息
 */
public class ScanTool implements IScanTool {

    public static ScanTool GET = new ScanTool();

    private volatile Thread mScannerThread = null;
    private IScanner mIScanner;

    private ScanTool() {
    }

    @Override
    public synchronized void initSerial(Context pContext, String pPath, int pBaudRate) {
        this.initSerial(pContext, pPath, pBaudRate, null);
    }

    @Override
    public synchronized void initSerial(Context pContext, String pPath, int pBaudRate, ScanCallBack pScanCallBack) {
        SoundUtil.get().init(pContext);
        if (mScannerThread != null && mScannerThread.isAlive()) {
            Log.e("Hello", "ScanTool.initSerial == > call");
            return;
        }
        if (pPath.contains("ttyACM")) {
            CdcScannerThread cdcScannerThread = new CdcScannerThread(pContext);
            cdcScannerThread.setCallBack(pScanCallBack);
            cdcScannerThread.startScan();
            mScannerThread = cdcScannerThread;
            mIScanner = cdcScannerThread;
        } else {
            SerialScannerThread serialScannerThread = new SerialScannerThread();
            serialScannerThread.setInitParam(pBaudRate, pPath);
            serialScannerThread.setCallBack(pScanCallBack);
            serialScannerThread.startScan();
            mScannerThread = serialScannerThread;
            mIScanner = serialScannerThread;
        }
    }

    @Override
    public void setScanCallBack(ScanCallBack pScanCallBack) {
        if (mIScanner != null) {
            mIScanner.setCallBack(pScanCallBack);
        }
    }

    @Deprecated()
    @Override
    public void resumeReceiveData() throws Exception {
        throw new Exception("这个接口将会在未来移除掉，请通过设置监听回调API实现暂停和继续接收数据功能");
    }

    @Deprecated()
    @Override
    public void pauseReceiveData() throws Exception {
        throw new Exception("这个接口将会在未来移除掉，请通过设置监听回调API实现暂停和继续接收数据功能");
    }

    @Override
    public void sendData(byte[] pBytes) {
        if (mIScanner != null) {
            mIScanner.sendData(pBytes);
        }
    }

    @Override
    public void playSound(boolean play) {
        if (mIScanner != null) {
            mIScanner.playSound(play);
        }
    }

    @Override
    public void release() {
        if (mIScanner != null) {
            mIScanner.stopScan();
            mIScanner = null;
        }
        SoundUtil.get().release();
    }
}
