package com.common.logservice.util;

public enum PriorityValues {
    P_FATAL("FATAL", 1),
    P_ERR("ERROR",2),
    P_WARNING("WARNING",3),
    P_INFO("INFO",4),
    P_DEBUG("DEBUG", 5),
    P_VERBOSE("VERBOSE", 6);

    private String prio;
    private int val;
    PriorityValues(String prio, int val) {
        this.prio = prio;
        this.val = val;
    }

    public String getPrio() {
        return this.prio;
    }

    public int getValue() {
        return this.val;
    }

    public static PriorityValues getPriority(int val){
        for (PriorityValues t : PriorityValues.values()) {
            if (t.getValue() == val) {
                return t;
            }
        }
        return null;
    }
}
