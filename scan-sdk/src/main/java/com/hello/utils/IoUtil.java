package com.hello.utils;

import android.util.Log;

import com.telpo.tps550.api.qrcode.QrcodePower;

import java.io.Closeable;
import java.io.IOException;

public class IoUtil {

    public static void close(Closeable pCloseable) {
        if (pCloseable == null) {
            return;
        }
        try {
            pCloseable.close();
        } catch (IOException pE) {
            pE.printStackTrace();
        }
    }

    /**
     * 特定的设备需要上下电的动作
     * 下电调用方法
     */
    public static void turnOff() {
        String internalModel = SysUtil.getInnerModel();
        //if ("TPS537".equals(internalModel)) {
        //    Tps537IoCrtl.setIoPower(false);
        //} else
        if (internalModel.contains("TPS360") || "TPS508".equals(internalModel) || "M1".equals(internalModel)) {
            Log.d("Hello", "{turnOff}turnOff");
            QrcodePower.closeQrcode();
        }
    }

    /**
     * 特定的设备需要上下电的动作
     * 是否有做了上电动作，要是没有上电就返回false
     * @return
     */
    public static boolean turnOn() {
        String internalModel = SysUtil.getInnerModel();
        //if ("TPS537".equals(internalModel)) {
        //    Tps537IoCrtl.setIoPower(true);
        //}  else
        if (internalModel.contains("TPS360") || "TPS508".equals(internalModel) || "M1".equals(internalModel)) {
            Log.d("Hello", "{IoUtil}turnOn");
            QrcodePower.openQrcode();
            return true;
        }
        return false;
    }

}
