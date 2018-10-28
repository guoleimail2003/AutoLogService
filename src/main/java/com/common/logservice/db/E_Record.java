package com.common.logservice.db;

import com.common.logservice.util.PriorityValues;
import com.common.logservice.util.TypeValues;

import org.json.JSONObject;

public class E_Record {
    private long id;
    private String imei_1;
    private String imei_2;
    private TypeValues type;
    private PriorityValues priority;
    private JSONObject object;
    private String title;
    private String file_path;
    private int file_count;
    private String rx_time;
    private String tx_time;
    private String description;
    private int errcount;
    private String err;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getImei_1() {
        return imei_1;
    }

    public void setImei_1(String imei_1) {
        this.imei_1 = imei_1;
    }

    public String getImei_2() {
        return imei_2;
    }

    public void setImei_2(String imei_2) {
        this.imei_2 = imei_2;
    }

    public TypeValues getType() {
        return type;
    }

    public void setType(TypeValues type) {
        this.type = type;
    }

    public PriorityValues getPriority() {
        return priority;
    }

    public void setPriority(PriorityValues priory) {
        this.priority = priory;
    }

    public JSONObject getObject() {
        return object;
    }

    public void setObject(JSONObject object) {
        this.object = object;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFile_path() {
        return file_path;
    }

    public void setFile_path(String file_path) {
        this.file_path = file_path;
    }

    public int getFile_count() {
        return file_count;
    }

    public void setFile_count(int file_count) {
        this.file_count = file_count;
    }

    public String getRx_time() {
        return rx_time;
    }

    public void setRx_time(String rx_time) {
        this.rx_time = rx_time;
    }

    public String getTx_time() {
        return tx_time;
    }

    public void setTx_time(String tx_time) {
        this.tx_time = tx_time;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getErrcount() {
        return errcount;
    }

    public void setErrcount(int errcount) {
        this.errcount = errcount;
    }

    public String getErr() {
        return err;
    }

    public void setErr(String err) {
        this.err = err;
    }
}
