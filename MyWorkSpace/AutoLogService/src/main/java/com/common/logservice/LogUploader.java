package com.common.logservice;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.common.logservice.db.DbFiledName;
import com.common.logservice.db.LogDataHelper;
import com.common.logservice.db.E_Record;
import com.common.logservice.ota.FirmwareDownload;
import com.common.logservice.server.ServerInfo;
import com.common.logservice.util.PriorityValues;
import com.common.logservice.util.TypeValues;
import com.common.logservice.util.Util;
import com.common.logservice.util.WebClient;;

public class LogUploader implements DbFiledName {

    private static final String TAG = "LogUploader";

    private Context mContext;
    private LogDataHelper dataHelper = null;
    private static String mImei = "";
    private static String mSn = "";
    private static String mHwVersion = "";
    private static String mSwVersion = "";
    private static String mBbVersion = "";
    private static String mMd5 = "";

    public static interface TaskHandler {
        boolean handle(LogUploader logUploader, E_Record r);
    }

    public static abstract class PostTaskHandler implements TaskHandler {

        private final String mBranch;
        private Context mContext;

        public PostTaskHandler(Context context, String branch) {
            mContext = context;
            mBranch = branch;
        }

        @Override
        public boolean handle(LogUploader uploader, E_Record r) {
        	Log.v(TAG, "handle");

        	compileObj(r.getObject());
            boolean result = doWebPost(mContext, uploader, r, mBranch);
        	Log.v(TAG, "handle doWebPost result = [" + result + "]");
            if (result) {
                onSuccess(r);
            } else {
                onFailure(r);
            }

            return result;
        }

        public abstract void onSuccess(E_Record r);
        public abstract void onFailure(E_Record r);
    }

    Map<Integer, TaskHandler> handlers= new HashMap<>();

    protected LogUploader(Context context) {
        mContext = context;
        dataHelper = new LogDataHelper(context);
        init(context);
    }

    public void updateIMEI(String imei) {
        mImei = imei;
        Log.v(TAG, "updateIMEI mImei = " + mImei);
    }

    public void updateSN(String sn) {
        mSn = sn;
        Log.v(TAG, "updateSN mSn = " + mSn);
    }

    public void updateHwVersion(String version) {
        mHwVersion = version;
        Log.v(TAG, "updateHwVersion mHwVersion = " + mHwVersion);
    }

    public void updateSwVersion(String version) {
        mSwVersion = version;
        Log.v(TAG, "updateSwVersion mSwVersion = " + mSwVersion);
    }

    public void updateBbVersion(String version) {
        mBbVersion = version;
        Log.v(TAG, "updateBbVersion mBbVersion = " + mBbVersion);
    }

	public String getSwVersion() {
		return mSwVersion;
	}
	
