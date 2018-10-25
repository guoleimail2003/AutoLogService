package com.common.logservice.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;

public class WebClient {
    private static final String TAG = WebClient.class.getSimpleName();

    public static class BadResponseException extends Exception {
         BadResponseException(String msg) {
            super(msg);
        }
    }

    public static class BadFileException extends Exception {
        BadFileException(String msg) {
            super(msg);
        }
    }

    public static class NotReachException extends Exception {
        NotReachException(String msg) {
            super(msg);
        }
    }

    public static String postRequest(URL url, Map<String, String> params) throws IOException {
        Log.v(TAG, "postRequest url = [" + url.toString() + "]");
        StringBuilder stringBuffer = new StringBuilder();

        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                try {
                    stringBuffer.append(entry.getKey())
                            .append("=")
                            .append(URLEncoder.encode(entry.getValue(), "utf-8"))
                            .append("&");
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
            //
            stringBuffer.deleteCharAt(stringBuffer.length() - 1);
            //System.out.println("-->>" + stringBuffer.toString());
        }

        HttpURLConnection connection = openPostConnection(url);

        connection.setConnectTimeout(3000);
        
        Log.v(TAG, "postRequest url = [" + stringBuffer.toString() + "]");

        //
        byte[] mydata = stringBuffer.toString().getBytes();

        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Content-Length", String.valueOf(mydata.length));

        OutputStream outputStream = connection.getOutputStream();

        outputStream.write(mydata);
        outputStream.close();

        return getResponseString(connection);
    }

