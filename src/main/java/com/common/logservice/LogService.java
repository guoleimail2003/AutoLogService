
package com.common.logservice;

import java.io.File;
import java.util.ArrayList;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

public class LogService extends Service {

    private static final String TAG = "LogService";

    //ACTION FIELD
    public static final String ACTION_REPORT_USER_EXCEPTION = "android.intent.action.ACTION_REPORT_EXCEPTION";
    public static final String ACTION_UPLOAD_LOG_FILE = "android.intent.action.ACTION_UPLOAD_LOG_FILE";
    public static final String ACTION_VALIDATE = "android.intent.action.ACTION_VALIDATE";
    public static final String ACTION_DOWNLOAD = "android.intent.action.ACTION_DOWNLOAD";
    public static final String ACTION_CHECK_UPDATE = "android.intent.action.ACTION_CHECK_UPDATE";
    public static final String ACTION_CHECK_CATEGORY = "android.intent.action.ACTION_CHECK_CATEGORY";

    //Action result
    public static final String ACTION_REPORT_USER_EXCEPTION_RESULT = "android.intent.action.ACTION_REPORT_USER_EXCEPTION_RESULT";
    public static final String ACTION_UPLOAD_LOG_FILE_RESULT = "android.intent.action.ACTION_UPLOAD_LOG_FILE_RESULT";
    public static final String ACTION_VALIDATE_RESULT = "android.intent.action.ACTION_VALIDATE_RESULT";
    public static final String ACTION_DOWNLOAD_RESULT = "android.intent.action.ACTION_DOWNLOAD_RESULT";
    public static final String ACTION_CHECK_UPDATE_RESULT = "android.intent.action.ACTION_CHECK_UPDATE_RESULT";
    public static final String ACTION_CHECK_CATEGORY_RESULT = "android.intent.action.ACTION_CHECK_CATEGORY_RESULT";
    public static final String ACTION_PING_TIMEOUT = "android.intent.action.ACTION_PING_TIMEOUT";

    //Report Exception Field Name
    //PRIORITY
    public static final String REPORT_EXCEPTION_PRIORITY = "R_E_PRIORITY";
    //description
    public static final String REPORT_EXCEPTION_DESCRIPTION = "R_E_DESCRIPTION";

    //Log File upload
    //description
    public static final String LOG_UPLOAD_DESCRIPTION = "L_U_DESCRIPTION";
    //Priority
    public static final String LOG_UPLOAD_PRIORITY = "L_U_PRIORITY";
    //File path
    public static final String LOG_UPLOAD_FILE_PATH = "L_U_FILE_PATH";
    //File count
    public static final String LOG_UPLOAD_FILE_COUNT = "L_U_FILE_COUNT";

    //Check update
    public static final String CHECK_UPDATE_DESCRIPTION = "C_U_DESCRIPTION";

    //Download software
    public static final String DOWNLOAD_URL = "D_URL";
    public static final String DOWNLOAD_SAVETO_PATH = "D_SAVETO_PATH";

    private static final long INTERVAL = (73 * 60 * 1000); // 1 hour

	private LogUploader mUploader;
    private LogException mExceptionHandler;
    private PendingIntent mPingIntent;

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
            } else if (ACTION_PING_TIMEOUT.equals(action)) {
                Log.v(TAG, "mBroadcastReceiver.onReceive PING timeout");
                mUploader.notifyPingTask();
            } else if (ACTION_REPORT_USER_EXCEPTION.equals(action)) {
                Log.v(TAG, "mBroadcastReceiver.onReceive action = " + action);
                String desc = intent.getStringExtra("title");
                Bundle bundle = new Bundle();

                mExceptionHandler.reportUserException(desc, bundle);
            } else if (ACTION_UPLOAD_LOG_FILE.equals(action)) {
                Log.v(TAG, "mBroadcastReceiver.onReceive action = " + action);
                String desc = intent.getStringExtra("EE_TITLE");
                String file_path = intent.getStringExtra("EE_FILE_PATH");
                Bundle bundle = new Bundle();
                mExceptionHandler.uploadLogFile(desc, file_path, 1, bundle);
            } else if (ACTION_CHECK_UPDATE.equals(action)) {
                Log.v(TAG, "mBroadcastReceiver.onReceive action = " + action);
                Bundle bundle = new Bundle();
                mUploader.checkUpdate(bundle);
            } else if (ACTION_VALIDATE.equals(action)) {
                Log.v(TAG, "mBroadcastReceiver.onReceive action = " + action);
                Bundle bundle = new Bundle();
                //VALIDATE
            } else if (ACTION_DOWNLOAD.equals(action)) {
                Log.v(TAG, "mBroadcastReceiver.onReceive action = " + action);
                Bundle bundle = new Bundle();
                String url = intent.getStringExtra("URL");
                String path = intent.getStringExtra("file_path");
                mUploader.download(url, path, bundle);
            } else if (ACTION_CHECK_CATEGORY.equals(action)) {
                Log.v(TAG, "mBroadcastReceiver.onReceive action = " + action);
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
    	public void download(String description, String url, String path, Bundle info) {
            Log.v(TAG, "download url = [" + url + "]");
            String result = null;
            if (mUploader != null) {
                result = mUploader.download(url, path, info);
            }
            Log.v(TAG, "download result = [" + result + "]");
            Intent intent = new Intent(ACTION_DOWNLOAD_RESULT);
            intent.putExtra("result", result);
            intent.putExtra("description", description);
            sendBroadcast(intent);
        }

        @Override
    	public void checkUpdate(String description, Bundle info) {
            Log.v(TAG, "checkUpdate");
            ArrayList<Bundle> pkgs = null;
            if (mUploader != null) {
                pkgs = mUploader.checkUpdate(info);
            }

            String error = "";
            if (pkgs != null) {
                Log.v(TAG, "connect to server success, and now download the firmware");

                //get the download url
                Bundle first = pkgs.get(0);
                String url = first.getString("firmware").trim();

                Log.d(TAG, "file_path = " + Environment.getExternalStorageDirectory());
                String path = Environment.getExternalStorageDirectory().getPath();
                path = path + "/" + "update.zip";
                download("abcdefg", url, path ,new Bundle());
            } else {
                Log.v(TAG, "Failed to connect to server");
                error = "Failed to connect to server";
            }

            Intent intent = new Intent(ACTION_CHECK_UPDATE_RESULT);
            intent.putParcelableArrayListExtra("pkgs", pkgs);
            if (error.isEmpty()) {
                intent.putExtra("description", description);
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

    LogService() {
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

        mUploader = new LogUploader(this);
		mExceptionHandler = new LogException(this, mUploader);

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(ACTION_PING_TIMEOUT);
        filter.addAction(ACTION_REPORT_USER_EXCEPTION);
        filter.addAction(ACTION_UPLOAD_LOG_FILE);
        filter.addAction(ACTION_DOWNLOAD);
        filter.addAction(ACTION_VALIDATE);
        filter.addAction(ACTION_CHECK_CATEGORY);
        filter.addAction(ACTION_CHECK_UPDATE);
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

