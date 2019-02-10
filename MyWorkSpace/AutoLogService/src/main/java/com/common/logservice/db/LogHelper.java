package com.common.logservice.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import com.common.logservice.log.SystemLog;
import com.common.logservice.util.PriorityValues;
import com.common.logservice.util.TypeValues;

import org.json.JSONException;
import org.json.JSONObject;

public class LogHelper extends SQLiteOpenHelper implements DBInfo {

    private static final String TAG = "LogHelper";

    public LogHelper(Context context) {

        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //create table task
        db.execSQL("CREATE TABLE " + TABLE_TASK + " ("
                + TASK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + TASK_IMEI_1 + " TEXT NOT NULL, "
                + TASK_IMEI_2 + " TEXT, "
                + TASK_TYPE + " INTEGER DEFAULT 0,"
                + TASK_PRIORITY + " INTEGER DEFAULT 0,"
                + TASK_JSON_OBJECT + " TEXT,"
                + TASK_DESCRIPTION + " TEXT,"
                + TASK_FILE_PATH + " TEXT,"
                + TASK_FILE_COUNT + " INTEGER DEFAULT 0,"
                + TASK_RX_TIME + " datetime DEFAULT current_timestamp, "
                + TASK_TX_TIME + " datetime,"
                + TASK_ERR_COUNT + " INTEGER DEFAULT 0,"
                + TASK_ERR + " TEXT"
                + ");");
        //create table firmware
        db.execSQL("CREATE TABLE " + TABLE_FIRMWARE + " ("
                + FIRMWARE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + FIRMWARE_NAME + " TEXT NOT NULL, "
                + FIRMWARE_VERSION + " TEXT, "
                + FIRMWARE_URI + " TEXT,"
                + FIRMWARE_DESCRIPTION + " TEXT"
                + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        Log.w(TAG, "Upgrading database from version " + oldVersion
                + " to " + newVersion
                + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASK);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FIRMWARE);
        onCreate(db);
    }

    private long insert(String table_name, ContentValues values) throws SQLiteException {
        long id = -1;
        if (table_name.equals(TABLE_TASK)) {
            id = insertTask(values);
        } else if (table_name.equals(TABLE_FIRMWARE)) {
            id = insertFirmware(values);
        } else {
            SystemLog.LOGE(TAG, "There is no match table when insert the value into table = [" + table_name + "]");
        }
        Log.v(TAG, "insert id = " + id);
        return id;
    }

    private long insertTask(ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();
        long id = db.insert(TABLE_TASK, null, values);
        Log.v(TAG, "insert TABLE_TASK id = " + id);
        return id;
    }

    private long insertFirmware(ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();
        long id = db.insert(TABLE_FIRMWARE, null, values);
        Log.v(TAG, "insert TABLE_FIRMWARE id = " + id);
        return id;
    }

    public void delete(long id, String table) {
        SQLiteDatabase db = getWritableDatabase();
        String where = "";
        if (TABLE_TASK.equals(table)) {
            where = TASK_ID + "=" + id;
        } else if (TABLE_FIRMWARE.equals(table)) {
            where = FIRMWARE_ID + "=" + id;
        }
        int num = db.delete(table, where,null);
        Log.v(TAG, "delete num = " + num);
    }

    public int update(String table, long id, ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();
        String where = "";
        if (TABLE_TASK.equals(table)) {
            where = TASK_ID + "=" + id;
        } else if (TABLE_FIRMWARE.equals(table)) {
            where = FIRMWARE_ID + "=" + id;
        }
        int num = db.update(table, values, where, null);
        Log.v(TAG, "update num = " + num);
        return num;
    }

    private Cursor query(String table, String[] projection, String selection,
                         String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = getReadableDatabase();
        return db.query(table, projection, selection,
                selectionArgs, null, null, sortOrder);
    }

