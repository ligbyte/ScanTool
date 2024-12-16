#include <jni.h>
#include <string>
#include <cerrno>
#include <unistd.h>
#include <malloc.h>
#include <fcntl.h>

#include "android/log.h"

#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  __FUNCTION__, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, __FUNCTION__, fmt, ##args)
#define LOGW(fmt, args...) __android_log_print(ANDROID_LOG_WARN, __FUNCTION__, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, __FUNCTION__, fmt, ##args)

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_hello_tps537_1io_1crtl_Tps537IoCrtl_setIoPower(JNIEnv *env, jclass clazz, jboolean b) {
    LOGI("set io power called!");
    int arg = b ? 1 : 0;
    int fd;
    int err;
    fd = open("/dev/telpo_gpio", O_RDWR);
    LOGD("open serial /dev/telpo_gpio fd=%d", fd);
    if (fd > 0) {
        err = ioctl(fd, 0x63, arg);
        LOGD("ioctl 0x61 status = %d", arg);
        LOGD("ioctl result = %d", err);
        close(fd);
        if (err < 0) {
            return false;
        }
        return true;
    }
    return false;
}