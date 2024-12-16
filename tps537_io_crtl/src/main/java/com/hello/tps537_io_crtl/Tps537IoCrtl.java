package com.hello.tps537_io_crtl;

public class Tps537IoCrtl {

    static {
        System.loadLibrary("tps537_io_crtl");
    }

    public static native boolean setIoPower(boolean b);
}