    private JSONObject convertJsonObject(Bundle bundle) {
        Log.v(TAG, "convertJsonObject");
        JSONObject jobj = new JSONObject();
        try {
            for (String key : bundle.keySet()) {
                Object val = bundle.get(key);
                jobj.put(key, val);
            }
            Log.v(TAG, "convertJsonObject jobj = [" + jobj.toString() + "]");
            return jobj;
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "convertJsonObject JSONException = " + e.getMessage());
        }
        return null;
    }

    public void updateTask(E_Record r) {
        dataHelper.updateTaskObject(r);
    }

    public void delayTask(E_Record r, int delay) {
        Log.v(TAG, "delayTask delay = " + delay);
        dataHelper.changeTask(r.getId(), delay, -1, r.getErrcount(), r.getErr());
    }

    public void removeTask(E_Record r) {
        dataHelper.removeTask(r.getId());
    }

    public boolean postTask(TypeValues type, PriorityValues priority, JSONObject obj, String title) {
        String file_path = null;
        int file_count = 0;
        try {
            if (obj.has(FILE_PATH)) {
                file_path = obj.getString(FILE_PATH);
            }
            if (obj.has(FILE_COUNT)) {
                file_count = obj.getInt(FILE_COUNT);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.v(TAG, "postTask type = " + type
                + "  priory = " + priority
                + " file_path = " + file_path
                + " file_count = " + file_count
                + " title =" + title);
        long id = dataHelper.addTask(type, priority, obj, title, file_path, file_count);
        Log.v(TAG, "postTask id = " + id);
        return id > 0;
    }

    public void createTask(TypeValues type, PriorityValues priority, Bundle bundle, String title) {
        Log.v(TAG, "createTask Bundle");
        createTaskInner(type, priority, convertJsonObject(bundle), title);
    }

    private void createTaskInner(TypeValues type, PriorityValues priority, JSONObject obj, String title) {
        Log.v(TAG, "createTaskInner");
        if (obj == null) {
            Log.e(TAG, "addTask null task object");
            return;
        }

        constructObject(obj);
        if (postTask(type, priority, obj, title)) {
            notifyTask();
        }
    }

    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public void notifyTask() {
        Log.v(TAG, "notifyTask");
        executor.execute(taskRoutineRunner);
    }

    public void notifyPingTask() {
        Log.v(TAG, "notifyPingTask");
        executor.execute(pingTaskRunner);
    }

    private Runnable pingTaskRunner = new Runnable() {
        @Override
        public void run() {
            Log.v(TAG, "pingTaskRunner run");
            taskRoutine(true);
        }
    };

    private Runnable taskRoutineRunner = new Runnable() {
        @Override
        public void run() {
            Log.v(TAG, "taskRoutineRunner run");
            taskRoutine(false);
        }
    };
    
    private void taskRoutine(boolean isHandlePing) {
        Log.v(TAG, "taskRoutine isHandlePing = [" +isHandlePing + "]");
		int executeCount = 0;
        Log.v(TAG, "taskRoutine loop begin");
        while (true) {
            E_Record r = dataHelper.queryFirstTask();
            if (r == null) {
                break;
            } else {
                Log.v(TAG, "taskRoutine find record id = [" + r.getId() + "]"
                        + " title =[" + r.getDescription() + "]");
            }

			executeCount++;
            if (!Util.isNetworkConnected(mContext)) {
                Log.e(TAG, "taskRoutine no network");
                break;
            }

            TaskHandler handle = handlers.get(r.getType().getValue());
            if (handle == null || r.getId() < 0) {
                Log.e(TAG, "taskRoutine remove invalid record id = [" + r.getId() + "]");
                dataHelper.removeTask(r.getId());
                continue;
            }

            if (!handle.handle(this, r)) {
                Log.e(TAG, "taskRoutine handle record id = [" + r.getId() + "] failed");
                r.setErr("Network problem issue");
                r.setErrcount(r.getErrcount() + 1);
                updateTask(r);
                break;
            } else {
                Log.v(TAG, "taskRoutine handle record id = [" + r.getId() + "] successfully");
                dataHelper.removeTask(r.getId());
            }
        }
        Log.v(TAG, "taskRoutine loop end executeCount = [" +executeCount + "]");
		
		if (executeCount == 0 && isHandlePing) {
			Log.v(TAG, "taskRoutine ping");
			handlePingTask();
		}
    }

    public void registerTaskHandler(TypeValues type, TaskHandler handler) {
        handlers.put(type.getValue(), handler);
    }

    private static boolean doWebPost(Context context, LogUploader uploader, E_Record r, String branch) {
        Log.v(TAG, "doWebPost branch = " + branch);
        Map<String, String> params = new HashMap<>();
        params.put("imei", r.getImei_1());
        params.put("exception", r.getDescription());  // exception description
        //params.put("object", r.getObject().toString());
        try {
            //String response = WebClient.post(context, POST_URL, params);
            String response = WebClient.post(context, params);
            JSONObject jobj = new JSONObject(response);
            //if (!jobj.has("result") || !jobj.getBoolean("result")) {
            //    throw new IOException("web response:" + jobj.getString("error"));
            //}
            uploader.removeTask(r);
            return true;
        } catch (IOException|JSONException e) {
            Log.v(TAG, "doWebPost Exception = [" + e.getMessage() + "]");
            onFail(uploader, r, e.getMessage(), WebClient.RECOVERABLE_ERROR);
            return false;
        }
    }

    public static int doFileUpload(Context context, LogUploader uploader, E_Record r) {
        Log.v(TAG, "doFileUpload");
        int ret;
        try {
            String key = "";
            int pos = 0;
            String desc = "", logpath = "";
            int file_count = 0;
            if (r.getObject().has(DESCRIPTION)) {
                desc = r.getObject().getString(DESCRIPTION); //req:upload dump log
            }
            if (r.getObject().has(FILE_PATH)) {
                logpath = r.getObject().getString(FILE_PATH);
            }
            if (r.getObject().has(FILE_COUNT)) {
                file_count = r.getObject().getInt(FILE_COUNT);
            }

            //if file_count > 1 , multi file uploaded
            Log.v(TAG, "doFileUpload check file_count = " + file_count);
            UploadFileTask post;
            if (file_count > 1) {
                //multi file
                String[] f_uploads = new String[file_count];
                //String f_extend_name = logpath.substring(logpath.lastIndexOf('.') + 1);
                f_uploads[0] = logpath;
                for (int i=1; i<file_count; i++) {
                    f_uploads[i] = logpath + "_" + i;
                }
                post = new UploadFileTask(f_uploads);
            } else {
                //signle file
                post = new UploadFileTask(logpath, pos);
            }

            ret = post.uploadPartialEx(context, desc); //req:upload dump log
            Log.v(TAG, "doFileUpload ret = [" + ret + "]");
            if (ret == WebClient.FINISHED) {
                uploader.removeTask(r);
            } else if (ret == 0) {
                r.getObject().put("pos", post.startOffset);
                uploader.updateTask(r);
            } else {
                onFail(uploader, r, post.errString, ret);
            }
        } catch (JSONException e) {
            ret = WebClient.UNRECOVERABLE_ERROR;
            onFail(uploader, r, "doFileUpload bad ACTION_LOGFILE object", ret);
            Log.e(TAG, "doFileUpload JSONException = " + e.getMessage());
        }

        return ret;
    }

    public static void onFail(LogUploader dealer, E_Record r, String err, int ret) {
        Log.v(TAG, "onFail err = [" + err + "] ret = [" + ret + "]");

        r.setErrcount(r.getErrcount() + 1);
        r.setErr(err);
        Log.v(TAG, "onFail errcount = [" + r.getErrcount() + "] obj = [" + r.getObject() + "]");
        if (r.getErrcount() > 20 || ret == WebClient.UNRECOVERABLE_ERROR) {
            Log.e(TAG, "onFail removeTask");
            dealer.removeTask(r);
            return;
        }
    }

    public static class UploadFileTask extends WebClient.UploadFile {
		
        public UploadFileTask(String path, int offset) {
			
            super(path, offset);
        }

        public UploadFileTask(String[] files) {
            super(files);
        }

        public int uploadPartialEx(Context context, String desc) {
            Log.v(TAG, "uploadPartialEx");
			Map<String, String> postparams = new HashMap<>();
			postparams.put("imei", mImei);
            int ret = uploadPartial(context, null, postparams); //req:upload dump log
            deleteUploadedFile();
            return ret;
        }
    }

    private boolean constructObject(JSONObject obj) {
        Log.v(TAG, "constructObject");
        try {
            //obj.put("key", genId());
            //obj.put("imei", mImei);
            obj.put("mSn", mSn);
            obj.put("mHwVersion", mHwVersion);
            obj.put("mSwVersion", mSwVersion);
            obj.put("mBbVersion", mBbVersion);
            //obj.put("time", Util.formatTime());
            Log.v(TAG, "constructObject obj = [" + obj.toString() + "]");
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "constructObject JSONException = " + e.getMessage());
            return false;
        }

        return true;
    }

    private void init(Context context) {
        Log.v(TAG, "init");
        checkDeviceInfo(mContext);
    }

    private void checkDeviceInfo(Context context) {
        Log.v(TAG, "checkIrrevocable");
        TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        String imei = "";
        try {
            imei = tm.getDeviceId();
            Log.v(TAG, "checkDeviceInfo imei = [" + imei + "]");
        } catch (SecurityException se) {
            Log.e(TAG, "There is no permission when calling getDeviceId()");
            se.printStackTrace();
        } finally {
            mImei = imei;
        }
        String hwv = Build.HARDWARE;
        Log.v(TAG, "checkIrrevocable hwv = [" + hwv + "]");
        mHwVersion = hwv;
        
        String swv = Build.VERSION.RELEASE;
        Log.v(TAG, "checkIrrevocable swv = [" + swv + "]");
        mSwVersion = swv;

        String bbv = Build.getRadioVersion();
        Log.v(TAG, "checkIrrevocable bbv = [" + bbv + "]");
        mBbVersion = bbv;
    }

    private static boolean compileObj(JSONObject obj) {
        Log.v(TAG, "compileObj");
        if (obj != null) {
            try {
                obj.put("imei", mImei);
                obj.put("hw_version", mHwVersion);
                obj.put("sw_version", mSwVersion);
                obj.put("bb_version", mBbVersion);
                Log.v(TAG, "compileObj object = [" + obj.toString() + "]");
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "compileObj JSONException = " + e.getMessage());
            }
        }
        return false;
    }
    
    private boolean handlePingTask() {
        Log.v(TAG, "handlePingTask");

        E_Record r = new E_Record();
        r.setId(System.currentTimeMillis());
        r.setPriority(PriorityValues.P_INFO);
        r.setType(TypeValues.T_PING);

        if (!compileObj(r.getObject())) {
            return false;
        }
        boolean result = doWebPost(mContext, this, r, "ping");
        Log.v(TAG, "handlePingTask doWebPost result = [" + result + "]");
        return result;
    }

    public String validate(String code, Bundle info) {
        Log.v(TAG, "validate code = [" + code);
        String ret = "OK";

        String url = ServerInfo.getValidateUrl(mContext);
        Log.v(TAG, "validate url = [" + url + "]");
        try {
            Map<String, String> params = new HashMap<>();
            params.put("name", code);
            params.put("imei", mImei);
            params.put("hw_version", mHwVersion);
            params.put("sw_version", mSwVersion);
            params.put("bb_version", mBbVersion);

            String response = WebClient.postRequest(new URL(url), params);
            Log.v(TAG, "validate response = [" + response + "]");
            JSONObject jobj = new JSONObject(response);
            boolean result = jobj.getBoolean("result");
            Log.v(TAG, "validate result = [" + result + "]");
            if (!jobj.has("result") || !result) {
                Log.e(TAG, "validate response invalid");
                ret = "ERROR";
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "validate IOException = " + e.getMessage());
            ret = "FAILED";
        } catch (RuntimeException e) {
            e.printStackTrace();
            Log.e(TAG, "validate RuntimeException = " + e.getMessage());
            ret = "FAILED";
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "validate JSONException = " + e.getMessage());
            ret = "FAILED";
        }
        
        return ret;
    }
    
    public String download(String url, String path, Bundle info) {
        Log.v(TAG, "download url = [" + url + "] path = [" + path + "]");
        FirmwareDownload download = new FirmwareDownload(url, path);
        //String ret = download.downloadFile(url, path);
        //Log.v(TAG, "download ret = [" + ret + "]");
        return "";
    }
    
    public ArrayList<Bundle> checkUpdate(Bundle info) {
        Log.v(TAG, "checkUpdate");
        String url = ServerInfo.getCheckUpdateUrl(mContext);
        Log.v(TAG, "checkUpdate url = [" + url.toString() + "]");
        ArrayList<Bundle> pkgs = WebClient.checkUpdate(mContext, url);
        return pkgs;
    }
    
    public String checkCategory(Bundle info) {
        Log.v(TAG, "checkCategory");
        String url = ServerInfo.getCheckCategoryUrl(mContext);
        Log.v(TAG, "checkCategory url = [" + url + "]");
        String json = WebClient.checkCategory(mContext, url);
        Log.v(TAG, "checkCategory json = [" + json + "]");
        return json;
    }

    public void uninit() {
        Log.v(TAG, "uninit");
    }
}

