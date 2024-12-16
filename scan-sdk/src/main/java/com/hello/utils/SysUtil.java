package com.hello.utils;

import android.annotation.SuppressLint;
import android.os.Build;

import java.lang.reflect.Method;

/**
 * Create by Hello on 2021/11/18 15:53
 * Description 系统工具类
 */
public class SysUtil {

    /**
     * 获取设备的内部型号
     */
    public static String getInnerModel() {
        if (Build.VERSION.SDK_INT > 28) {
            try {
                Method forName = Class.class.getDeclaredMethod("forName", String.class);
                Method getDeclaredMethod = Class.class.getDeclaredMethod("getDeclaredMethod", String.class, Class[].class);
                Class<?> vmRuntimeClass = (Class) forName.invoke((Object) null, "dalvik.system.VMRuntime");
                Method getRuntime = (Method) getDeclaredMethod.invoke(vmRuntimeClass, "getRuntime", null);
                Method setHiddenApiExemptions = (Method) getDeclaredMethod.invoke(vmRuntimeClass, "setHiddenApiExemptions", new Class[]{String[].class});
                Object sVmRuntime = null;
                if (getRuntime != null) {
                    sVmRuntime = getRuntime.invoke(null);
                }
                if (setHiddenApiExemptions != null) {
                    setHiddenApiExemptions.invoke(sVmRuntime, (Object) new String[]{"L"});
                }
            } catch (Throwable var6) {
                var6.printStackTrace();
            }
        }

        String value = "";

        try {
            @SuppressLint("PrivateApi") Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class, String.class);
            value = (String) get.invoke(c, "ro.internal.model", "");
        } catch (Exception var5) {
            var5.printStackTrace();
        }

        return value;
    }
}