    public long addTask(TypeValues type,
                        String[] imei_1_2,
                        PriorityValues priority, JSONObject jobj,
                        String title, String file_path, int file_count) {
        String imei1 = "";
        String imei2 = "";
        if (imei_1_2.length > 1) {
            //Mutlti Sim and Single IMEI device;
            imei1 = imei_1_2[0];
            imei2 = imei_1_2[1];
        } else {
            //Single SIM and Single IMEI device;
            imei1 = imei_1_2[0];
        }

        Log.v(TAG, "addTask imei_1 = " + imei1
                + " imei_2 = " + imei2
                +  " type = " + type
                + " priority = " + priority);
        Log.d(TAG, "addTask  jobj = " + (jobj == null?null:jobj.toString()));
        ContentValues cv = new ContentValues();
        cv.put(TASK_IMEI_1, imei1);
        cv.put(TASK_IMEI_2, imei2);
        cv.put(TASK_TYPE, type.getValue());
        cv.put(TASK_PRIORITY, priority.getValue());
        cv.put(TASK_JSON_OBJECT, jobj.toString());
        cv.put(TASK_DESCRIPTION, title);
        cv.put(TASK_FILE_PATH, file_path);
        cv.put(TASK_FILE_COUNT, file_count);

        return insert(TABLE_TASK, cv);
    }

    public void updateTaskObject(E_Record r) {
        ContentValues cv = new ContentValues();
        cv.put(TASK_JSON_OBJECT, r.getObject().toString());
        update(TABLE_TASK, r.getId(), cv);
    }

    public void removeTask(long id) {
        Log.v(TAG, "removeTask id = " + id);
        delete(id, TABLE_TASK);
    }

