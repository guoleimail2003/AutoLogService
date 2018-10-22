/*******************************************************************************
    Copyright(c) 2008 - 2015 Leadcore Technology CO.,LTD.
    All Rights Reserved. By using this module you agree to the terms of the
    Leadcore Technology CO.,LTD License Agreement for it.
********************************************************************************
* Filename	: Utils.java
*
* Description	: <describing what this file is to do>
*
* Notes		: <the limitations to use this file>
*
*--------------------------------------------------------------------------------
* Change History: 
*--------------------------------------------------------------------------------
*          
* 2015-07-21, AUTHOR, create originally.
*
*******************************************************************************/

package com.common.logservice;

import android.content.Context;
import android.util.Log;


public class Utils {
    private static final String TAG = "AutoLogService.Utils";
       
    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }
    
    public static boolean isNumeric(final CharSequence cs) {
        if (isEmpty(cs)) {
            return false;
        }
        final int sz = cs.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isDigit(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}

