package com.common.logservice;

import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class LogServiceOptions {

    private static final String TAG = "LogServiceOptions";

    private String mProperty = null;

    private static final int SECTION_COUNT = 30;

    private static final String PROPERTY_LOG_SERVICE = "persist.sys.log_service";
    private static final String PROPERTY_LOG_SERVICE_DEFAULT = "I0,0,I1,0,I2,0,I3,0,I4,0,I5,0,I6,0,I7,0,I8,0,I9,0,I10,1,I11,1,I12,1,I13,1,I14,1";

	protected Map<String, Boolean> mapOption;
	protected Map<String, Integer> mapPropIndex;

    private long optionUpdateStamp = 0;

    public LogServiceOptions() {
		mapOption = new HashMap<>();
		mapPropIndex = new HashMap<>();
		
		mapOption.put("DeleteAfterUpload", true);
		mapPropIndex.put("DeleteAfterUpload", 1);
		
		mapOption.put("ReportModem", true);
		mapPropIndex.put("ReportModem", 21);
		
		mapOption.put("ReportELogHl", true);
		mapPropIndex.put("ReportELogHl", 23);
		
		mapOption.put("ReportELogPl", true);
		mapPropIndex.put("ReportELogPl", 25);
		
		mapOption.put("ReportApAnr", true);
		mapPropIndex.put("ReportApAnr", 27);
		
		mapOption.put("ReportApCrash", true);
		mapPropIndex.put("ReportApCrash", 29);
		
		mapOption.put("Location", true);
		mapPropIndex.put("Location", 3);
    }

	public boolean DeleteAfterUpload() {
		return mapOption.get("DeleteAfterUpload");
	}

	public void parseJson(String cfg) {
		
		try {
			JSONObject jobj = new JSONObject(cfg);
			
			
			for (Entry<String, Boolean> entry: mapOption.entrySet()) {
				String key = entry.getKey();
				if (jobj.has(key)) {
					entry.setValue(jobj.getString(key).equals("1"));
					Log.v(TAG, "parseJson " + key + " = " + jobj.getString(key));
				}
			}
		} catch (JSONException e) {
		}
		
	}

    public int queryProperty(String property) {
        if (property != null) {
            mProperty = property;
        }
        else {
            mProperty = SystemProperties.get(PROPERTY_LOG_SERVICE, PROPERTY_LOG_SERVICE_DEFAULT);
        }
        Log.v(TAG, "queryProperty mProperty = " + mProperty);
        return parseProperty();
    }

    private int parseProperty() {

        int ret = -1;
        Log.v(TAG, "parseProperty mProperty = " + mProperty);
        if (mProperty != null) {
            String [] configs = mProperty.split(",");

            if (configs.length == SECTION_COUNT) {
				
				for (Entry<String, Integer> entry: mapPropIndex.entrySet()) {
					String key = entry.getKey();
					int index = entry.getValue();
					int val = Integer.parseInt(configs[index]);
					mapOption.put(key, (val != 0));
					Log.v(TAG, "parseProperty " + key + " = " + val);
				}
                ret = 0;

            }else {
                Log.v(TAG, "parseProperty mProperty invalid");
            }
        }
        Log.v(TAG, "parseProperty ret = " + ret);

        return ret;
    }

}
