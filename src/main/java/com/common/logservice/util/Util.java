


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
        
        private static final String DEFAULT =
                "{\"IP\":\"191.168.31.232\"," +
                "/system/etc/assistant3/connection.json";

        public String ip = "192.168.31.232";
        public String internal_server = "http://192.168.31.232:3000";
        public String external_server = "http://192.168.31.232:3000";
        public String query_path = "/ota/versions";
        public String download_path = "/api/DownloadPackage";
        public String validate_path = "/account/token/commit";
        public String category_path = "/api/exceptions/cat";
        public String config_path = "/autolog/config/json";
        public String post_path = "/log/report";
        public String upload_path = "/log/upload";

        public int port = 3000;
        
        private static Info info = null;

        private Info(String file) {
            build(file);
        }

        private void build(String file) {
            Log.v(TAG, "build file = " + file);
            File f = new File(file);
            if (!f.exists()) {
                Log.v(TAG, "constructor file not exists");
                return;
            }
            
            JsonReader reader = null;
            try {
                reader = new JsonReader(new FileReader(f));
                reader.beginObject();
                while (reader.hasNext()) {
                    String val = reader.nextName();
                    if (val.equals("ip")) {
                        ip = reader.nextString();
                        Log.v(TAG, "build ip = " + ip);
                    } else if (val.equals("internal_server")) {
                        internal_server = reader.nextString();
                        Log.v(TAG, "build internal_server = " + internal_server);
                    } else if (val.equals("external_server")) {
                        external_server = reader.nextString();
                        Log.v(TAG, "build external_server = " + external_server);
                    } else if (val.equals("query_path")) {
                        query_path = reader.nextString();
                        Log.v(TAG, "build query_path = " + query_path);
                    } else if (val.equals("download_path")) {
                        download_path = reader.nextString();
                        Log.v(TAG, "build download_path = " + download_path);
                    } else if (val.equals("validate_path")) {
                        validate_path = reader.nextString();
                        Log.v(TAG, "build validate_path = " + validate_path);
                    } else if (val.equals("category_path")) {
                        category_path = reader.nextString();
                        Log.v(TAG, "build category_path = " + category_path);
                    } else if (val.equals("config_path")) {
                        config_path = reader.nextString();
                        Log.v(TAG, "build config_path = " + config_path);
                    } else if (val.equals("post_path")) {
                        post_path = reader.nextString();
                        Log.v(TAG, "build post_path = " + post_path);
                    } else if (val.equals("upload_path")) {
                        upload_path = reader.nextString();
                        Log.v(TAG, "build upload_path = " + upload_path);
                    } else if (val.equals("port")) {
                        port = reader.nextInt();
                        Log.v(TAG, "build port = " + port);
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endObject();
                reader.close(); 
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "build catch IOException = " + e.getMessage());
            } finally {
                try {
                    if (reader != null) {
                        reader.close(); 
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "build finally IOException = " + e.getMessage());
                }
            }
        }
        
        public static Info getSingleton() {
            Log.v(TAG, "getSingleton info = " + info);
            if (info == null) {
                info = new Info(DEFAULT);
            }
            return info;
        }
    }
    

    public static boolean isEmulator() {
        return (Build.MODEL.toLowerCase().contains("sdk"));
    }

    
    public static boolean isIntranet(Context context) {

        //if (!isWifiConnect(context)) {
        //    Log.e(TAG, "isIntranet no wifi connect");
        //    return false;
        //}

        try {
            Info info = Info.getSingleton();
            Log.v(TAG, "isIntranet ip = [" + info.ip + "] port = [" + info.port + "]");
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

    public static boolean isWifiConnect(Context context) {

        if (TEST)
            return true;
		
		ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        boolean connected = netInfo != null && netInfo.isAvailable() && netInfo.isConnected();

        return connected && (netInfo.getType() == ConnectivityManager.TYPE_WIFI);
    }

    public static boolean isNetworkConnected(Context context) {

        if (TEST)
            return true;

		ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isAvailable() && netInfo.isConnected();
    }
	
	

    public static String formatTime(long timeMillis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timeMillis);
        return String.format(Locale.CHINA, "%04d-%02d-%02d %02d:%02d:%02d",
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                c.get(Calendar.SECOND));
    }

    public static String formatTime() {
        return formatTime(System.currentTimeMillis());
    }

    public static String queryIMEI(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
         return tm.getDeviceId();
    }

    public static String queryVersion() {
        return SystemProperties.get(PROPERTY_LC_VERSION, null);
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
