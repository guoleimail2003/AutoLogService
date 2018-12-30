package com.common.logservice.server;

import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;

import com.common.logservice.exception.ServerIPPortException;
import com.common.logservice.log.SystemLog;
import com.common.logservice.util.Util;

public class ServerInfo {
    private static final String TAG = "ServerInfo";


    //get the ip from the property named by persist.sys.logservice.ip
    private static final String PROPERTY_SERVER_IP_AND_PORT = "persist.sys.logservice.ip";

    private static final boolean mIsDebug = SystemLog.isDebug();

    private static String mIPAndPort;
    public static String IP = "";
    public static int PORT = 0;
    public static String HTTP_HEAD =  "http://";
    public static String OTA_PATH = "/ota/versions";
    public static String REPORT_PATH = "/log/report";
    public static String UPLOAD_PATH = "/log/upload";

    //Currently reverse for future.
    public static String VALIDATE_PATH = "/account/token/commit";
    public static String CATEGORY_PATH = "/api/exceptions/cat";
    public static String CONFIG_PATH = "/autolog/config/json";

    private static ServerInfo mInfo = null;

    private ServerInfo() {
        mInfo = getSingleton();

        Log.v(TAG, "ServerInfo constructor process "
                + "mIPAndPort = [" + mIPAndPort + "]"
                + " IP = [" + IP + "]"
                + " PORT = [" + PORT + "]");
    }

    public static ServerInfo getSingleton() {
        Log.v(TAG, "getSingleton info = " + mInfo);
        //check is the debug mode
        updateIpAndPort();
        Log.v(TAG, "mIPAndPort = " + mIPAndPort);
        if (mInfo == null) {
            mInfo = new ServerInfo();
        }
        return mInfo;
    }

    public static String getServerIPAndPort() {
        String server = SystemProperties.get(PROPERTY_SERVER_IP_AND_PORT, "").trim();
        mIPAndPort = server;
        try {
            if (server.isEmpty()) {
                throw new ServerIPPortException("Server ip and port is empty");
            }
            IP = mIPAndPort.split(":")[0].trim();
            PORT = Integer.parseInt(mIPAndPort.split(":")[1]);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        } catch (ServerIPPortException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }

        return server;
    }

    public static void updateIpAndPort() {
        Log.d(TAG, "updateIpAndPort");
        mIPAndPort = getServerIPAndPort();
        mInfo = new ServerInfo();
        Log.d(TAG, "Info updateIpAndPort mIPAndPort = [" + mIPAndPort + "]");
    }

    public static String getCheckCategoryUrl(Context context) {
        StringBuilder url = new StringBuilder();
        url.append(HTTP_HEAD);
        url.append(mIPAndPort);
        url.append(CATEGORY_PATH);
        Log.v(TAG, "getCheckCategoryUrl url = [" + url.toString() + "]");

        return url.toString();
    }

    public static String getCheckUpdateUrl(Context context) {
        StringBuilder url = new StringBuilder();
        url.append(HTTP_HEAD);
        url.append(mIPAndPort);
        url.append(OTA_PATH);
        Log.v(TAG, "getCheckCategoryUrl url = [" + url.toString() + "]");

        return url.toString();
    }

    public static String getPostUrl(Context context) {
        StringBuilder url = new StringBuilder();
        url.append(HTTP_HEAD);
        url.append(mIPAndPort);
        url.append(REPORT_PATH);
        Log.v(TAG, "getPostUrl url = [" + url.toString() + "]");

        return url.toString();
    }

    public static String getUploadUrl(Context context) {
        StringBuilder url = new StringBuilder();
        url.append(HTTP_HEAD);
        url.append(mIPAndPort);
        url.append(UPLOAD_PATH);
        Log.v(TAG, "getUploadUrl url = [" + url.toString() + "]");

        return url.toString();
    }

    public static String getServerUrl(Context context) {
        StringBuilder url = new StringBuilder();
        url.append(HTTP_HEAD);
        url.append(mIPAndPort);
        Log.v(TAG, "getCheckCategoryUrl url = [" + url.toString() + "]");

        return url.toString();
    }

    public static String getValidateUrl(Context context) {
        StringBuilder url = new StringBuilder();
        url.append(HTTP_HEAD);
        url.append(mIPAndPort);
        url.append(VALIDATE_PATH);
        Log.v(TAG, "getValidateUrl url = [" + url.toString() + "]");

        return url.toString();
    }
}
