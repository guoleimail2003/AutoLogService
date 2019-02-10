
package com.common.logservice;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;

import com.common.logservice.log.SystemLog;
import com.common.logservice.server.ServerInfo;
import com.common.logservice.util.Util;

public class LogService extends Service {

    private static final String TAG = "LogService";
    //Action result
    public static final String ACTION_REPORT_USER_EXCEPTION_RESULT = "android.intent.action.ACTION_REPORT_USER_EXCEPTION_RESULT";
    public static final String ACTION_UPLOAD_LOG_FILE_RESULT = "android.intent.action.ACTION_UPLOAD_LOG_FILE_RESULT";
    public static final String ACTION_VALIDATE_RESULT = "android.intent.action.ACTION_VALIDATE_RESULT";
    public static final String ACTION_DOWNLOAD_RESULT = "android.intent.action.ACTION_DOWNLOAD_RESULT";
    public static final String ACTION_CHECK_UPDATE_RESULT = "android.intent.action.ACTION_CHECK_UPDATE_RESULT";
    public static final String ACTION_CHECK_CATEGORY_RESULT = "android.intent.action.ACTION_CHECK_CATEGORY_RESULT";
    public static final String ACTION_PING_TIMEOUT = "android.intent.action.ACTION_PING_TIMEOUT";

    private static final long INTERVAL = (73 * 60 * 1000); // 1 hour

