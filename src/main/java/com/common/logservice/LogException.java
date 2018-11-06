package com.common.logservice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.common.logservice.db.DbFiledName;
import com.common.logservice.db.E_Record;
import com.common.logservice.util.PriorityValues;
import com.common.logservice.util.TypeValues;
import com.common.logservice.util.WebClient;

import org.json.JSONException;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LogException implements DbFiledName {

    private static final String TAG = "LogException";

    private static final String ACTION_REPORT_EXCEPTION = "android.intent.action.EE_USER_EXCEPTION";
    private static final String ACTION_UPLOAD_LOG = "android.intent.action.EE_LOG_UPLOAD";

    private static final String ACTION_EXCEPTION_RESULT = "android.intent.action.ACTION_EXCEPTION_RESULT";

    private static final int THREAD_POOL_NUM = 3;

    protected static final int REPORT_EXCEPTION_RET = 1;
    protected static final int LOGFILE_RET = 2;

    private Context mContext;
    private LogUploader mUploader;

    LogException(Context context, LogUploader uploader) {
        mContext = context;
        mUploader = uploader;
        init();
    }

    public void init() {
        registerTaskHandlers();
    }

    public void uninit() {

    }

    private void registerTaskHandlers() {

        mUploader.registerTaskHandler(TypeValues.T_REPORT_EXCEPTION,
                new LogUploader.PostTaskHandler(mContext, "exception") {

            public void onSuccess(E_Record r) {
                reportUserResult(r, REPORT_EXCEPTION_RET, "");
            }

            public void onFailure(E_Record r) {
                reportUserResult(r, REPORT_EXCEPTION_RET, r.getErr());
            }
        });

        mUploader.registerTaskHandler(TypeValues.T_UPLOAD_LOGFILE,
                new LogUploader.TaskHandler() {

            public boolean handle(LogUploader uploader, E_Record r) {
                int ret = LogUploader.doFileUpload(mContext, uploader, r);
                Log.v(TAG, "handle ret = " + ret);
                if (ret == WebClient.FINISHED) {
                    try {
                        String logpath = r.getObject().getString(FILE_PATH);
                        Log.v(TAG, "handle ACTION_LOGFILE notify delete path = " + logpath);
                        File file = new File(logpath);
                        file.delete();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    return true;
                } else if (ret == 0) {
                    return true;
                }
                reportUserResult(r, LOGFILE_RET, r.getErr());

                return false;
            }
        });
    }

    private void reportUserResult(E_Record r, int result, String err) {
        Log.v(TAG, "sendExceptionResult result = " + result + " err = " + err);
        Intent intent = new Intent(ACTION_EXCEPTION_RESULT);
        Bundle data = new Bundle();
        data.putInt("result", result);
        data.putString("err", err);
        intent.putExtra("data", data);
        mContext.sendBroadcast(intent);
    }

    public void reportUserException(String description, Bundle info) {
        Log.v(TAG, "uploadUserException title = " + description);
        Bundle bundle = new Bundle();

        if (info != null) {
            bundle.putBundle("info", info);
        }
        mUploader.createTask(TypeValues.T_REPORT_EXCEPTION,
                PriorityValues.P_ERR,
                bundle, description);
    }

    public void uploadLogFile(String description, String path, int file_count, Bundle info) {
        Log.v(TAG, "uploadLogFile path = " + path);
        Bundle bundle = new Bundle();
        bundle.putString(DESCRIPTION, description);
        bundle.putString(FILE_PATH, path);
        bundle.putInt(FILE_COUNT, file_count);
        if (info != null) {
            bundle.putBundle("info", info);
        }
        
		mUploader.createTask(TypeValues.T_UPLOAD_LOGFILE, PriorityValues.P_FATAL,
                bundle, description);
    }
}