    public int queryNextTaskDelay() {

        int nextCall = -1;

        String queryStr = String.format("select count(*), min(julianday(%s)) - julianday(current_timestamp) ",
                TASK_TX_TIME);

        queryStr += String.format("from %s where %s>current_timestamp ",
                TABLE_TASK, TASK_TX_TIME);

        Cursor c = getReadableDatabase().rawQuery(queryStr, null);
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

    public E_Record queryTaskFirstTask() {
        E_Record r = null;
        Cursor c =  query(TABLE_TASK, new String[]{TASK_ID, TASK_IMEI_1, TASK_IMEI_2, TASK_DESCRIPTION, TASK_TYPE, TASK_PRIORITY, TASK_JSON_OBJECT, TASK_FILE_PATH, TASK_FILE_COUNT,},
                TASK_RX_TIME + " <= current_timestamp", null, TASK_PRIORITY + "," + TASK_ID + " limit 1");
        if (c.getCount() > 0 && c.moveToFirst()) {
            r = new E_Record();
            r.setId(c.getLong(c.getColumnIndex(TASK_ID)));
            r.setImei_1(c.getString(c.getColumnIndex(TASK_IMEI_1)));
            r.setImei_2(c.getString(c.getColumnIndex(TASK_IMEI_2)));
            r.setPriority(PriorityValues.getPriority(c.getInt(c.getColumnIndex(TASK_PRIORITY))));
            r.setType(TypeValues.getType(c.getInt(c.getColumnIndex(TASK_TYPE))));
            String str = c.getString(c.getColumnIndex(TASK_JSON_OBJECT));
            r.setDescription(c.getString(c.getColumnIndex(TASK_DESCRIPTION)));
            r.setFile_path(c.getString(c.getColumnIndex(TASK_FILE_PATH)));
            r.setFile_count(c.getInt(c.getColumnIndex(TASK_FILE_COUNT)));
            Log.v(TAG, "queryFirstTask id = [" + r.getId() + "]"
                    + " imei_1 = [" + r.getImei_1() + "]"
                    + " priority = [" + r.getPriority() + "]"
                    + " type = [" + r.getType() + "]"
                    + " title = [" + r.getDescription() + "]"
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

    public E_Record queryFirstTask() {
        E_Record r = null;
        Cursor c =  query(TABLE_TASK, new String[]{TASK_ID, TASK_IMEI_1, TASK_IMEI_2, TASK_DESCRIPTION, TASK_TYPE, TASK_PRIORITY, TASK_JSON_OBJECT, TASK_FILE_PATH, TASK_FILE_COUNT,},
                TASK_RX_TIME + " <= current_timestamp", null, TASK_PRIORITY + "," + TASK_ID + " limit 1");
        if (c.getCount() > 0 && c.moveToFirst()) {
            r = new E_Record();
            r.setId(c.getLong(c.getColumnIndex(TASK_ID)));
            r.setImei_1(c.getString(c.getColumnIndex(TASK_IMEI_1)));
            r.setImei_2(c.getString(c.getColumnIndex(TASK_IMEI_2)));
            r.setPriority(PriorityValues.getPriority(c.getInt(c.getColumnIndex(TASK_PRIORITY))));
            r.setType(TypeValues.getType(c.getInt(c.getColumnIndex(TASK_TYPE))));
            String str = c.getString(c.getColumnIndex(TASK_JSON_OBJECT));
            r.setDescription(c.getString(c.getColumnIndex(TASK_DESCRIPTION)));
            r.setFile_path(c.getString(c.getColumnIndex(TASK_FILE_PATH)));
            r.setFile_count(c.getInt(c.getColumnIndex(TASK_FILE_COUNT)));
            Log.v(TAG, "queryFirstTask id = [" + r.getId() + "]"
                    + " imei_1 = [" + r.getImei_1() + "]"
                    + " priority = [" + r.getPriority() + "]"
                    + " type = [" + r.getType() + "]"
                    + " title = [" + r.getDescription() + "]"
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

    public E_Record queryFirstDownload() {
        E_Record r = null;
        Cursor c =  query(TABLE_TASK, new String[]{TASK_ID, TASK_IMEI_1, TASK_IMEI_2, TASK_DESCRIPTION, TASK_TYPE, TASK_PRIORITY, TASK_JSON_OBJECT, TASK_FILE_PATH, TASK_FILE_COUNT,},
                TASK_RX_TIME + " <= current_timestamp", null, TASK_PRIORITY + "," + TASK_ID + " limit 1");
        if (c.getCount() > 0 && c.moveToFirst()) {
            r = new E_Record();
            r.setId(c.getLong(c.getColumnIndex(TASK_ID)));
            r.setImei_1(c.getString(c.getColumnIndex(TASK_IMEI_1)));
            r.setImei_2(c.getString(c.getColumnIndex(TASK_IMEI_2)));
            r.setPriority(PriorityValues.getPriority(c.getInt(c.getColumnIndex(TASK_PRIORITY))));
            r.setType(TypeValues.getType(c.getInt(c.getColumnIndex(TASK_TYPE))));
            String str = c.getString(c.getColumnIndex(TASK_JSON_OBJECT));
            r.setDescription(c.getString(c.getColumnIndex(TASK_DESCRIPTION)));
            r.setFile_path(c.getString(c.getColumnIndex(TASK_FILE_PATH)));
            r.setFile_count(c.getInt(c.getColumnIndex(TASK_FILE_COUNT)));
            Log.v(TAG, "queryFirstTask id = [" + r.getId() + "]"
                    + " imei_1 = [" + r.getImei_1() + "]"
                    + " priority = [" + r.getPriority() + "]"
                    + " type = [" + r.getType() + "]"
                    + " title = [" + r.getDescription() + "]"
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
        String queryStr = String.format("update %s set ", TABLE_TASK);

        String sep = "";
        if (delay >= 0) {
            queryStr += sep;
            sep = ",";
            queryStr += String.format("%s=datetime(julianday(current_timestamp) + %d.0/(24*60)) ",
                    TASK_TX_TIME, delay);
        }

        if (priority >= 0) {
            queryStr += sep;
            sep = ",";
            queryStr += String.format("%s=%d ", TASK_PRIORITY, priority);
        }

        queryStr += String.format("where %s=%d", TASK_ID, id);

        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(queryStr);
    }

    public void delay(long id, int delay, int errcount, String err) {
        changeTask(id, delay, -1, errcount, err);
    }
}