	private LogUploader mUploader;
    private LogException mExceptionHandler;
    private PendingIntent mPingIntent;
    private String mVer;
    private Date mVerDate;
    private SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyyMMdd");

	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v(TAG, "mBroadcastReceiver.onReceive action = " + action);
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo info = connectivityManager.getActiveNetworkInfo();
                if(info != null && info.isConnected()) {
                    Log.v(TAG, "mBroadcastReceiver.onReceive Network connected");
                    mUploader.notifyTask();
                }
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo info = connectivityManager.getActiveNetworkInfo();
                if(info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI) {
                    Log.v(TAG, "mBroadcastReceiver.onReceive Wifi connected");
                    mUploader.notifyTask();
                }
            } else {
                Log.v(TAG, "mBroadcastReceiver.onReceive action = " + action + " can not handle it!");
            }
        }
    };

    private IBinder mLogBinder = new ILogService.Stub() {
        @Override
        public void uploadUserException(String description, Bundle info) {
            Log.v(TAG, "uploadUserException description = " + description);
            if (mExceptionHandler != null) {
                mExceptionHandler.reportUserException(description, info);
            }
        }

        @Override
        public void updateServerIPPort(Bundle info) throws RemoteException {
            Log.v(TAG, "updateServerIPPort");
            ServerInfo.updateIpAndPort();
        }

        @Override
        public void uploadLogFile(String description, String file_path, int file_count, Bundle info) {
            Log.v(TAG, "uploadLogFile path = [" + file_path + "]" + ", file_count = [" + file_count + "]");
            if (mExceptionHandler != null) {
                mExceptionHandler.uploadLogFile(description, file_path, file_count, info);
            }
        }

        @Override
    	public void validate(String description, String code, Bundle info) {
            Log.v(TAG, "validate code = [" + code + "]");
            String result = null;
            if (mUploader != null) {
                result = mUploader.validate(code, info);
            }
            Log.v(TAG, "validate result = [" + result + "]");
            Intent intent = new Intent(ACTION_VALIDATE_RESULT);
            intent.putExtra("result", result);
            intent.putExtra("description", description);
            sendBroadcast(intent); 
        }

        @Override
    	public void download(String url, String path, Bundle info) {
                Log.v(TAG, "download url = [" + url + "] + path = [" + path + "]");
                String result = null;
                if (mUploader != null) {
                    result = mUploader.download(url, path, info);
                }
                Log.v(TAG, "download result = [" + result + "] path = [" + path  + "]");
                Intent intent = new Intent(ACTION_DOWNLOAD_RESULT);
                intent.putExtra("result", result);
                intent.putExtra("path", path);
                sendBroadcast(intent);
        }

        @Override
    	public void checkUpdate(Bundle info) {
            Log.v(TAG, "checkUpdate");
            ArrayList<Bundle> pkgs = null;
            if (mUploader != null) {
                pkgs = mUploader.checkUpdate(info);
            }

            String error = "";
            if (pkgs == null || (pkgs != null && pkgs.size() == 0) ) {
                Log.v(TAG, "connect to server success, and there are no update file on the server");
            } else if (pkgs != null && pkgs.size() > 0) {
                Log.v(TAG, "connect to server success, and now check whether to download the firmware");

                //get the download url
                Bundle first = pkgs.get(0);
                String url = first.getString("firmware").trim();
                String upgrade_ver = first.getString("version").trim();
                Date upgrade_ver_date = null;
                try {
                    int index = upgrade_ver.lastIndexOf("_");
                    String test = upgrade_ver.substring(index + 1, upgrade_ver.length());
                    upgrade_ver_date = mSimpleDateFormat.parse(
                            upgrade_ver.substring(index + 1, upgrade_ver.length())
                        );
                } catch (ParseException e) {
                    e.printStackTrace();
                    return;
                }

                Log.d(TAG, "file_path = " + Environment.getExternalStorageDirectory()
                        + " upgrade_ver = " + upgrade_ver
                        + " upgrade_ver_date = " + upgrade_ver_date
                        + " mVer = " + mVer
                        + " mVerDate = " + mVerDate);
                if (mVerDate != null && upgrade_ver_date != null) {
                    if (upgrade_ver_date.getTime() > mVerDate.getTime()) {
                        String path = Environment.getExternalStorageDirectory().getPath() + "/" + upgrade_ver + ".zip";
                        File save_toFile = new File(path);
                        if (save_toFile.exists()) {
                            save_toFile.delete();
                        }
                        download(url, path, new Bundle());
                    }
                }
            } else {
                Log.v(TAG, "Failed to connect to server");
                error = "Failed to connect to server";
            }

            Intent intent = new Intent(ACTION_CHECK_UPDATE_RESULT);
            intent.putParcelableArrayListExtra("pkgs", pkgs);
            if (error.isEmpty()) {
                intent.putExtra("description", "CheckUpdate Success");
            } else {
                intent.putExtra("description", error);
            }
            sendBroadcast(intent);
        }

        @Override
    	public void checkCategory(String description, Bundle info) {
            Log.v(TAG, "checkCategory");
            String category = null;

            if (mUploader != null) {
                category = mUploader.checkCategory(info);
            }

            Log.v(TAG, "checkCategory category = [" + category + "]");
            Intent intent = new Intent(ACTION_CHECK_CATEGORY_RESULT);
            intent.putExtra("category", category);
            intent.putExtra("description", description);
            sendBroadcast(intent);
        }
    };

    public LogService() {
        Log.v(TAG, "constructor");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind");
        return mLogBinder;
    }

    @Override
    public boolean onUnbind(Intent paramIntent) {
        Log.v(TAG, "onUnbind");
        return super.onUnbind(paramIntent);
    }

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate");
        super.onCreate();

        if (SystemLog.isDebug()) {
            //test code set the ip and
            SystemProperties.set("ro.build.display.id", "ST720_R2F16_DS_A4RV037P3_20181117");
            mVer = SystemProperties.get("ro.build.display.idd", "ST720_R2F16_DS_A4RV037P3_20181117");
        } else {
            //code get the system property
            mVer = SystemProperties.get("ro.build.display.id", "ST720_R2F16_DS_A4RV037P3_20181117");
        }
        int date_index = mVer.lastIndexOf("_");
        try {
            mVerDate = mSimpleDateFormat.parse(mVer.substring(date_index + 1));
        } catch (ParseException e) {
            e.printStackTrace();
            Log.e(TAG, "mVerDate = [" + mVerDate + "] mVer = [" + mVer + "]" );
            mVerDate = null;
            mVer = null;
        }
        Log.v(TAG, "LogService mVer = " + mVer
                + " mVerDate = " + mVerDate);

        mUploader = new LogUploader(this);
		mExceptionHandler = new LogException(this, mUploader);

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(ACTION_PING_TIMEOUT);
        registerReceiver(mBroadcastReceiver, filter);

        Intent intent = new Intent(ACTION_PING_TIMEOUT);
        mPingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        long currentTimeMillis = System.currentTimeMillis();
        Log.v(TAG, "onCreate currentTimeMillis = " + currentTimeMillis + " INTERVAL = " + INTERVAL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        super.onDestroy();

        if (mUploader != null) {
            mUploader.uninit();
        }

        if (mExceptionHandler != null) {
            mExceptionHandler.uninit();
        }

        unregisterReceiver(mBroadcastReceiver);
    }
}

