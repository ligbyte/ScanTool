package com.hello.scan;

public interface ScanCallBack {
    /**
     * 扫码回调
     *
     * @param data 条码数据
     */
    void onScanCallBack(String data);


    /**
     * 扫码回调
     *
     * @param pBytes 条码的byte数组数据
     */
    default void onScanCallBack(byte[] pBytes) {
    }

    /**
     * 串口初始化结果回调，该回调默认实现，开发者
     * 可以根据需求自己实现
     *
     * @param isSuccess 初始化结果。
     *                  true表示初始化成功，
     *                  false表示初始化失败
     */
    default void onInitScan(boolean isSuccess) {
    }
}
