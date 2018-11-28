


package com.common.logservice.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Calendar;
import java.util.Locale;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.util.JsonReader;
import android.util.Log;

public class Util {
    private static final String TAG = "Util";
    public static boolean TEST = false;

    private static final String PROPERTY_LC_VERSION = "ro.build.display.lc.id";

    //public static final String SERVERS[] = {
    //        "http://172.21.3.240:6543",
    //        "http://acs.leadcoretech.com"};

    private static class Info {
        private static final String TAG = "Util.info";
        private static final String IP_AND_PORT = SystemProperties.get("persist.sys.logservice.ip", "192.168.31.232:3000").trim();

        public String ip = null;
        public int port = 0;
        public String internal_server = "http://" + IP_AND_PORT;
        public String external_server = "http://" + IP_AND_PORT;
        public String query_path = "/ota/versions";
        public String validate_path = "/account/token/commit";
        public String category_path = "/api/exceptions/cat";
        public String config_path = "/autolog/config/json";
        public String post_path = "/log/report";
        public String upload_path = "/log/upload";

        private static Info info = null;

        private Info() {
            try {
                ip = IP_AND_PORT.split(":")[0].trim();
                port = Integer.parseInt(IP_AND_PORT.split(":")[1]);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "There is invalid server ip and port in property persist.sys.logservice.ip");
            }
        }

        public static Info getSingleton() {
            Log.v(TAG, "getSingleton info = " + info);
            Log.v(TAG, "IP_AND_PORT = " + IP_AND_PORT);
            if (info == null) {
                info = new Info();
            }
            return info;
        }
    }
    

    public static boolean isEmulator() {
        return (Build.MODEL.toLowerCase().contains("sdk"));
    }

    
    public static boolean isIntranet(Context context) {

        try {
            Info info = Info.getSingleton();
            Log.v(TAG, "isIntranet ip = [" + info.ip + " port = [" + info.port + "]");
            SocketAddress socketAddress = new InetSocketAddress(info.ip, info.port);
            Socket clientSocket = new Socket();
            clientSocket.connect(socketAddress, 5000);
            clientSocket.close();
            Log.v(TAG, "isIntranet true");
        } catch (IOException e) {
            Log.e(TAG, "isIntranet IOException = " + e.getMessage());
            return false;
        }
        return true;
    }

    public static boolean isNetworkConnected(Context context) {
		ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isAvailable() && netInfo.isConnected();
    }

    public static String queryIMEI(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
         return tm.getDeviceId();
    }

    public static String getValidateUrl(Context context) {
        Info info = Info.getSingleton();
        String url = null;
        if (isIntranet(context)) {
            url = info.internal_server + info.validate_path;
        } else {
            url = info.external_server + info.validate_path;
        }
        Log.v(TAG, "getValidateUrl url = [" + url + "]");

        return url;
    }

    public static String getCheckCategoryUrl(Context context) {
        Info info = Info.getSingleton();
        String url = null;
        if (isIntranet(context)) {
            url = info.internal_server + info.category_path;
        } else {
            url = info.external_server + info.category_path;
        }
        Log.v(TAG, "getCheckCategoryUrl url = [" + url + "]");

        return url;
    }    

    public static String getCheckUpdateUrl(Context context) {
        Info info = Info.getSingleton();
        String url = null;
        if (isIntranet(context)) {
            url = info.internal_server + info.query_path;
        } else {
            url = info.external_server + info.query_path;
        }
        Log.v(TAG, "getCheckUpdateUrl url = [" + url + "]");

        return url;
    }    

    public static String getConfigeUrl(Context context) {
        Info info = Info.getSingleton();
        String url = null;
        if (isIntranet(context)) {
            url = info.internal_server + info.config_path;
        } else {
            url = info.external_server + info.config_path;
        }
        Log.v(TAG, "getConfigeUrl url = [" + url + "]");

        return url;
    }    

    public static String getPostUrl(Context context) {
        Info info = Info.getSingleton();
        String url = null;
        if (isIntranet(context)) {
            url = info.internal_server + info.post_path;
        } else {
            url = info.external_server + info.post_path;
        }
        Log.v(TAG, "getPostUrl url = [" + url + "]");

        return url;
    }    

    public static String getUploadUrl(Context context) {
        Info info = Info.getSingleton();
        String url = null;
        if (isIntranet(context)) {
            url = info.internal_server + info.upload_path;
        } else {
            url = info.external_server + info.upload_path;
        }
        Log.v(TAG, "getUploadUrl url = [" + url + "]");

        return url;
    }    

    public static String getServerUrl(Context context) {
        Info info = Info.getSingleton();
        String server = null;
        if (isIntranet(context)) {
            server = info.internal_server;
        } else {
            server = info.external_server;
        }
        Log.v(TAG, "getServerUrl server = [" + server + "]");

        return server;
    }
}
