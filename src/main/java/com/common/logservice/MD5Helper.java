
package com.common.logservice;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.util.Log;
 
public class MD5Helper {
    
    private static final String TAG = "LogServiceMD5Helper";
    
    private static char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                                        'a', 'b', 'c', 'd', 'e', 'f' };
    
    public static final String MD5Digest(byte[] b) {
        
        Log.v(TAG, "MD5Digest");
        try {
            byte[] input = b;
            //Log.v(TAG, "MD5Digest input = " + input);
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            mdInst.update(input);
            byte[] md = mdInst.digest();
            int len = md.length;
            //Log.v(TAG, "MD5Digest md = " + md + " len = " + len);
            char str[] = new char[len * 2];
            int k = 0;
            for (int i = 0; i < len; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            String digest = new String(str);
            Log.v(TAG, "MD5Digest digest = [" + digest + "] length = [" + digest.length() + "]");
            return digest;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "MD5Digest Exception = " + e.getMessage());
        }
        return null;
    }
 
 
     public static final String MD5Digest(String original) {
        Log.v(TAG, "MD5Digest original = [" + original + "]");
        String digest = null;
        if (original != null) {
            byte[] input = original.getBytes();
            //Log.v(TAG, "MD5Digest input = " + input);
            MessageDigest instance = null;
            try {
                instance = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                Log.e(TAG, "MD5Digest Exception = " + e.getMessage());
                return digest;
            }

            if (instance != null) {
                instance.update(input);
                byte[] md = instance.digest();
                int len = md.length;
                //Log.v(TAG, "MD5Digest md = " + md + " len = " + len);
                char str[] = new char[len * 2];
                int k = 0;
                for (int i = 0; i < len; i++) {
                    byte byte0 = md[i];
                    str[k++] = hexDigits[byte0 >> 4 & 0xf];
                    str[k++] = hexDigits[byte0 & 0xf];
                }
                digest = new String(str);
                Log.v(TAG, "MD5Digest digest = [" + digest + "] length = [" + digest.length() + "]");
            }
        }
        return digest;
     }
}

