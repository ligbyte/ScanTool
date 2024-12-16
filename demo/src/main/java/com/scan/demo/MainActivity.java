package com.scan.demo;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.hello.scan.ScanCallBack;
import com.hello.scan.ScanTool;
import com.scan.demo.databinding.ActivityMainBinding;
import com.telpo.tps550.api.util.StringUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements ScanCallBack {

    private static final Map<String, Pair<String, Integer>> sDevices;

    static {
        /*
         * 支持设备列表
         */
        sDevices = new HashMap<>();
        sDevices.put("TPS508", new Pair<>("/dev/ttyACM0", 115200));
        sDevices.put("TPS360", new Pair<>("/dev/ttyACM0", 115200));
        sDevices.put("P8", new Pair<>("/dev/ttyACM0", 115200));
        sDevices.put("TPS537", new Pair<>("/dev/ttyACM0", 115200));

//        sDevices.put("D2", new Pair<>("/dev/ttyHSL0", 115200)); // D2串口模式
//        sDevices.put("D2M", new Pair<>("/dev/ttyHSL0", 115200)); // D2M串口模式
        sDevices.put("D2M", new Pair<>("/dev/ttyS7", 115200)); // D2M串口模式
        //sDevices.put("D2", new Pair<>("/dev/ttyHSL0", 9600)); // D2串口模式
//        sDevices.put("D2", new Pair<>("/dev/ttyACM0", 115200)); // D2U转串模式

        //sDevices.put("TPS980", new Pair<>("/dev/ttyS0", 115200));
        //sDevices.put("TPS980P", new Pair<>("/dev/ttyS0", 115200));
        //sDevices.put("TPS980P", new Pair<>("/dev/ttyACM0", 115200));
        sDevices.put("TPS980P", new Pair<>("/dev/ttyS0", 115200));

//        sDevices.put("T20", new Pair<>("/dev/ttyS7", 115200));
        sDevices.put("T20", new Pair<>("/dev/ttyHS2", 115200));
        sDevices.put("T20p", new Pair<>("/dev/ttyWK0", 115200));
        sDevices.put("TPS530", new Pair<>("/dev/ttyUSB0", 115200));
        sDevices.put("CW-TB2CA-9230", new Pair<>("/dev/ttyS4", 115200));

        sDevices.put("C31", new Pair<>("/dev/ttyACM0", 115200));
        sDevices.put("TPS732", new Pair<>("/dev/ttyACM0", 115200));

        sDevices.put("C50A", new Pair<>("/dev/ttyACM0", 115200));
        sDevices.put("C50", new Pair<>("/dev/ttyACM0", 115200));

//        sDevices.put("K8",new Pair<>("/dev/ttyACM0",115200));
        sDevices.put("K8",new Pair<>("/dev/ttyS4",115200));

        sDevices.put("TPS580P", new Pair<>("/dev/ttyHSL0", 115200));

        sDevices.put("C1P", new Pair<>("/dev/ttyACM0", 115200));
    }

    private int scanCount = 0;
    private ActivityMainBinding mBinding;
    private HandlerThread mHandlerThread;
    private Handler mWorkHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        initHandler();

        if (!initScanTool()) {
            Toast.makeText(this, "该机型还没有适配", Toast.LENGTH_SHORT).show();
        } else {
            ScanTool.GET.playSound(true);
        }


        mBinding.init.setOnClickListener(pView -> initScanTool());
        mBinding.release.setOnClickListener(pView -> ScanTool.GET.release());
        mBinding.pauseReceiveData.setOnClickListener((pView -> ScanTool.GET.setScanCallBack(null)));
        mBinding.resumeReceiveData.setOnClickListener((pView -> ScanTool.GET.setScanCallBack(MainActivity.this)));

        mBinding.sendData.setOnClickListener(new View.OnClickListener() {
            boolean mBoolean = false;

            @Override
            public void onClick(View v) {
                mBoolean = !mBoolean;
                if (mBoolean) {//开灯
                    //照明灯
                    byte[] bytes = StringUtil.toBytes("7E 01 30 30 30 30 40 49 4C 4C 53 43 4E 31 3B 03 ".replace(" ", ""));
                    ScanTool.GET.sendData(bytes);
                    //瞄准灯
                    byte[] bytes1 = StringUtil.toBytes("7E 01 30 30 30 30 40 41 4D 4C 45 4E 41 31 3B 03 ".replace(" ", ""));
                    ScanTool.GET.sendData(bytes1);
                } else {//关灯
                    //照明灯
                    byte[] bytes = StringUtil.toBytes("7E 01 30 30 30 30 40 49 4C 4C 53 43 4E 30 3B 03 ".replace(" ", ""));
                    ScanTool.GET.sendData(bytes);
                    //瞄准灯
                    byte[] bytes1 = StringUtil.toBytes("7E 01 30 30 30 30 40 41 4D 4C 45 4E 41 30 3B 03".replace(" ", ""));
                    ScanTool.GET.sendData(bytes1);
                }
            }
        });
    }

    /**
     * 把扫描的数据存入本地
     * */
    private void initHandler() {
        mHandlerThread = new HandlerThread("wt");
        mHandlerThread.start();
        mWorkHandler = new Handler(mHandlerThread.getLooper()) {

            boolean writingFile = false;
            File mFile;
            FileOutputStream mFos;

            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                String data = (String) msg.obj;
                if (!writingFile) {
                    String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + "ScanCodeContext.txt";
                    mFile = new File(filePath);
                    if (mFile.exists()) {
                        mFile.delete();
                    }
                    try {
                        mFile.createNewFile();
                        mFos = new FileOutputStream(mFile);
                        mFos.write(data.getBytes(StandardCharsets.UTF_8));
                        writingFile = true;
                    } catch (IOException pE) {
                        pE.printStackTrace();
                    }
                } else {
                    try {
                        mFos.write(data.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException pE) {
                        pE.printStackTrace();
                    }
                }
            }
        };
    }

    /**
     * 判断使用模式
     *
     * @return 返回true表示表示该机型已经适配，false表示该机型还没有适配
     */
    private boolean initScanTool() {

        for (String s : sDevices.keySet()) {
            if (s.equals(Build.MODEL)) {
                Pair<String, Integer> pair = sDevices.get(s);
                if (pair == null) continue;
                Log.e("Hello", "judgeModel == > " + s);
                Log.e("Hello", "path == > " + pair.first);
                Log.e("Hello", "baud rate == > " + pair.second);
                ScanTool.GET.initSerial(this, pair.first, pair.second);
                ScanTool.GET.setScanCallBack(MainActivity.this);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onScanCallBack(String data) {
        if (TextUtils.isEmpty(data)) return;
        Log.e("Hello", "回调数据 == > " + data + "{我是测试}");
        if (scanCount % 2 == 0) {
            mBinding.scanResult.setTextColor(Color.RED);
        } else {
            mBinding.scanResult.setTextColor(Color.BLUE);
        }
        scanCount++;
        mBinding.scanResult.setText(String.format(Locale.CHINA, "扫码结果：\n\n%s", data));
        mBinding.tips.setText(String.format(Locale.CHINA, "扫码次数：%d次", scanCount));

        //将内容写入文件，用作测试验证
        Message message = mWorkHandler.obtainMessage();
        message.obj = "次数：" + String.format(Locale.getDefault(), "%05d", scanCount) + "：" + data + "\n";
        mWorkHandler.sendMessage(message);
    }


//    @Override
//    public void onScanCallBack(byte[] pBytes) {
//        String str = StringUtil.toHexString(pBytes);
//        Log.e("Hello", "回调数据2222222 == > " + str + "{我是测试}");
//        if (scanCount % 2 == 0) {
//            mBinding.scanResult.setTextColor(Color.RED);
//        } else {
//            mBinding.scanResult.setTextColor(Color.BLUE);
//        }
//        scanCount++;
//        mBinding.scanResult.setText(String.format(Locale.CHINA, "扫码结果：\n\n%s", str));
//        mBinding.tips.setText(String.format(Locale.CHINA, "扫码次数：%d次", scanCount));
//    }

    @Override
    public void onInitScan(boolean isSuccess) {
        Log.e("Hello{MainActivity}", "onInitScan == > " + isSuccess);
        String str = isSuccess ? "初始化成功" : "初始化失败";
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
        ScanTool.GET.playSound(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ScanTool.GET.release();
    }
}