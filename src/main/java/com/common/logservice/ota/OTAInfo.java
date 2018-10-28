package com.common.logservice.ota;

import org.json.JSONException;
import org.json.JSONObject;

public class OTAInfo {
    private String TAG = OTAInfo.class.getSimpleName();

    private static final String NAME = "'name";
    private static final String VERSION = "version";
    private static final String DESCRIPTION = "description";
    private static final String UPDATEAT = "updateAt";
    private static final String FIRMWARE = "firmware";


    private String name;
    private String version;
    private String description;
    private String updateAt;
    private String firmwareURL;

    private JSONObject mJSONParser;
    private String[] mColumns = {NAME, VERSION, DESCRIPTION, UPDATEAT, FIRMWARE};

    OTAInfo(String obj) {
        try {
            mJSONParser = new JSONObject(obj);
            setName(mJSONParser.getString(NAME));
            setVersion(mJSONParser.getString(VERSION));
            setFirmwareURL(mJSONParser.getString(FIRMWARE));
            setDescription(mJSONParser.getString(DESCRIPTION));
            setUpdateAt(mJSONParser.getString(UPDATEAT));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String ver) {
        this.version = ver;
    }

    public String getFirmwareURL() {
        return firmwareURL;
    }

    public void setFirmwareURL(String url) {
        this.firmwareURL = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String des) {
        this.description = des;
    }

    public String getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(String updateAt) {
        this.updateAt = updateAt;
    }
}
