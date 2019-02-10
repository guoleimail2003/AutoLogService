package com.common.logservice.log;

import android.os.SystemProperties;
import android.util.Log;

public class SystemLog {
    private static final String TAG = "Log";
    private static final String PROPERTY_AUTOLOGSERVICE_DEBUG = "property.autologservice.debug";
    private static boolean DEBUG_TEST = false;
    private static LogLevel mLogLevel;

    public static enum LogLevel {
        NORMAL(0),
        VERBOSE(1),
        DEBUG(2),
        INFO(3),
        WARNING(4),
        ERROR(5),
        FATAL(6);

        private int val = 0;

        LogLevel(Integer l) {
            this.val = l;
        }

        public int getLevelValue() {
            return this.val;
        }

        public static LogLevel getLogLevelType(int l) {
            LogLevel ret = NORMAL;
            for (LogLevel level : LogLevel.values()) {
                if (level.val == l) {
                    ret = level;
                }
            }
            return ret;
        }

        public boolean greaterthan (LogLevel b) {
            boolean ret = false;
            if (this.val > b.val) {
                ret = true;
            }
            return ret;
        }

        public boolean equals (LogLevel b) {
            boolean ret = false;
            if (this.val == b.val) {
                ret = true;
            }
            return ret;
        }

        public boolean lessthan (LogLevel b) {
            boolean ret = false;
            if (this.val < b.val) {
                ret = true;
            }
            return ret;
        }
    }

    public static boolean isDebug() {
        getDebugFlag();
        return DEBUG_TEST;
    }


    public static boolean getDebugFlag() {
        int lev = SystemProperties.getInt(PROPERTY_AUTOLOGSERVICE_DEBUG,  0);
        mLogLevel = LogLevel.getLogLevelType(lev);
        switch (mLogLevel) {
            case NORMAL:
                break;
            case VERBOSE:
                break;
            case DEBUG:
                break;
            case INFO:
                break;
            case WARNING:
                break;
            case ERROR:
                break;
            case FATAL:
                break;
            default:
                break;
        }
        DEBUG_TEST = lev > LogLevel.NORMAL.getLevelValue();
        Log.d(TAG, "getDebugFlag() = " + DEBUG_TEST);
        return DEBUG_TEST;
    }

    public static void LOGV(String caller, String msg) {
        if (mLogLevel.greaterthan(LogLevel.NORMAL)) {
            Log.v(caller, msg);
        }
    }

    public void LOGD(String caller, String msg) {
        if (mLogLevel.greaterthan(LogLevel.VERBOSE)) {
            Log.d(caller, msg);
        }
    }

    public static void LOGI(String caller, String msg) {
        if (mLogLevel.greaterthan(LogLevel.DEBUG)) {
            Log.i(caller, msg);
        }
    }

    public static void LOGW(String caller, String msg) {
        if (mLogLevel.greaterthan(LogLevel.INFO)) {
            Log.e(caller, msg);
        }
    }

    public static void LOGE(String caller, String msg) {
        if (mLogLevel.greaterthan(LogLevel.WARNING)) {
            Log.e(caller, msg);
        }
    }
}