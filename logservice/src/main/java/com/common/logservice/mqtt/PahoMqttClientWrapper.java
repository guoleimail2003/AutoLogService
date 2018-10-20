
package com.common.logservice.mqtt;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.app.Notification.Builder;
import android.util.Base64;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

public class PahoMqttClientWrapper {

    public static final String INTENT_LC_MSG_PEER = "com.leadcore.assistant.message.peer";
    public static final String INTENT_LC_MSG_GENERIC = "com.leadcore.assistant.message.generic";
    public static final String INTENT_LC_MSG_OTA = "com.leadcore.assistant.message.ota";
    public static final String INTENT_LC_MSG_CONTROL = "com.leadcore.assistant.message.control";
    public static final String INTENT_LC_MSG_CONFIG = "com.leadcore.assistant.message.config";

    public static class DefaultTopicListener implements MqttSubcriber.TopicListener {

        Context mContext;
        String mVersion = "unknown_version";

        DefaultTopicListener(Context context, String version) {
            mContext = context;
            mVersion = version;
            if (mVersion != null && !mVersion.isEmpty())
                mVersion = version;
        }

        public void deliveryComplete(IMqttDeliveryToken token) {

        }

        public boolean handleMessage(MqttSubcriber client, String topic, byte[] payload, int qos, boolean isRetained) {

            String data = new String(payload);

            if (topic.equals("ue/config/subscribes")) {

                // format
                // topic,qos,topic,qos
                // pattern {{u}}  clientid   {{v}} version
                String[] parts = data.split(",");

                List<String> ltopics = new ArrayList<>();
                List<Integer> lqoss = new ArrayList<>();

                String t = null;
                for (int i=0; i<parts.length; i++) {
                    String v = parts[i];
                    v = v.trim();
                    if (t == null) {
                        t = v;
                        t = t.replace("{{u}}", client.getClientID());
                        t = t.replace("{{v}}", mVersion);
                    }
                    else {
                        int q;
                        try {
                            q = Integer.parseInt(v);
                        }catch (NumberFormatException e) {
                            q = 0;
                            i--;
                        }

                        ltopics.add(t);
                        lqoss.add(q);
                        t = null;
                    }
                }

                if (t != null) {
                    ltopics.add(t);
                    lqoss.add(0);
                }

                int size = ltopics.size();
                final String[] topics = new String[size];
                int[] qoss = new int[size];
                ltopics.toArray(topics);
                for (int i=0; i<size; i++)
                    qoss[i] = lqoss.get(i);

                client.subscribe(topics, qoss, new IMqttActionListener() {
                    @Override
                    public void onFailure(IMqttToken token, Throwable exception) {
                        Log.e("MQTT", "subscribe to "+ Arrays.toString(topics) +" failed");
                    }

                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                    }
                });
                return true;
            }

            if (topic.startsWith("ue/")) {
                String subtopic = topic.substring(3);
                if (subtopic.startsWith("config/")) {
                    String tag = subtopic.substring(7);
                    Intent intent = new Intent(INTENT_LC_MSG_CONFIG);
                    intent.putExtra("tag", tag);
                    intent.putExtra("data", data);
                    mContext.sendBroadcast(intent);
                }
                else if (subtopic.startsWith("control/")) {
                    String tag = subtopic.substring(8);
                    Intent intent = new Intent(INTENT_LC_MSG_CONTROL);
                    intent.putExtra("tag", tag);
                    intent.putExtra("data", data);
                    mContext.sendBroadcast(intent);
                }
                else if (subtopic.startsWith("ota/")) {
                    String tag = subtopic.substring(4);
                    Intent intent = new Intent(INTENT_LC_MSG_OTA);
                    intent.putExtra("tag", tag);
                    intent.putExtra("data", data);
                    mContext.sendBroadcast(intent);
                }
                else if (subtopic.equals("demo")) {
                    notifcation(mContext, data);
                }
                else {
                    Intent intent = new Intent(INTENT_LC_MSG_GENERIC);
                    intent.putExtra("tag", subtopic);
                    intent.putExtra("data", data);
                    mContext.sendBroadcast(intent);
                }
                return true;
            }

