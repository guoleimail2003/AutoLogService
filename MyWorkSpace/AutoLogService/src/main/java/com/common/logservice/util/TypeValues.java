package com.common.logservice.util;

public enum  TypeValues{
    T_BASE("BASE", 0),
    T_PING("PING", 1),
    T_REPORT_EXCEPTION("REPORT_EXCEPTION", 2),
    T_UPLOAD_LOGFILE("UPLOAD_LOGFILE",3),
    T_MODEM_DUMP("MODEM_DUMP", 4),
    T_APP_CRASH("APP_CRASH",5),
    T_APP_NOT_RESPONSE("APP_NOT_RESPONSE", 6);

    private String type;
    private int val;
    TypeValues(String type, int val) {
        this.type = type;
        this.val = val;
    }

    public static TypeValues getType(int val){
        for (TypeValues t : TypeValues.values()) {
            if (t.val == val) {
                return t;
            }
        }
        return null;
    }

    public int getValue(){
        return val;
    }

    public String getTypeName() {
        return type;
    }
};
