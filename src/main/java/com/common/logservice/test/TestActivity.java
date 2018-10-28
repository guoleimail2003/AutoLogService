package com.common.logservice.test;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;

import com.common.logservice.ILogService;
import com.common.logservice.LogService;
import com.common.logservice.R;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Date;

public class TestActivity extends Activity {

    private Button mReportUserException;
    private Button mUploadFileLog;
    private Button mCheckupdate;
    private ILogService.Stub mLogService;
    private static int exception_index = 0;
    private static int upload_file_index = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //test code ,can not do network access in main thread.
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        setContentView(R.layout.test_main);

        mReportUserException = (Button)findViewById(R.id.reportUserException);
        mUploadFileLog = (Button)findViewById(R.id.uploadLogFile);
        mCheckupdate = (Button)findViewById(R.id.checkupdate);


        mReportUserException.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String description = "user_ecxeption" + (exception_index++);
                Bundle b = new Bundle();
                try {
                    mLogService.uploadUserException(description, b);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        mUploadFileLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String description =  "APP error";
                File f = new File("/sdcard/1.txt");
                if (f.canWrite() && !f.exists()) {
                    try {
                        f.getParentFile().mkdirs();
                        f.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                FileOutputStream fout = null;
                try {
                    fout = new FileOutputStream(f);
                    fout.write(new Date().toString().getBytes());
                    fout.flush();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        fout.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                String file_path = "/sdcard/1.txt";
                Bundle b = new Bundle();
                try {
                    mLogService.uploadLogFile(description, file_path, 1, b);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        mCheckupdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                String abcd = "abc";
                try {
                    mLogService.checkUpdate(abcd, bundle);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });


        Intent intent = new Intent(getApplicationContext(), LogService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mLogService = (ILogService.Stub)service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mLogService = null;
        }
    };

}
