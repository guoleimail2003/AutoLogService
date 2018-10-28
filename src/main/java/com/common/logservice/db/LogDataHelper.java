package com.common.logservice.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.common.logservice.util.PriorityValues;
import com.common.logservice.util.TypeValues;
import com.common.logservice.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

public class LogDataHelper implements DbFiledName {

    private static final String TAG = "LogDataHelper";
    private Context mContext;

    private static class DataBaseHelper extends SQLiteOpenHelper {

        private static final int DATABASE_VERSION = 1;
        private static final String DB_NAME = "logs.db";
        private static final String TABLE_NAME = "task";


        private DataBaseHelper(Context context) {
            super(context, DB_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                    + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + IMEI_1 + " TEXT NOT NULL, "
                    + IMEI_2 + " TEXT, "
                    + TYPE + " INTEGER DEFAULT 0,"
                    + PRIORITY + " INTEGER DEFAULT 0,"
                    + JSON_OBJECT + " TEXT,"
                    + TITLE + " TEXT,"
                    + FILE_PATH + " TEXT,"
                    + FILE_COUNT + " INTEGER DEFAULT 1,"
                    + RX_TIME + " datetime DEFAULT current_timestamp, "
                    + TX_TIME + " datetime,"
                    + ERR_COUNT + " INTEGER DEFAULT 0,"
                    + ERR + " TEXT"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            Log.w(TAG, "Upgrading database from version " + oldVersion
                    + " to " + newVersion
                    + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }

        private long insert(ContentValues values) throws SQLiteException {
            SQLiteDatabase db = getWritableDatabase();
            long id = db.insert(TABLE_NAME, null, values);
            Log.v(TAG, "insert id = " + id);
            return id;
        }

        public void delete(long id) {
            SQLiteDatabase db = getWritableDatabase();
            int num = db.delete(TABLE_NAME, ID + "="+id ,null);
            Log.v(TAG, "delete num = " + num);
        }

        public int update(long id, ContentValues values) {
            SQLiteDatabase db = getWritableDatabase();
            String where = ID + "=" + id;
            int num = db.update(TABLE_NAME, values, where, null);
            Log.v(TAG, "update num = " + num);
            return num;
        }

        private Cursor query(String[] projection, String selection,
                            String[] selectionArgs, String sortOrder) {
            SQLiteDatabase db = getReadableDatabase();
            return db.query(TABLE_NAME, projection, selection,
                    selectionArgs, null, null, sortOrder);
        }
    }

    private DataBaseHelper helper = null;

    public LogDataHelper(Context context) {
        helper = new DataBaseHelper(context);
        mContext = context;
    }

    public long addTask(TypeValues type,
                        PriorityValues priority, JSONObject jobj,
                        String title, String file_path, int file_count) {
        String imei1 = Util.queryIMEI(mContext);
        String imei2 = Util.queryIMEI(mContext);
        Log.v(TAG, "addTask imei_1 = " + imei1
                + " imei_2 = " + imei2
                +  " type = " + type
                + " priority = " + priority);
        Log.d(TAG, "addTask  jobj = " + (jobj == null?null:jobj.toString()));
        ContentValues cv = new ContentValues();
        cv.put(IMEI_1, imei1);
        cv.put(IMEI_2, imei2);
        cv.put(TYPE, type.getValue());
        cv.put(PRIORITY, priority.getValue());
        cv.put(JSON_OBJECT, jobj.toString());
        cv.put(TITLE, title);
        cv.put(FILE_PATH, file_path);
        cv.put(FILE_COUNT, file_count);

        return helper.insert(cv);
    }

    public void updateTaskObject(E_Record r) {
        ContentValues cv = new ContentValues();
        cv.put(JSON_OBJECT, r.getObject().toString());
        helper.update(r.getId(), cv);
    }

    public void removeTask(long id) {
        Log.v(TAG, "removeTask id = " + id);
        helper.delete(id);
    }

    public int queryNextTaskDelay() {

        int nextCall = -1;

        String queryStr = String.format("select count(*), min(julianday(%s)) - julianday(current_timestamp) ",
                TX_TIME);

        queryStr += String.format("from %s where %s>current_timestamp ",
                DataBaseHelper.TABLE_NAME, TX_TIME);

        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery(queryStr, null);
        if (c.moveToFirst()) {
            int count = c.getInt(0);
            if (count > 0) {
                double v = c.getDouble(1);
                nextCall = (int) Math.ceil(v * 24 * 60);
            }
        }
        c.close();

        return nextCall;
    }

    public E_Record queryFirstTask() {
        E_Record r = null;
        Cursor c =  helper.query(new String[]{ID, IMEI_1, IMEI_2, TYPE, PRIORITY, JSON_OBJECT, FILE_PATH, TITLE},
                RX_TIME + " <= current_timestamp", null, PRIORITY + "," + ID + " limit 1");
        if (c.getCount() > 0 && c.moveToFirst()) {
            r = new E_Record();
            r.setId(c.getLong(c.getColumnIndex(ID)));
            r.setImei_1(c.getString(c.getColumnIndex(IMEI_1)));
            r.setImei_2(c.getString(c.getColumnIndex(IMEI_2)));
            r.setPriority(PriorityValues.getPriority(c.getInt(c.getColumnIndex(PRIORITY))));
            r.setType(TypeValues.getType(c.getInt(c.getColumnIndex(TYPE))));
            String str = c.getString(c.getColumnIndex(JSON_OBJECT));
            r.setTitle(c.getString(c.getColumnIndex(TITLE)));
            r.setFile_path(c.getString(c.getColumnIndex(FILE_PATH)));
            Log.v(TAG, "queryFirstTask id = [" + r.getId() + "]"
                    + " imei_1 = [" + r.getImei_1() + "]"
                    + " imei_2 = [" + r.getImei_2() + "]"
                    + " priority = [" + r.getPriority() + "]"
                    + " type = [" + r.getType() + "]"
                    + " title = [" + r.getTitle() + "]"
                    + " file_path = [" + r.getFile_path() + "]"
                    + " file_count = [" + r.getFile_count() + "]");
            try {
                r.setObject(new JSONObject(str));
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "queryFirstTask JSONException = [" + str + "]");
                r.setObject(null);
            } finally {
                c.close();
            }
        }
        return r;
    }

    public void changeTask(long id, int delay, int priority, int errcount, String err) {
        Log.v(TAG, "changeTask id = " + id);
        String queryStr = String.format("update %s set ",
                DataBaseHelper.TABLE_NAME);

        String sep = "";
        if (delay >= 0) {
            queryStr += sep;
            sep = ",";
            queryStr += String.format("%s=datetime(julianday(current_timestamp) + %d.0/(24*60)) ",
                    TX_TIME, delay);
        }

        if (priority >= 0) {
            queryStr += sep;
            sep = ",";
            queryStr += String.format("%s=%d ", PRIORITY, priority);
        }

        queryStr += String.format("where %s=%d", ID, id);

        SQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL(queryStr);
    }

    public void delay(long id, int delay, int errcount, String err) {
        changeTask(id, delay, -1, errcount, err);
    }
}
