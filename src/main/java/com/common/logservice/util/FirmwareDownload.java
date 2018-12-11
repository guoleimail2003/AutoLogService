package com.common.logservice.util;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class FirmwareDownload implements Runnable {
    private static final String TAG = FirmwareDownload.class.getSimpleName();

    private static final String FIRMWARE_NAME = "update.zip";
    private Thread mThread;
    //it show the current file download finished
    private static Boolean mFinished;
    private static Boolean mThreadDownloading;
    private static Integer mThreadIndex = 0;
    private static Integer mThreadRetry = 0;

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
        if (mThread == null && mFinished) {
            mThread = new Thread(this);
        }
    }

    private void initVar() {
        mFinished = false;
        mDownloadedByes = 0;
        mThreadDownloading = false;
        if (mThreadIndex++ > 254) {
            mThreadIndex = 0;
        }
    }

    @Override
    public synchronized void run() {
        //check the thread is downloading
        if (mThreadDownloading) {
            return;
        }

        mThreadDownloading = true;

        if (mURL != null && !mURL.isEmpty()
                && mStorage_as != null && !mStorage_as.isEmpty()) {
            while (!mFinished) {
                String ret = downloadFile(mURL, mStorage_as);
                if ("OK".equals(ret)) {
                    mFinished = true;
                } else if ("FAILED".equals(ret)) {
                    mFinished = false;
                    mThreadRetry++;
                    if (mThreadRetry == 5) {
                        Log.d(TAG, "Thread retry 5 times and failed still, abort task");
                        mFinished = true;
                    }
                }
                try {
                    Thread.sleep(200L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Log.e(TAG, "The parameter have invalid paramer,"
                    + " mURL = " + mURL
                    + " mStorage_as = " + mStorage_as);
        }

        Log.d(TAG, "Download Success");
        mThreadDownloading = false;
    }

    public String downloadFile(String furl, String path) {
        Log.v(TAG, "downloadFile furl [" + furl + "] path = [" + path + "]");
        String ret = "OK";
        URL url = null;
        HttpURLConnection conn = null;
        InputStream is = null;
        BufferedInputStream bis = null;
        RandomAccessFile fos = null;
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
                fos = new RandomAccessFile(new File(path), "rw");
                fos.seek(f_size);

                byte [] buffer = new byte[1024*4];
                int len = 0;
                while ((len = bis.read(buffer)) != -1) {
                    Log.v(TAG, "downloadFile len = [" + len + "] + total = [" + (total/1024/1024) + "M]");
                    fos.write(buffer, 0, len);
                    total += len;
                }
                mFinished = false;
                Log.v(TAG, "mFinished = [" + mFinished + "] downloadFile total = [" + total + "] completed");
            } else {
                //Server response error code is not 2xx
                Log.e(TAG, "downloadFile responsecode invalid");
                ret = "FAILED";
                mFinished = false;
                mStartOffset = total;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "downloadFile catch IOException = " + e.getMessage());
            ret = "FAILED";
            mFinished = true;
        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.e(TAG, "downloadFile NullPointerException = " + e.getMessage());
            ret = "FAILED";
            mFinished = true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "downloadFile Excpetion = " + e.getMessage());
            e.printStackTrace();
            mFinished = true;
            ret = "FAILED";
        } finally {

            try {
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