    static String getResponseString(HttpURLConnection connection) throws IOException {
        Log.v(TAG, "getResponseString");
        // read response
        String strLine;
        String strResponse = "";
        InputStream response = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(response));
        while ((strLine = reader.readLine()) != null) {
            strResponse += strLine + "\n";
        }
        Log.v(TAG, "getResponseString return string = [" + strResponse + "]");
        reader.close();
        return strResponse;
    }

    static HttpURLConnection openPostConnection(URL url) throws IOException {
        HttpURLConnection connection;
        connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Charset", "UTF-8");
        return connection;
    }


    public static String httpGet(URL url) throws IOException {
        Log.v(TAG, "httpGet url = [" + url.toString() + "]");
        HttpURLConnection connection;
        String result = "";

        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Charset", "UTF-8");
        connection.setRequestProperty("accept", "*/*");
        connection.setRequestProperty("connection", "Keep-Alive");
        connection.setConnectTimeout(3000);
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            result = getResponseString(connection);
        }

        return result;
    }




    private static final String BOUNDARY = "----------HV2ymHFg03ehbqgZCaKO6jyH";
    private static final String sepBoundary = "--" + BOUNDARY + "\r\n";
    private static final String endBoundary = "--" + BOUNDARY + "--\r\n";


    private static DataInputStream prepareFileStream(String path, int startOffset) throws BadFileException {
        DataInputStream dis;
        try {
            dis = new DataInputStream(new FileInputStream(path));
            if (startOffset > dis.available()) {
                throw new BadFileException("invalid file offset");
            }
            dis.skipBytes(startOffset);
        } catch (Exception e) {
            throw new BadFileException(e.getMessage());
        }
        return dis;
    }

    private static void emitParamter(OutputStream out, String key, String val) throws IOException {
        StringBuilder contentBody = new StringBuilder();
        contentBody.append("Content-Disposition: form-data; name=\"")
                .append(key)
                .append("\"\r\n")
                .append("\r\n")
                .append(val)
                .append("\r\n")
                .append(sepBoundary);

        out.write(contentBody.toString().getBytes("utf-8"));
    }

    private static void emitParamters(OutputStream out, Map<String, String> params) throws IOException {
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            Log.v(TAG, "emitParamters key = [" + key + "] value = [" + value + "]");
            emitParamter(out, key, value);
        }
    }

    private static JSONObject uploadFileChunk(
            URL url, String path, int offset, int blockSize, Map<String, String> params)
            throws BadFileException, NotReachException, BadResponseException {
        Log.v(TAG, "uploadFileChunk url = [" + url + "] path = [" + path + "] offset = [" + offset + "] blockSize = [" + blockSize + "]");
        File file = new File(path);
        String filename = file.getName();
        DataInputStream dis = prepareFileStream(path, offset);

        final int READ_BUF_SIZE = 8192;
        HttpURLConnection connection;

        try {
            connection = openPostConnection(url);
        } catch (Exception e) {
            throw new NotReachException(e.getMessage());
        }

        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
        connection.setRequestProperty("Accept-Encoding", "application/json");

        OutputStream out = null;
        try {
            out = connection.getOutputStream();
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean eof = false;
        try {
            eof = dis.available() <= blockSize;
        }catch (IOException e) {
            e.printStackTrace();
        }
        Log.v(TAG, "uploadFileChunk eof = [" + eof + "]");
        try {
            out.write(sepBoundary.getBytes("utf-8"));
            emitParamters(out, params);
            //emitParamter(out, "offset", Integer.toString(offset));
            //emitParamter(out, "eof", eof ? "1" : "0");
        } catch (UnsupportedEncodingException e) {
            throw new BadFileException(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }

        StringBuilder contentBody = new StringBuilder();
        contentBody.append("Content-Disposition:form-data; name=\"file\" " + "; filename=\"")
                .append(filename)
                .append("\"\r\n")
                .append("Content-Type:text/plain")
                .append("\r\n\r\n");
        try {
            out.write(contentBody.toString().getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }

        byte[] bufferOut = new byte[READ_BUF_SIZE];
        int totalCount = 0;
        try {
            while (totalCount < blockSize) {
                int bytes = dis.read(bufferOut);
                if (bytes <= 0) {
                    break;
                }
                out.write(bufferOut, 0, bytes);
                totalCount += bytes;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        contentBody = new StringBuilder();
        contentBody.append("\r\n")
                .append(sepBoundary)
                .append("\r\n")
                .append("\r\n")
                .append(endBoundary);
        try {
            out.write(contentBody.toString().getBytes("utf-8"));
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // read response
        String strLine;
        StringBuilder strResponse = new StringBuilder();
        try {
            InputStream response = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(response));
            while ((strLine = reader.readLine()) != null) {
                strResponse.append(strLine);
                strResponse.append("\n");
            }
            reader.close();
            response.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        connection.disconnect();

        Log.v(TAG, "uploadFileChunk strResponse = [" + strResponse.toString() + "]");

        JSONObject jsonobj;
        try {
            jsonobj = new JSONObject(strResponse.toString());
            if (!jsonobj.has("result") || !jsonobj.getBoolean("result")) {
                throw new BadResponseException(jsonobj.getString("error"));
            }
            jsonobj.put("eof", eof);
            jsonobj.put("bytes", totalCount);
        } catch (JSONException e) {
            throw new BadResponseException(strResponse.toString());
        }

        return jsonobj;
    }

    private static JSONObject uploadFileChunk(URL url,
                                             String path,
                                             int startOffset,
                                             int blockSize,
                                             Map<String, String> params,
                                             String storeAs,
                                             boolean eof) throws BadFileException, BadResponseException, NotReachException {
        //postparams.put("subpath", storeAs);
        //params.put("EOF_FILE", eof ? "1" : "0");

        return uploadFileChunk(url, path, startOffset, blockSize, params);
    }


    //public static final String SERVERS[] = {
    //        "http://172.21.3.240",
    //        "http://acs.leadcoretech.com"};

    public static final int FINISHED = 1;
    public static final int UNRECOVERABLE_ERROR = 2;
    public static final int RECOVERABLE_ERROR = 3;

    public static class UploadFile {

        private static final String TAG = UploadFile.class.getSimpleName();

        public String errString = "";
        public int startOffset = 0;
        private int fileIndex = 0;

        private int CHUNK_SIZE = 1024*1024;
        private String[] fileList;
        private String[] nameList;


        public UploadFile(String path, int offset) {
            fileList = new String[]{path};
            startOffset = offset;
        }

        public UploadFile(String[] files) {

            fileList = new String[files.length];
            nameList = new String[files.length];
            for (int i=0; i<files.length; i++) {
                fileList[i] = files[i];
                File f = new File(files[i]);
                nameList[i] = f.getName();
            }
        }


        protected int uploadPartial(Context context, String urlpath, Map<String, String> params) {
            Log.v(TAG, "uploadPartial urlpath = [" + urlpath + "]");
            if (fileList == null || fileList.length == 0 
                || fileIndex < 0 || fileIndex >= fileList.length) {
                // no file to upload
                return FINISHED;
            }

            URL useurl;
            try {
                useurl = new URL(Util.getUploadUrl(context));
            } catch (MalformedURLException e) {
                return UNRECOVERABLE_ERROR;
            }

            JSONObject jobj;

            try {
                long startTime = System.currentTimeMillis();

                if (nameList != null && nameList.length > 0) {
                    jobj = uploadFileChunk(useurl, fileList[fileIndex], startOffset, CHUNK_SIZE, params, nameList[fileIndex], fileIndex == fileList.length - 1);
                } else {
                    jobj = uploadFileChunk(useurl, fileList[fileIndex], startOffset, CHUNK_SIZE, params);
                }
                
                long endTime = System.currentTimeMillis();
                Log.v(TAG, "uploadPartial time = [" + (endTime - startTime) + "]");
                if ((endTime - startTime) < 5 * 1000) {
                    if (CHUNK_SIZE <= 1024*1024) {
                        CHUNK_SIZE = 1024*1024;
                    }
                } else if ((endTime - startTime) > 10 * 1000) {
                    if (CHUNK_SIZE > 1024*128) {
                        CHUNK_SIZE = 1024*128;
                    }
                }
            } catch (BadFileException e) {
                errString = e.getMessage();
                return UNRECOVERABLE_ERROR;
            } catch (BadResponseException e) {
                errString = e.getMessage();
                return RECOVERABLE_ERROR;
            } catch (NotReachException e) {
                errString = e.getMessage();
                return RECOVERABLE_ERROR;
            }

            boolean eof;
            int bytes;
            try {
                eof = jobj.getBoolean("eof");
                bytes = jobj.getInt("bytes");

            } catch (JSONException e) {
                errString = e.getMessage();
                return RECOVERABLE_ERROR;
            }

            startOffset += bytes;
            if (eof) {
                fileIndex++;
                if (fileIndex >= fileList.length) {
                    return FINISHED;
                }
            }
            return 0;
        }
    }

    public static String post(Context context, Map<String, String> params) throws IOException {
        try {
            String url = Util.getPostUrl(context);
            Log.v(TAG, "post url = [" + url + "]");
            URL useurl = new URL(url);
            return postRequest(useurl, params);
        } catch (MalformedURLException e) {
            throw new IOException(e.getMessage());
        }
    }

    public static String downloadFile(String furl, String path) {
        Log.v(TAG, "downloadFile furl [" + furl + "] path = [" + path + "]");
        String ret = "OK";
        URL url = null;  
        HttpURLConnection conn = null;  
        InputStream is = null;  
        BufferedInputStream bis = null;  
        FileOutputStream fos = null;  
        try {
            url = new URL(furl);
            conn = (HttpURLConnection)url.openConnection();  
            conn.setConnectTimeout(5000);  
            conn.setReadTimeout(3000);
            int httpResponseCode = conn.getResponseCode();
            Log.v(TAG, "downloadFile httpResponseCode = [" + httpResponseCode + "]");
            if (200 == httpResponseCode) {
                is = conn.getInputStream();  
                bis = new BufferedInputStream(is);   
                fos = new FileOutputStream(new File(path));  
                byte [] buffer = new byte[1024];  
                int len = 0;  
                int total = 0;
                while ((len = bis.read(buffer)) != -1) {  
                    fos.write(buffer, 0, len);  
                    total += len;
                }
                Log.v(TAG, "downloadFile total = [" + total + "]");
            } else {
                Log.e(TAG, "downloadFile responsecode invalid");
                ret = "ERROR";
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "downloadFile catch IOException = " + e.getMessage());
            ret = "FAILED";
        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.e(TAG, "downloadFile NullPointerException = " + e.getMessage());
            ret = "FAILED";
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
                
                if (bis != null) {
                    bis.close();
                }
                Log.v(TAG, "downloadFile download [" + furl + "] completed");
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "downloadFile finally IOException = " + e.getMessage());
                ret = "FAILED";
            }
        }
        Log.v(TAG, "downloadFile ret = [" + ret + "]");

        return ret;
    }

    public static ArrayList<Bundle> checkUpdate(Context context, String url) {
        Log.v(TAG, "checkUpdate url = [" + url + "]");
        HttpURLConnection conn = null;
        InputStreamReader isrd = null;
        ArrayList<Bundle> pkgs = new ArrayList<Bundle>();
        try {
            URL httpUrl = new URL(url);
            conn = (HttpURLConnection)httpUrl.openConnection();  
            conn.setConnectTimeout(5000);  
            conn.setReadTimeout(3000);
            int httpResponseCode = conn.getResponseCode();
            Log.v(TAG, "checkUpdate httpResponseCode = [" + httpResponseCode + "]");
            if (HttpURLConnection.HTTP_OK == httpResponseCode) {
                isrd = new InputStreamReader(conn.getInputStream());
            } else {
                Log.e(TAG, "checkUpdate responsecode invalid");
                pkgs = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "checkUpdate IOException = " + e.getMessage());
            pkgs = null;
        }

        if (pkgs != null) {
            JsonReader reader = null;
            try {
                reader = new JsonReader(isrd);
                reader.beginObject();
                while (reader.hasNext()) {
                    String val = reader.nextName();
                    if (val.equals("packages")) {
                        reader.beginArray();
                        while (reader.hasNext()) {
                            Bundle pkg = getPackage(context, reader);
                            if (pkg != null) {
                                pkgs.add(pkg);
                            }
                        }
                        reader.endArray();
                    } else {
                        reader.skipValue();
                    }
                }
                reader.endObject();
            } catch (NullPointerException e) {
                pkgs = null;
                e.printStackTrace();
                Log.e(TAG, "checkUpdate catch NullPointerException = " + e.getMessage());
            } catch (IOException e) {
                pkgs = null;
                e.printStackTrace();
                Log.e(TAG, "checkUpdate catch IOException = " + e.getMessage());
            } finally {
                try {
                    if (reader != null) {
                        reader.close(); 
                    }
                } catch (IOException e) {
                    pkgs = null;
                    e.printStackTrace();
                    Log.e(TAG, "checkUpdate finally IOException = " + e.getMessage());
                }
            }
        }

        return pkgs;
    }

    public static String checkCategory(Context context, String url) {
        Log.v(TAG, "checkCategory url = [" + url + "]");
        HttpURLConnection conn = null;
        InputStreamReader isrd = null;
        BufferedReader brd = null;
        String json = null;
        StringBuilder builder = new StringBuilder();
        try {
            URL httpUrl = new URL(url);
            conn = (HttpURLConnection)httpUrl.openConnection();  
            conn.setConnectTimeout(5000);  
            conn.setReadTimeout(3000);
            int httpResponseCode = conn.getResponseCode();
            Log.v(TAG, "checkCategory httpResponseCode = [" + httpResponseCode + "]");
            if (HttpURLConnection.HTTP_OK == httpResponseCode) {
                String temp = null;
                isrd = new InputStreamReader(conn.getInputStream());
                brd = new BufferedReader(isrd);
				while ((temp = brd.readLine()) != null) {
					builder.append(temp);
				}
				json = builder.toString();
            } else {
                Log.e(TAG, "checkCategory responsecode invalid");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "checkCategory IOException = " + e.getMessage());
        } finally {
            try {
                if (brd != null) {
                    brd.close(); 
                }
                if (isrd != null) {
                    isrd.close(); 
                }
                if (conn != null){
                    conn.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "checkCategory finally IOException = " + e.getMessage());
            }
        }
        Log.v(TAG, "checkCategory json = [" + json + "]");
        return json;
    }

    private static Bundle getPackage(Context context, JsonReader rd) {
        Log.v(TAG, "getPackage");
        Bundle pkg = new Bundle();
        try {
            rd.beginObject();
            while (rd.hasNext()) {
                String val = rd.nextName();
                if (val.equals("name")) {
                    pkg.putString("name", rd.nextString());
                } else if (val.equals("version")) {
                    pkg.putString("version", rd.nextString());
                } else if (val.equals("description")) {
                    pkg.putString("description", rd.nextString());
                } else if (val.equals("firmware")) {
                    pkg.putString("firmware", rd.nextString());
                } else
                    {
                    rd.skipValue();
                }
            }
            rd.endObject();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "getPackage IOException = " + e.getMessage());
        }

        String name = pkg.getString("name", "");
        String version = pkg.getString("version", "");
        String description = pkg.getString("descritpion","");
        String firmware = pkg.getString("firmware", "");
        Log.v(TAG, "getPackage name[" + name
                + "] version[" + version
                + "] descrition[" + description + "]"
                + "] firmware[" + firmware + "]");
        if (name.isEmpty() || version.isEmpty() || description.isEmpty() || firmware.isEmpty()) {
            Log.v(TAG, "getPackage invalid pkg");
            pkg = null;
        }
        
        return pkg;
    }
}

