package com.common.logservice.db;

public interface DBInfo {
    //Database name
     String DATABASE_NAME = "logs.db";

    //Table name
     int DATABASE_VERSION = 1;
     String TABLE_TASK = "task";
     String TABLE_FIRMWARE = "firmware";

    //Table filed name
     String TASK_ID = "_id";
     String TASK_IMEI_1 = "_imei_1";
     String TASK_IMEI_2 = "_imei_2";
     String TASK_TYPE = "_type";
     String TASK_PRIORITY = "_priority";
     String TASK_JSON_OBJECT = "_json_obj";
     String TASK_DESCRIPTION = "_description";
     String TASK_FILE_PATH = "_file_path";
     String TASK_FILE_COUNT = "_file_count";
     String TASK_RX_TIME = "_rx_time";
     String TASK_TX_TIME = "_tx_time";
     String TASK_ERR_COUNT = "_err_count";
     String TASK_ERR = "_err";

    //Talbe firmware field name
     String FIRMWARE_ID = "_id";
     String FIRMWARE_NAME = "_name";
     String FIRMWARE_VERSION = "_version";
     String FIRMWARE_URI = "_uri";
     String FIRMWARE_DESCRIPTION = "_description";

}
