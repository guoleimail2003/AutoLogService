package com.common.logservice;

import android.content.Context;
import android.os.Environment;
import android.os.SystemProperties;
import android.util.Log;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class LogServiceOptions {

    private static final String TAG = "LogServiceOptions";

    private static JSONObject mJsonObject;
    private static ServerConfig mServerConfig;
    private static String DEFAULT = "{\"server_ip_addr\":\"192.168.31.232\",\"server_access_port\":3000}";
    private static String DEFAULT_FILE_PATH = Environment.getExternalStorageDirectory() + "/autoservice.txt";

	private String[] OPTION_FIELD_NAME = {
		"server_ip_addr",
		"server_access_port",

			//Add options here first step

	};

	private static enum  OPTION_FIELD_INDEX {
		SERVER_IP_ADDR(0),
		SERVER_ACCESS_PORT(1)
		;

		private int value;
		OPTION_FIELD_INDEX(int v) {
			this.value = v;
		}
	};

	public class ServerConfig {
		private String serverIPAddr;
		private Integer serverAccessPort;

		//Add option here third step.

		ServerConfig(String cfg) {
			mJsonObject = parseJson(cfg);
			for (String str : OPTION_FIELD_NAME) {
				try {
					if (mJsonObject.has(str)) {
						if (str.equals(OPTION_FIELD_NAME[0])) {
							setServerIPAddr(mJsonObject.getString(str));
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}

			}

		}

		public ServerConfig getServerConfig(String file_path) {
			if (mServerConfig == null) {
				mServerConfig = new ServerConfig(file_path);
			}
			return mServerConfig;
		}

		public String getServerIPAddr() {
			return serverIPAddr;
		}

		public void setServerIPAddr(String serverIPAddr) {
			this.serverIPAddr = serverIPAddr;
		}

		public Integer getServerAccessPort() {
			return serverAccessPort;
		}

		public void setServerAccessPort(Integer serverAccessPort) {
			this.serverAccessPort = serverAccessPort;
		}
	}

    public LogServiceOptions(String cfg_path) {
    	super();
    	//mServerConfig = getServerConfig(cfg_path);
    }


	public JSONObject parseJson(String cfg) {
		JSONObject jsonObject = null;
		try {
			jsonObject = new JSONObject(cfg);

			for (String field: OPTION_FIELD_NAME) {
				if (jsonObject.has(field)) {
					Log.v(TAG, "parseJson " + field + " = " + jsonObject.getString(field));
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return jsonObject;
	}
}
