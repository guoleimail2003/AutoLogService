package com.common.logservice.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

public class LogProvider extends ContentProvider implements DBInfo {
    private final static String TAG = "LogProvider";

    public static final String AUTHORITY = "com.common.logservice";
    public static final int TASK_CODE = 1;
    public static final int FIRMWARE_CODE = 2;

    private Context mContext;
    private LogHelper mLogHelper;


    private static final UriMatcher mMatcher;

    static {
        mMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        //if Uri matched content://com.common.logservice/task, return TASK
        mMatcher.addURI(AUTHORITY, "task", TASK_CODE);
        //content://com.common.logservice/firmware, return FRIMWARE_CODE
        mMatcher.addURI(AUTHORITY, "firmware", FIRMWARE_CODE);
    }

    @Override
    public boolean onCreate() {
        initVar();
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType( Uri uri) {
        return null;
    }

    @Override
    public Uri insert( Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete( Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update( Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    private void initVar() {
        mContext = getContext();
    }

    public static String getTableName(Uri uri) {
        String table_name = null;
        switch (mMatcher.match(uri)) {
            case TASK_CODE:
                table_name = DBInfo.TABLE_TASK;
                break;
            case FIRMWARE_CODE:
                table_name = DBInfo.TABLE_FIRMWARE;
                break;
            default:
                break;
        }
        return table_name;
    }
}
