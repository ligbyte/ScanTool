package com.hello.scan;

import android.content.Context;

/**
 * Create by Hello on 2022-11-28 22:03
 * Description: 描述信息
 */
public interface IScanTool {


    /**
     * 初始化串口
     *
     * @param pContext  上下文对象
     * @param pPath     串口地址
     * @param pBaudRate 波特率
     */
    public void initSerial(Context pContext, String pPath, int pBaudRate);

    /**
     * 初始化串口
     *
     * @param pContext      上下文对象
     * @param pPath         串口地址
     * @param pBaudRate     波特率
     * @param pScanCallBack 扫码回调
     */
    public void initSerial(Context pContext, String pPath, int pBaudRate, ScanCallBack pScanCallBack);


    /**
     * 设置回调
     *
     * @param pScanCallBack 扫码回调接口
     */
    public void setScanCallBack(ScanCallBack pScanCallBack);

    /**
     * 继续接收数据
     */
    public void resumeReceiveData() throws Exception;

    /**
     * 暂停接收数据
     */
    public void pauseReceiveData() throws Exception;

    /**
     * 发送指令
     *
     * @param pBytes 需要发送的指令
     */
    public void sendData(byte[] pBytes);

    /**
     * 播放声音
     *
     * @param play true播放声音，false不播放声音
     */
    public void playSound(boolean play);

    /**
     * 释放
     */
    public void release();
}
