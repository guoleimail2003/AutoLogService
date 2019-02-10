package com.common.logservice;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogServiceManager extends Activity {

    private Button mReportUserException;
    private Button mUploadFileLog;
    private Button mUploadMultiFileLog;
    private Button mCheckupdate;
    private Button mUpdateIPAndPort;
    private ILogService mLogService;
    private static int exception_index = 0;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //test code ,can not do network access in main thread.
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        setContentView(R.layout.test_main);

        mReportUserException = (Button) findViewById(R.id.reportUserException);
        mUploadFileLog = (Button) findViewById(R.id.uploadLogFile);
        mUploadMultiFileLog = (Button) findViewById(R.id.uploadMultiLogFile);
        mCheckupdate = (Button) findViewById(R.id.checkupdate);
        mUpdateIPAndPort = (Button) findViewById(R.id.updateipandport);


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
                String description = "Single File uploaded";
                Date date = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
                String file_name = sdf.format(date);
                File f = new File("/sdcard/" + file_name + ".txt");
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
                String file_path = "/sdcard/" + file_name + ".txt";
                Bundle b = new Bundle();
                try {
                    mLogService.uploadLogFile(description, file_path, 1, b);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        mUploadMultiFileLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String description = "Multi File uploaded";
                File f = new File(Environment.getExternalStorageDirectory() + "/1.txt");
                File f2 = new File(Environment.getExternalStorageDirectory() + "/1.txt_1");
                File f3 = new File(Environment.getExternalStorageDirectory() + "/1.txt_2");
                if (f.canWrite() && !f.exists()) {
                    try {
                        f.getParentFile().mkdirs();
                        f.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (f2.canWrite() && !f2.exists()) {
                    try {
                        f2.getParentFile().mkdirs();
                        f2.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (f3.canWrite() && !f3.exists()) {
                    try {
                        f3.getParentFile().mkdirs();
                        f3.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                FileOutputStream fout = null;
                try {
                    fout = new FileOutputStream(f);
                    fout.write(new Date().toString().getBytes());
                    fout.flush();
                    fout = new FileOutputStream(f2);
                    fout.write(new Date().toString().getBytes());
                    fout.flush();
                    fout = new FileOutputStream(f3);
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
                String file_path = f.getAbsolutePath();
                Bundle b = new Bundle();
                try {
                    mLogService.uploadLogFile(description, file_path, 3, b);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        mCheckupdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                try {
                    mLogService.checkUpdate(bundle);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        mUpdateIPAndPort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                try {
                    mLogService.updateServerIPPort(bundle);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        Intent intent = new Intent();
        intent.setClassName("com.common.logservice", "com.common.logservice.LogService");
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mLogService = (ILogService.Stub.asInterface(service));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mLogService = null;
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(conn);
    }
}