            String clientFea = client.getClientID() + "/";
            int pos = topic.indexOf(clientFea);
            if (pos >= 0) {
                String tag = topic.substring(clientFea.length()+pos);

                Intent intent = new Intent(INTENT_LC_MSG_PEER);
                intent.putExtra("tag", tag);
                intent.putExtra("data", data);
                mContext.sendBroadcast(intent);

                if (tag.equals("$notify")) {
                    notifcation(mContext, data);
                }

                return true;
            }

            return false;
        }

        public void onConnected(MqttSubcriber client) {

            client.subscribe("ue/config/subscribes", 2, new IMqttActionListener() {
                @Override
                public void onFailure(IMqttToken token, Throwable exception) {
                    Log.e("MQTT", "initial subscribe failed");
                }

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                }
            });
        }

        public void onLostConnection() {
        }

    }


    static String hexDigitsStr = "0123456789abcdefABCDEF";
    private static int hexStringToByte(int h, int l) {
        int i = hexDigitsStr.indexOf(h);
        h = (i > 15) ? (i-6) : i;
        i = hexDigitsStr.indexOf(l);
        l = (i > 15) ? (i-6) : i;
        return ((h<<4)|l);
    }

    private static String generateKey(String uuid) {

        try {
            byte[] data = new byte[uuid.length() / 2];
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) hexStringToByte(uuid.charAt(i * 2), uuid.charAt(i * 2 + 1));
            }

            CRC32 crc32 = new CRC32();
            crc32.update(data);
            long crc = crc32.getValue();
            byte[] cv = new byte[]{(byte) (crc >> 24), (byte) (crc >> 16), (byte) (crc >> 8), (byte) (crc)};

            byte[] iv = new byte[8];
            new SecureRandom().nextBytes(iv);

            IvParameterSpec IV = new IvParameterSpec(iv);
            DESKeySpec desKey = new DESKeySpec("YC&14_!E".getBytes());
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey securekey = keyFactory.generateSecret(desKey);
            Cipher cipher = Cipher.getInstance("DES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, securekey, IV);
            byte[] result = cipher.doFinal(data);
            String base64code = Base64.encodeToString(result, Base64.NO_PADDING | Base64.NO_WRAP);
            String iv64 = Base64.encodeToString(iv, Base64.NO_PADDING | Base64.NO_WRAP);
            return iv64 + base64code + Base64.encodeToString(cv, Base64.NO_PADDING | Base64.NO_WRAP);
        }
        catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static MqttSubcriber createSubscriber(Context context, String uuid, String version) {
        MqttSubcriber sub = new MqttSubcriber(context, uuid);
        String key = generateKey(uuid);
        Log.d("MQTT", "login key:" + key);
        sub.setUser("lc_ue", key);
        sub.setTopicListener(new PahoMqttClientWrapper.DefaultTopicListener(context, version));
        sub.start();
        return sub;
    }


        /** Message ID Counter **/
        private static int MessageID = 0;

        /**
         * Displays a notification in the notification area of the UI
         * @param context Context from which to create the notification
         * @param messageString The string to display to the user as a message
         */
        static void notifcation(Context context, String messageString) {

            //Get the notification manage which we will use to display the notification
            String ns = Context.NOTIFICATION_SERVICE;
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);

            long when = System.currentTimeMillis();

            //the message that will be displayed as the ticker
            String ticker = messageString;

            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setPackage("com.leadcore.assistant3");

            //build the pending intent that will start the appropriate activity
            PendingIntent pendingIntent = PendingIntent.getActivity(context,
                    0, intent, 0);

            //build the notification
            Builder notificationCompat = new Builder(context);
            notificationCompat.setAutoCancel(true)
                    .setContentTitle("Leadcore Notification")
                    .setContentIntent(pendingIntent)
                    .setContentText(messageString)
                    .setTicker(ticker)
                    .setWhen(when)
                    .setDefaults(Notification.DEFAULT_ALL | Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm);

            Notification notification = notificationCompat.build();
            //display the notification
            mNotificationManager.notify(MessageID, notification);
            MessageID++;

        }
}
