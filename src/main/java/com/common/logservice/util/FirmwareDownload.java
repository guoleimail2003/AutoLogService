package com.common.logservice.util;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FirmwareDownload implements Runnable {
    private static final String TAG = FirmwareDownload.class.getSimpleName();

    private static final String FIRMWARE_NAME = "update.zip";
    private Thread mThread;
    //it show the current file download finished
    private Boolean mFinished;

    private long mDownloadedByes;


    private Long mStartOffset;
    private String mURL;
    private String mStorage_as = Environment.getExternalStorageDirectory().getPath() + "/" + FIRMWARE_NAME;

    public FirmwareDownload(String url, String storage_as) {
        super();
        this.mURL = url;
        this.mStartOffset = 0L;
        if (storage_as != null
                && !storage_as.isEmpty()) {
            this.mStorage_as = storage_as;
        }

        //initVar
        initVar();;

        //initi thread
        mThread = new Thread(this);
    }

    private void initVar() {
        mFinished = false;
        mDownloadedByes = 0;
    }

    @Override
    public void run() {
        //check the
        if (mURL != null && !mURL.isEmpty()
                && mStorage_as != null && !mStorage_as.isEmpty()) {
            while (!mFinished) {
                downloadFile(mURL, mStorage_as);
            }
        } else {
            Log.e(TAG, "The parameter have invalid paramer,"
                    + " mURL = " + mURL
                    + " mStorage_as = " + mStorage_as);
        }
    }

    public String downloadFile(String furl, String path) {
        Log.v(TAG, "downloadFile furl [" + furl + "] path = [" + path + "]");
        String ret = "OK";
        URL url = null;
        HttpURLConnection conn = null;
        InputStream is = null;
        BufferedInputStream bis = null;
        FileOutputStream fos = null;
        try {
            url = new URL(furl);
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(false);
            conn.setDoInput(true);
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Charset", "UTF-8");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(3000);

            File f_update = new File(path);
            long f_size = f_update.length();
            if (f_update.exists() &&  f_size > 0) {
                Log.v(TAG, "file size = " + f_size);
                conn.setRequestProperty("Range", "bytes=" + String.valueOf(f_size) + "-");
            } else {
                f_update.createNewFile();
            }

            int httpResponseCode = conn.getResponseCode();
            Log.v(TAG, "downloadFile httpResponseCode = [" + httpResponseCode + "]");

            long total = 0;
            if ((int)(HttpURLConnection.HTTP_OK/100) == (int)(httpResponseCode/100)) {
                is = conn.getInputStream();
                bis = new BufferedInputStream(is);
                fos = new FileOutputStream(new File(path));
                byte [] buffer = new byte[1024];
                int len = 0;
                while ((len = bis.read(buffer)) != -1) {
                    Log.v(TAG, "downloadFile len = [" + len + "] + total = [" + (total/1024/1024) + "M]");
                    fos.write(buffer, 0, len);
                    total += len;
                }
                Log.v(TAG, "downloadFile total = [" + total + "] completed");
            } else {
                //Server response error code is not 2xx
                Log.e(TAG, "downloadFile responsecode invalid");
                ret = "ERROR";
                mFinished = false;
                mStartOffset = total;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "downloadFile catch IOException = " + e.getMessage());
            ret = "FAILED";
        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.e(TAG, "downloadFile NullPointerException = " + e.getMessage());
            ret = "FAILED";
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "downloadFile Excpetion = " + e.getMessage());
            e.printStackTrace();
            ret = "FAILED";
        } finally {

            try {
                fos.flush();
                if (fos != null) {
                    fos.close();
                }

                if (bis != null) {
                    bis.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "downloadFile finally IOException = " + e.getMessage());
                ret = "FAILED";
            }
        }
        Log.v(TAG, "downloadFile ret = [" + ret + "]");

        return ret;
    }
}
