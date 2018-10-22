
package com.common.logservice.mqtt;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PowerManager;
import android.util.Log;
import android.util.SparseArray;


import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttPingSender;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by zhouruijian on 2016/6/13.
 *
 */
public class MqttSubcriber implements MqttCallback {

    static final String TAG = "MqttService";

    static final boolean useSSL = false;
    static final int defaultPort = 1883;
    static final String[] defaultHosts = new String[] {

            "acs.leadcoretech.com",
            "172.21.3.240",
    };

    /** Default timeout*/
    static final int defaultTimeOut = 20;
    /** Default keep alive value*/
    static final int defaultKeepAlive = 1200;

    static final boolean defaultCleanSession = true;


    static final String MESSAGE_ID = "messageId";
    static final String DESTINATION_NAME = "destinationName";

    static final String CALLBACK_STATUS = TAG + ".callbackStatus";
    static final String CALLBACK_MESSAGE_ID = TAG + '.' + MESSAGE_ID;
    static final String CALLBACK_DESTINATION_NAME = TAG + '.' + DESTINATION_NAME;
    static final String CALLBACK_MESSAGE_PARCEL = TAG + ".PARCEL";
    static final String CALLBACK_ERROR_MESSAGE = TAG + ".errorMessage";
    static final String CALLBACK_EXCEPTION_STACK = TAG + ".exceptionStack";
    static final String CALLBACK_EXCEPTION = TAG + ".exception";
    static final String CALLBACK_ACTIVITY_TOKEN = TAG + "." + "activityToken";

    private static final String NOT_CONNECTED = "not connected";

    static final String PING_SENDER = TAG + ".pingSender.";
    static final String PING_WAKELOCK = TAG + ".client.";

    static final int MSG_CONNECTION_LOST = 100;
    static final int MESSAGE_DELIVERED = 101;
    static final int MSG_MESSAGE_ARRIVED = 102;
    static final int MSG_CONNECT = 103;
    static final int MSG_SUBSCRIBE = 104;
    static final int MSG_DISCONNECT = 105;
    static final int MSG_UNSUBSCRIBE = 106;
    static final int MSG_PUBLSH = 107;



    // somewhere to persist received messages until we're sure
    // that they've reached the application
    private Map<IMqttDeliveryToken, String /* Topic */> savedTopics = new HashMap<IMqttDeliveryToken, String>();
    private Map<IMqttDeliveryToken, MqttMessage> savedSentMessages = new HashMap<IMqttDeliveryToken, MqttMessage>();
    private Map<IMqttDeliveryToken, Integer> savedActivityTokens = new HashMap<>();
    MessageStore messageStore;
    private MqttConnectOptions connectOptions;
    private static ExecutorService pool = Executors.newCachedThreadPool();

    private MqttAsyncClient myClient = null;
    private final String clientId;
    private String mUsername;
    private String mPassword;
    private volatile boolean disconnected = true;
    private volatile boolean isConnecting = false;
    private MqttClientPersistence persistence = null;

    private PowerManager.WakeLock wakelock = null;
    private String wakeLockTag = null;
    private NetworkConnectionIntentReceiver networkConnectionMonitor;
    private Context mContext;


    enum Status {
        /**
         * Indicates that the operation succeeded
         */
        OK,

        /**
         * Indicates that the operation failed
         */
        ERROR,

        /**
         * Indicates that the operation's result may be returned asynchronously
         */
        NO_RESULT
    }

    public interface TopicListener {
        void onConnected(MqttSubcriber client);
        void onLostConnection();
        boolean handleMessage(MqttSubcriber client, String topic, byte[] payload, int qos, boolean isRetained);
        void deliveryComplete(IMqttDeliveryToken token);
    }


    TopicListener topicListener;

    private Handler handler;

    private static class ActionHandler extends Handler {

        MqttSubcriber service;
        ActionHandler(MqttSubcriber svr) {
            super();
            service = svr;
        }
        @Override
        public void handleMessage(Message msg) {
            service.handleActionResult(msg.what, msg.getData());
        }
    }

    private void handleActionResult(int msgId, Bundle data) {
        switch (msgId) {
            case MSG_CONNECTION_LOST: {

                if (topicListener != null) {
                    topicListener.onLostConnection();
                }
                //Exception reason = (Exception) data.getSerializable(CALLBACK_EXCEPTION);
                //if (reason != null) {
                //  reconnect();
                //}
            }
            break;

            case MSG_MESSAGE_ARRIVED:
            {
                String messageId = data.getString(CALLBACK_MESSAGE_ID);
                String destinationName = data.getString(CALLBACK_DESTINATION_NAME);
                ParcelableMqttMessage message = data.getParcelable(CALLBACK_MESSAGE_PARCEL);
                if (topicListener != null && message != null)
                    topicListener.handleMessage(this, destinationName, message.getPayload(), message.getQos(), message.isRetained());
                acknowledgeMessageArrival(messageId);
            }
            break;

            case MSG_CONNECT: {
                IMqttToken token = connectToken;
                removeMqttToken(data);
                simpleAction(token, data);
            }
            break;

            case MSG_DISCONNECT : {
                IMqttToken token = removeMqttToken(data);
                if (token != null) {
                    ((MqttTokenAndroid) token).notifyComplete();
                }
                if (topicListener != null) {
                    topicListener.onLostConnection();
                }
            }
            break;

            case MSG_SUBSCRIBE:
            case MSG_UNSUBSCRIBE:
            {
                IMqttToken token = removeMqttToken(data);
                simpleAction(token, data);
            }
            break;

            case MSG_PUBLSH: {
                IMqttToken token = getMqttToken(data);
                simpleAction(token, data);
                break;
            }

            case MESSAGE_DELIVERED: {
                IMqttToken token = removeMqttToken(data);
                if (token != null) {
                    if (topicListener != null) {
                        Status status = (Status) data.getSerializable(CALLBACK_STATUS);
                        if (status == Status.OK) {
                            topicListener.deliveryComplete((IMqttDeliveryToken) token);
                        }
                    }
                }
            }
            break;
        }
    }

    private Status acknowledgeMessageArrival(String id) {
        if (messageStore.discardArrived(id)) {
            return Status.OK;
        }
        else {
            return Status.ERROR;
        }
    }

    private void simpleAction(IMqttToken token, Bundle data) {
        if (token != null) {
            Status status = (Status) data.getSerializable(CALLBACK_STATUS);
            if (status == Status.OK) {
                ((MqttTokenAndroid) token).notifyComplete();
            }
            else {
                Exception exceptionThrown = (Exception) data.getSerializable(CALLBACK_EXCEPTION);
                ((MqttTokenAndroid) token).notifyFailure(exceptionThrown);
            }
        } else {
            Log.e(TAG, "simpleAction : token is null");
        }
    }

    private int reconnectActivityToken = -1;
    private IMqttToken connectToken;
    private SparseArray<IMqttToken> tokenMap = new SparseArray<>();
    private int tokenNumber = 0;

    private synchronized int storeToken(IMqttToken token) {
        tokenMap.put(tokenNumber, token);
        return tokenNumber++;
    }

    private synchronized IMqttToken removeMqttToken(Bundle data) {

        int activityToken = data.getInt(CALLBACK_ACTIVITY_TOKEN, -1);
        if (activityToken >= 0){
            IMqttToken token = tokenMap.get(activityToken);
            tokenMap.delete(activityToken);
            return token;
        }
        return null;
    }

    private synchronized IMqttToken getMqttToken(Bundle data) {
        int activityToken = data.getInt(CALLBACK_ACTIVITY_TOKEN);
        IMqttToken token = tokenMap.get(activityToken);
        return token;
    }

    String getServerURI() {
        return getDefaultUris()[0];
    }

    synchronized void reconnect() {
        if (isConnecting) {
            Log.d(TAG, "The client is connecting. Reconnect return directly.");
            return ;
        }

        if(!isOnline()){
            Log.d(TAG, "The network is not reachable. Will not do reconnect");
            return;
        }

        if (myClient != null && disconnected) {
            // use the activityToke the same with action connect
            Log.d(TAG, "Do Real Reconnect!");
            final Bundle resultBundle = createConnectBundle();
            try {
                myClient.connect(connectOptions, null, new MqttConnectActionListener(resultBundle));
                setConnectingState(true);
            } catch (MqttException e) {
                Log.e(TAG, Log.getStackTraceString(e));
                Log.e(TAG, "Cannot reconnect to remote server." + e.getMessage());
                setConnectingState(false);
                handleException(MSG_CONNECT, resultBundle, e);
            }

        }
    }

    public void connect(MqttConnectOptions options,
                        IMqttActionListener callback) throws MqttException {
        IMqttToken token = new MqttTokenAndroid(callback, null);
        connectOptions = options;
        connectToken = token;

        pool.execute(new Runnable() {

            @Override
            public void run() {
                doConnect();
            }
        });
    }

    private void doConnect() {
        reconnectActivityToken = storeToken(connectToken);

        Log.d(TAG, "Connecting {" + getServerURI() + "} as {" + clientId + "}");
        final Bundle resultBundle = createConnectBundle();
        try {
            if (persistence == null) {
                // ask Android where we can put files
                File myDir = mContext.getExternalFilesDir(TAG);

                if (myDir == null) {
                    // No external storage, use internal storage instead.
                    myDir = mContext.getDir(TAG, Context.MODE_PRIVATE);

                    if(myDir == null){
                        //Shouldn't happen.
                        resultBundle.putString(CALLBACK_ERROR_MESSAGE,
                                "Error! No external and internal storage available");
                        resultBundle.putSerializable(CALLBACK_EXCEPTION, new MqttPersistenceException());
                        callbackToActivity(MSG_CONNECT, Status.ERROR,
                                resultBundle);
                        return;
                    }
                }

                // use that to setup MQTT client persistence storage
                persistence = new MqttDefaultFilePersistence(
                        myDir.getAbsolutePath());
            }

            if (myClient == null) {
                myClient = new MqttAsyncClient(getServerURI(), clientId, persistence, new AlarmPingSender());
                myClient.setCallback(this);
            }

            if (isConnecting ) {
                Log.d(TAG, "Connect return:isConnecting:" + isConnecting + ".disconnected:" + disconnected);
            }else if(!disconnected){
                Log.d(TAG, "myClient != null and the client is connected and notify!");
                doAfterConnectSuccess(resultBundle);
            }else {
                if (isOnline()) {
                    Log.d(TAG, "Do Real connect!");
                    setConnectingState(true);
                    myClient.connect(connectOptions, null, new MqttConnectActionListener(resultBundle));
                }
            }
        } catch (Exception e) {
            handleException(MSG_CONNECT, resultBundle, e);
        }
    }

    public void disconnect(IMqttActionListener callback) throws MqttException {
        disconnect(callback, -1);
    }

    public void disconnect(IMqttActionListener callback, long quiesceTimeout) throws MqttException {
        Log.d(TAG, "disconnect()");
        disconnected = true;
        final Bundle resultBundle = createResultBundle(MSG_DISCONNECT, callback);
        if (resultBundle != null) {
            IMqttActionListener listener = new MqttConnectionListener(MSG_DISCONNECT, resultBundle);
            try {
                if (quiesceTimeout >= 0)
                    myClient.disconnect(quiesceTimeout, null, listener);
                else
                    myClient.disconnect(null, listener);
            } catch (Exception e) {
                handleException(MSG_DISCONNECT, resultBundle, e);
            }
        }

        releaseWakeLock();
        myClient = null;
    }

    public void subscribe(String topic, int qos, IMqttActionListener callback) {
        subscribe(new String[]{topic}, new int[]{qos}, callback);
    }

    public void subscribe(String[] topic, int[] qos,
                          IMqttActionListener callback)  {

        Log.d(TAG, "subscribe({" + Arrays.toString(topic) + "}," + Arrays.toString(qos));

        final Bundle resultBundle = createResultBundle(MSG_SUBSCRIBE, callback, topic);
        if (resultBundle != null) {
            IMqttActionListener listener = new MqttConnectionListener(MSG_SUBSCRIBE, resultBundle);
            try {
                myClient.subscribe(topic, qos, null, listener);
            } catch (Exception e) {
                handleException(MSG_SUBSCRIBE, resultBundle, e);
            }
        }
    }

    public void unsubscribe(String topic,
                            IMqttActionListener callback) {
        unsubscribe(new String[]{topic}, callback);
    }

    public void unsubscribe(String[] topic,
                            IMqttActionListener callback) {

        Log.d(TAG, "unsubscribe({" + Arrays.toString(topic) + "}");

        final Bundle resultBundle = createResultBundle(MSG_UNSUBSCRIBE, callback);
        if (resultBundle != null) {
            IMqttActionListener listener = new MqttConnectionListener(MSG_UNSUBSCRIBE, resultBundle);
            try {
                myClient.unsubscribe(topic, null, listener);
            } catch (Exception e) {
                handleException(MSG_UNSUBSCRIBE, resultBundle, e);
            }
        }
    }

    public void publish(String topic, byte[] payload, int qos,
                        boolean retained, IMqttActionListener callback) {
        MqttMessage message = new MqttMessage(payload);
        message.setQos(qos);
        message.setRetained(retained);
        MqttDeliveryTokenAndroid token = new MqttDeliveryTokenAndroid(callback, message);
        int activityToken = storeToken(token);
        final Bundle resultBundle = initResultBundle(MSG_PUBLSH, activityToken);
        if (resultBundle != null) {
            IMqttActionListener listener = new MqttConnectionListener(MSG_PUBLSH, resultBundle);
            try {
                IMqttDeliveryToken internalToken = myClient.publish(topic, payload, qos, retained, null, listener);
                savedTopics.put(internalToken, topic);
                savedSentMessages.put(internalToken, message);
                savedActivityTokens.put(internalToken, activityToken);
                token.setDelegate(internalToken);
            } catch (Exception e) {
                handleException(MSG_PUBLSH, resultBundle, e);
            }
        }
    }

    public String getClientID() {
        return clientId;
    }

    public void setUser(String username, String password) {
        mUsername = username;
        mPassword = password;
    }

    public void setTopicListener(TopicListener listener) {
        topicListener = listener;
    }

    public void start() {

        registerBroadcastReceivers();

        //connect options strings
        String username = mUsername;
        String password = mPassword;

        //rebuild objects starting with the connect options
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setCleanSession(defaultCleanSession);
        opts.setKeepAliveInterval(defaultKeepAlive);
        opts.setConnectionTimeout(defaultTimeOut);

        opts.setWill("client/" + clientId + "/status", "off".getBytes(), 0, true);

        opts.setPassword(password != null ? password.toCharArray() : null);
        opts.setUserName(username);

        opts.setServerURIs(getDefaultUris());

        try {
            connect(opts, new IMqttActionListener() {
                @Override
                public void onFailure(IMqttToken token, Throwable exception) {
                    Log.e(TAG, "Client failed to connect host ");
                }

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                    publish("client/" + clientId + "/status", "on".getBytes(), 0, true, null);

                    if (topicListener != null)
                        topicListener.onConnected(MqttSubcriber.this);
                }
            });
        }catch (MqttException e) {
            Log.e(TAG, "Failed to reconnect the client ", e);
        }
    }

    public MqttSubcriber(Context context, String clientId) {
        this.mContext = context;
        this.clientId = clientId;
        handler = new ActionHandler(this);
        wakeLockTag = this.getClass().getCanonicalName() + " on host " + getServerURI();
        messageStore = new MessageStore();
    }

    private String[] getDefaultUris() {
        String[] networks = new String[defaultHosts.length];
        for (int i= 0; i<defaultHosts.length; i++) {
            if (useSSL)
                networks[i] = "ssl://" + defaultHosts[i] + ":" + defaultPort;
            else
                networks[i] = "tcp://" + defaultHosts[i] + ":" + defaultPort;
        }
        return networks;
    }


    public void stop() {

        try {
            disconnect(null);
        }
        catch(MqttException e) {
            // do nothing
        }

        unregisterBroadcastReceivers();

        if (messageStore !=null )
            messageStore.close();
    }

    private void registerBroadcastReceivers() {
        if (networkConnectionMonitor == null) {
            networkConnectionMonitor = new NetworkConnectionIntentReceiver();
            mContext.registerReceiver(networkConnectionMonitor, new IntentFilter(
                    ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    private void unregisterBroadcastReceivers(){
        if(networkConnectionMonitor != null){
            mContext.unregisterReceiver(networkConnectionMonitor);
            networkConnectionMonitor = null;
        }
    }

    /*
   * Called in response to a change in network connection - after losing a
   * connection to the server, this allows us to wait until we have a usable
   * data connection again
   */
    private class NetworkConnectionIntentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Internal network status receive.");
            // we protect against the phone switching off
            // by requesting a wake lock - we request the minimum possible wake
            // lock - just enough to keep the CPU running until we've finished
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm
                    .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT");
            wl.acquire();
            Log.d(TAG, "Reconnect for Network recovery.");
            if (isOnline()) {
                Log.d(TAG, "Online,reconnect.");
                // we have an internet connection - have another try at
                // connecting
                reconnect();
            } else {
                notifyClientsOffline();
            }

            wl.release();
        }
    }

    public void notifyClientsOffline() {
        if (!disconnected) {
            connectionLost(null);
        }
    }

    boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        Log.v(TAG, "isOnline info = [" + info + "]");
        return (info != null && info.isAvailable() && info.isConnected());
        //return (cm.getActiveNetworkInfo() != null
        //        && cm.getActiveNetworkInfo().isAvailable()
        //        && cm.getActiveNetworkInfo().isConnected());
    }

    private void doAfterConnectSuccess(final Bundle resultBundle) {
        //since the device's cpu can go to sleep, acquire a wakelock and drop it later.
        acquireWakeLock();
        callbackToActivity(MSG_CONNECT, Status.OK, resultBundle);
        deliverBacklog();
        setConnectingState(false);
        disconnected = false;
        releaseWakeLock();
    }

    private void doAfterConnectFail(final Bundle resultBundle){
        //
        acquireWakeLock();
        disconnected = true;
        setConnectingState(false);
        callbackToActivity(MSG_CONNECT, Status.ERROR, resultBundle);
        releaseWakeLock();
    }

    /**
     * Attempt to deliver any outstanding messages we've received but which the
     * application hasn't acknowledged. If "cleanSession" was specified, we'll
     * have already purged any such messages from our messageStore.
     */
    private void deliverBacklog() {
        Iterator<StoredMessage> backlog = messageStore.getAllArrivedMessages();
        while (backlog.hasNext()) {
            StoredMessage msgArrived = backlog.next();
            Bundle resultBundle = messageToBundle(msgArrived.messageId, msgArrived.topic, msgArrived.message);
            callbackToActivity(MSG_MESSAGE_ARRIVED, Status.OK, resultBundle);
        }
    }


    private Bundle messageToBundle(String messageId, String topic,
                                   MqttMessage message) {
        Bundle result = new Bundle();
        result.putString(CALLBACK_MESSAGE_ID, messageId);
        result.putString(CALLBACK_DESTINATION_NAME, topic);
        result.putParcelable(CALLBACK_MESSAGE_PARCEL, new ParcelableMqttMessage(message));
        return result;
    }

    private void handleException(int msgId, final Bundle resultBundle, Exception e) {
        resultBundle.putString(CALLBACK_ERROR_MESSAGE,
                e.getLocalizedMessage());

        resultBundle.putSerializable(CALLBACK_EXCEPTION, e);

        callbackToActivity(msgId, Status.ERROR, resultBundle);
    }

    synchronized void setConnectingState(boolean isConnecting){
        this.isConnecting = isConnecting;
    }

    void callbackToActivity(int msgId, Status status,
                            Bundle dataBundle) {
        // Don't call traceDebug, as it will try to callbackToActivity leading
        // to recursion.
        if (dataBundle != null) {
            dataBundle.putSerializable(CALLBACK_STATUS, status);
        }
        Message msg = handler.obtainMessage(msgId);
        msg.setData(dataBundle);
        handler.sendMessage(msg);
    }

    private void acquireWakeLock() {
        if (wakelock == null) {
            PowerManager pm = (PowerManager) mContext.getSystemService(Service.POWER_SERVICE);
            wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag);
        }
        wakelock.acquire();
    }

    private void releaseWakeLock() {
        if(wakelock != null && wakelock.isHeld()){
            wakelock.release();
        }
    }

    Bundle createConnectBundle() {
        Bundle resultBundle = new Bundle();
        resultBundle.putInt(CALLBACK_ACTIVITY_TOKEN, reconnectActivityToken);
        return resultBundle;
    }

    Bundle createResultBundle(IMqttActionListener listener, MqttMessage message) {
        MqttDeliveryTokenAndroid token = new MqttDeliveryTokenAndroid(listener, message);
        int activityToken = storeToken(token);
        return initResultBundle(MSG_PUBLSH, activityToken);
    }

    Bundle createResultBundle(int action, IMqttActionListener listener) {
        return createResultBundle(action, listener, null);
    }

    Bundle createResultBundle(int action, IMqttActionListener listener, String[] topics) {
        IMqttToken token = new MqttTokenAndroid(listener, topics);
        int activityToken = storeToken(token);
        return initResultBundle(action, activityToken);
    }

    Bundle initResultBundle(int action, int activityToken) {
        Bundle resultBundle = new Bundle();
        resultBundle.putInt(CALLBACK_ACTIVITY_TOKEN, activityToken);

        if ((myClient == null) || (!myClient.isConnected())) {
            resultBundle.putString(CALLBACK_ERROR_MESSAGE, NOT_CONNECTED);
            Log.e(TAG, "action [" + Integer.toString(action) + " failed because not connected");
            callbackToActivity(action, Status.ERROR, resultBundle);
            return null;
        }

        return resultBundle;
    }

    static class MqttTokenAndroid implements IMqttToken {

        private IMqttActionListener listener;

        private volatile boolean isComplete;

        private volatile MqttException lastException;

        private final Object waitObject = new Object();

        private String[] topics;

        private MqttException pendingException;

        private IMqttToken delegate; // specifically for getMessageId

        MqttTokenAndroid(IMqttActionListener listener, String[] topics) {
            this.listener = listener;
            this.topics = topics;
        }

        void setDelegate(IMqttToken delegate) {
            this.delegate = delegate;
        }

        void notifyComplete() {
            synchronized (waitObject) {
                isComplete = true;
                waitObject.notifyAll();
                if (listener != null) {
                    listener.onSuccess(this);
                }
            }
        }

        /**
         * notify unsuccessful completion of the operation
         */
        void notifyFailure(Throwable exception) {
            synchronized (waitObject) {
                isComplete = true;
                if (exception instanceof MqttException) {
                    pendingException = (MqttException) exception;
                }
                else {
                    pendingException = new MqttException(exception);
                }
                waitObject.notifyAll();
                if (exception instanceof MqttException) {
                    lastException = (MqttException) exception;
                }
                if (listener != null) {
                    listener.onFailure(this, exception);
                }
            }

        }

        @Override
        public MqttException getException() {
            return lastException;
        }

        @Override
        public int[] getGrantedQos() {
            return delegate.getGrantedQos();
        }

        @Override
        public void waitForCompletion() throws MqttException {
            synchronized (waitObject) {
                try {
                    waitObject.wait();
                }
                catch (InterruptedException e) {
                    // do nothing
                }
            }
            if (pendingException != null) {
                throw pendingException;
            }
        }

        @Override
        public IMqttAsyncClient getClient() {
            return null;
        }

        @Override
        public void waitForCompletion(long timeout) throws MqttException {
            synchronized (waitObject) {
                try {
                    waitObject.wait(timeout);
                }
                catch (InterruptedException e) {
                    // do nothing
                }
                if (!isComplete) {
                    throw new MqttException(MqttException.REASON_CODE_CLIENT_TIMEOUT);
                }
                if (pendingException != null) {
                    throw pendingException;
                }
            }
        }

        @Override
        public void setActionCallback(IMqttActionListener listener) {
            this.listener = listener;
        }

        @Override
        public IMqttActionListener getActionCallback() {
            return listener;
        }

        @Override
        public String[] getTopics() {
            return topics;
        }

        @Override
        public int getMessageId() {
            return (delegate != null) ? delegate.getMessageId() : 0;
        }

        @Override
        public MqttWireMessage getResponse() {
            return delegate.getResponse();
        }

        @Override
        public boolean isComplete() {
            return isComplete;
        }

        @Override
        public void setUserContext(Object userContext) {
        }

        @Override
        public Object getUserContext() {
            return null;
        }

        @Override
        public boolean getSessionPresent() {
            return delegate.getSessionPresent();
        }
    }

    static class MqttDeliveryTokenAndroid extends MqttTokenAndroid
            implements IMqttDeliveryToken {

        // The message which is being tracked by this token
        private MqttMessage message;

        MqttDeliveryTokenAndroid(IMqttActionListener listener, MqttMessage message) {
            super(listener, null);
            this.message = message;
        }

        /**
         * @see org.eclipse.paho.client.mqttv3.IMqttDeliveryToken#getMessage()
         */
        @Override
        public MqttMessage getMessage() throws MqttException {
            return message;
        }
    }

    @Override
    public void connectionLost(Throwable why) {
        
        if (why != null) {
			why.printStackTrace();
			Log.v(TAG, "connectionLost(" + why.getMessage() + ")");
		}
        disconnected = true;
        try {
            myClient.disconnect(null, new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // No action
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    // No action
                }
            });
        } catch (Exception e) {
            // ignore it - we've done our best
        }

        Bundle resultBundle = new Bundle();
        if (why != null) {
            resultBundle.putString(CALLBACK_ERROR_MESSAGE, why.getMessage());
            if (why instanceof MqttException) {
                resultBundle.putSerializable(CALLBACK_EXCEPTION, why);
            }
            resultBundle.putString(CALLBACK_EXCEPTION_STACK, Log.getStackTraceString(why));
        }
        callbackToActivity(MSG_CONNECTION_LOST, Status.OK, resultBundle);
        // client has lost connection no need for wake lock
        releaseWakeLock();

        if (why != null) {
            //reconnect();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken messageToken) {

        Log.d(TAG, "deliveryComplete(" + messageToken + ")");
        MqttMessage message = savedSentMessages.remove(messageToken);
        if (message != null) {
            // irrelevant
            String topic = savedTopics.remove(messageToken);
            Integer activityToken = savedActivityTokens.remove(messageToken);
            Bundle resultBundle = messageToBundle(null, topic, message);
            resultBundle.putInt(CALLBACK_ACTIVITY_TOKEN, activityToken);
            callbackToActivity(MESSAGE_DELIVERED, Status.OK, resultBundle);
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message)
            throws Exception {
        Log.d(TAG, "messageArrived(" + topic + ",{" + message.toString() + "})");

        String messageId = messageStore.storeArrived(topic, message);
        Bundle resultBundle = messageToBundle(messageId, topic, message);
        resultBundle.putString(CALLBACK_MESSAGE_ID, messageId);
        callbackToActivity(MSG_MESSAGE_ARRIVED, Status.OK, resultBundle);
    }


    private class MqttConnectActionListener implements IMqttActionListener {

        private final Bundle resultBundle;

        private MqttConnectActionListener(Bundle resultBundle) {
            this.resultBundle = resultBundle;
        }

        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            Log.d(TAG, "connect success!");
            Log.d(TAG, "DeliverBacklog when reconnect.");
            doAfterConnectSuccess(resultBundle);
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken,
                              Throwable exception) {
            resultBundle.putString(CALLBACK_ERROR_MESSAGE, exception.getLocalizedMessage());
            resultBundle.putSerializable(CALLBACK_EXCEPTION, exception);
            //exception.printStackTrace();
            Log.e(TAG, "connect fail, call connect to reconnect.reason:"
                    + exception.getMessage());

            doAfterConnectFail(resultBundle);
        }
    }

    private class MqttConnectionListener implements IMqttActionListener {

        private final int msgId;
        private final Bundle resultBundle;

        private MqttConnectionListener(int msgid, Bundle resultBundle) {
            msgId = msgid;
            this.resultBundle = resultBundle;
        }

        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            callbackToActivity(msgId, Status.OK, resultBundle);
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            resultBundle.putString(CALLBACK_ERROR_MESSAGE, exception.getLocalizedMessage());

            resultBundle.putSerializable(CALLBACK_EXCEPTION, exception);
            callbackToActivity(msgId, Status.ERROR, resultBundle);
        }
    }

    class AlarmPingSender implements MqttPingSender {

        private ClientComms comms;
        private BroadcastReceiver alarmReceiver;
        private PendingIntent pendingIntent;
        private volatile boolean hasStarted = false;

        public AlarmPingSender() {
        }

        @Override
        public void init(ClientComms comms) {
            this.comms = comms;
            this.alarmReceiver = new AlarmReceiver();
        }

        @Override
        public void start() {
            String action = PING_SENDER  + clientId;
            Log.d(TAG, "Register alarmreceiver to MqttService"+ action);
            mContext.registerReceiver(alarmReceiver, new IntentFilter(action));

            pendingIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(
                    action), PendingIntent.FLAG_UPDATE_CURRENT);

            schedule(comms.getKeepAlive());
            hasStarted = true;
        }

        @Override
        public void stop() {
            // Cancel Alarm.
            AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Service.ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);

            Log.d(TAG, "Unregister alarmreceiver to "+ clientId);
            if(hasStarted){
                hasStarted = false;
                try{
                    mContext.unregisterReceiver(alarmReceiver);
                }catch(IllegalArgumentException e){
                    //Ignore unregister errors.
                }
            }
        }

        @Override
        public void schedule(long delayInMilliseconds) {
            long nextAlarmInMilliseconds = System.currentTimeMillis()
                    + delayInMilliseconds;
            Log.d(TAG, "Schedule next alarm at " + nextAlarmInMilliseconds);
            AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Service.ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds,
                    pendingIntent);
        }

        /*
         * This class sends PingReq packet to MQTT broker
         */
        class AlarmReceiver extends BroadcastReceiver {
            private PowerManager.WakeLock wakelock;
            private String wakeLockTag = PING_WAKELOCK  + clientId;

            @Override
            public void onReceive(Context context, Intent intent) {
                // According to the docs, "Alarm Manager holds a CPU wake lock as
                // long as the alarm receiver's onReceive() method is executing.
                // This guarantees that the phone will not sleep until you have
                // finished handling the broadcast.", but this class still get
                // a wake lock to wait for ping finished.
                int count = intent.getIntExtra(Intent.EXTRA_ALARM_COUNT, -1);
                Log.d(TAG, "Ping " + count + " times.");

                Log.d(TAG, "Check time :" + System.currentTimeMillis());
                IMqttToken token = comms.checkForActivity();

                // No ping has been sent.
                if (token == null) {
                    return;
                }

                // Assign new callback to token to execute code after PingResq
                // arrives. Get another wakelock even receiver already has one,
                // release it until ping response returns.
                if (wakelock == null) {
                    PowerManager pm = (PowerManager) mContext.getSystemService(Service.POWER_SERVICE);
                    wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                            wakeLockTag);
                }
                wakelock.acquire();
                token.setActionCallback(new IMqttActionListener() {

                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d(TAG, "Success. Release lock(" + wakeLockTag + "):"
                                + System.currentTimeMillis());
                        //Release wakelock when it is done.
                        if(wakelock != null && wakelock.isHeld()){
                            wakelock.release();
                        }
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken,
                                          Throwable exception) {
                        Log.d(TAG, "Failure. Release lock(" + wakeLockTag + "):"
                                + System.currentTimeMillis());
                        //Release wakelock when it is done.
                        if(wakelock != null && wakelock.isHeld()){
                            wakelock.release();
                        }

                        reconnect();
                    }
                });
            }
        }
    }


    class MessageStore extends SQLiteOpenHelper {

        private SQLiteDatabase db = null;

        // the name of the table in the database to which we will save messages
        private static final String DATABASE_NAME = "mqttAndroidService.db";
        private static final int DATABASE_VERSION = 1;
        static final String ARRIVED_MESSAGE_TABLE_NAME = "MqttArrivedMessageTable";
        static final String DUPLICATE = "duplicate";
        static final String RETAINED = "retained";
        static final String QOS = "qos";
        static final String PAYLOAD = "payload";
        static final String MTIMESTAMP = "mtimestamp";

        public MessageStore() {
            super(mContext, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase database) {
            String createArrivedTableStatement = "CREATE TABLE "
                    + ARRIVED_MESSAGE_TABLE_NAME + "("
                    + MESSAGE_ID + " TEXT PRIMARY KEY, "
                    + DESTINATION_NAME + " TEXT, "
                    + PAYLOAD + " BLOB, "
                    + QOS + " INTEGER, "
                    + RETAINED + " TEXT, "
                    + DUPLICATE + " TEXT, " + MTIMESTAMP
                    + " INTEGER" + ");";
            try {
                database.execSQL(createArrivedTableStatement);
            } catch (SQLException e) {
                throw e;
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.d(TAG, "onUpgrade");
            try {
                db.execSQL("DROP TABLE IF EXISTS " + ARRIVED_MESSAGE_TABLE_NAME);
            } catch (SQLException e) {
                throw e;
            }
            onCreate(db);
            Log.d(TAG, "onUpgrade complete");
        }

        public String storeArrived(String topic,
                                   MqttMessage message) {
            db = getWritableDatabase();

            //Log.d(TAG, "storeArrived{" + message.toString() + "}");
            byte[] payload = message.getPayload();
            int qos = message.getQos();
            boolean retained = message.isRetained();
            boolean duplicate = message.isDuplicate();

            ContentValues values = new ContentValues();
            String id = java.util.UUID.randomUUID().toString();
            values.put(MESSAGE_ID, id);
            values.put(DESTINATION_NAME, topic);
            values.put(PAYLOAD, payload);
            values.put(QOS, qos);
            values.put(RETAINED, retained);
            values.put(DUPLICATE, duplicate);
            values.put(MTIMESTAMP, System.currentTimeMillis());

            try {
                db.insertOrThrow(ARRIVED_MESSAGE_TABLE_NAME, null, values);
            } catch (SQLException e) {
                throw e;
            }
            int count = getArrivedRowCount();
            //Log.d(TAG, "storeArrived: inserted message with id of {"
            //                        + id
            //                        + "} - Number of messages in database for this clientHandle = "
            //                        + count);
            return id;
        }

        private int getArrivedRowCount() {
            String[] cols = new String[1];
            cols[0] = "COUNT(*)";
            Cursor c = db.query(ARRIVED_MESSAGE_TABLE_NAME, cols,
                    null,
                    null, null, null, null);
            int count = 0;
            if (c.moveToFirst()) {
                count = c.getInt(0);
            }
            c.close();
            return count;
        }

        public void clearArrivedMessages() {

            db = getWritableDatabase();
            db.delete(ARRIVED_MESSAGE_TABLE_NAME, null, null);
        }

        public boolean discardArrived(String id) {

            db = getWritableDatabase();

            //Log.d(TAG, "discardArrived{" + id + "}");
            int rows;
            try {
                rows = db.delete(ARRIVED_MESSAGE_TABLE_NAME,MESSAGE_ID + "='" + id + "'", null);
            } catch (SQLException e) {
                Log.e(TAG, Log.getStackTraceString(e));
                return false;
            }
            if (rows != 1) {
                Log.e(TAG,
                        "discardArrived - Error deleting message {" + id
                                + "} from database: Rows affected = " + rows);
                return false;
            }
            int count = getArrivedRowCount();
            //Log.d(TAG,
            //        "discardArrived - Message deleted successfully. - messages in db for this clientHandle "
            //                + count);
            return true;
        }

        public Iterator<StoredMessage> getAllArrivedMessages() {
            return new Iterator<StoredMessage>() {
                private Cursor c;
                private boolean hasNext;

                {
                    db = getWritableDatabase();
                    // anonymous initialiser to start a suitable query
                    // and position at the first row, if one exists
                    c = db.query(ARRIVED_MESSAGE_TABLE_NAME, null, null, null,
                            null, null, "mtimestamp ASC");
                    hasNext = c.moveToFirst();
                }

                @Override
                public boolean hasNext() {
                    if (!hasNext){
                        c.close();
                    }
                    return hasNext;
                }

                @Override
                public StoredMessage next() {
                    String messageId = c.getString(c.getColumnIndex(MESSAGE_ID));
                    String topic = c.getString(c.getColumnIndex(DESTINATION_NAME));
                    byte[] payload = c.getBlob(c.getColumnIndex(PAYLOAD));
                    int qos = c.getInt(c.getColumnIndex(QOS));
                    boolean retained = Boolean.parseBoolean(c.getString(c.getColumnIndex(RETAINED)));
                    boolean dup = Boolean.parseBoolean(c.getString(c.getColumnIndex(DUPLICATE)));

                    // build the result
                    MqttMessageHack message = new MqttMessageHack(payload);
                    message.setQos(qos);
                    message.setRetained(retained);
                    message.setDuplicate(dup);

                    // move on
                    hasNext = c.moveToNext();
                    return new StoredMessage(messageId, topic, message);
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }

                @Override
                protected void finalize() throws Throwable {
                    c.close();
                    super.finalize();
                }

            };
        }

        @Override
        public void close() {
            super.close();
            if (this.db!=null)
                this.db.close();

        }
    }

    static class MqttMessageHack extends MqttMessage {

        public MqttMessageHack(byte[] payload) {
            super(payload);
        }

        @Override
        protected void setDuplicate(boolean dup) {
            super.setDuplicate(dup);
        }
    }

    static class StoredMessage {
        public String messageId;
        public String topic;
        public MqttMessage message;

        StoredMessage(String messageId, String topic,
                      MqttMessage message) {
            this.messageId = messageId;
            this.topic = topic;
            this.message = message;
        }
    }

    static class ParcelableMqttMessage extends MqttMessage implements Parcelable {
        String messageId = null;

        ParcelableMqttMessage(MqttMessage original) {
            super(original.getPayload());
            setQos(original.getQos());
            setRetained(original.isRetained());
            setDuplicate(original.isDuplicate());
        }

        ParcelableMqttMessage(Parcel parcel) {
            super(parcel.createByteArray());
            setQos(parcel.readInt());
            boolean[] flags = parcel.createBooleanArray();
            setRetained(flags[0]);
            setDuplicate(flags[1]);
            messageId = parcel.readString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeByteArray(getPayload());
            parcel.writeInt(getQos());
            parcel.writeBooleanArray(new boolean[]{isRetained(), isDuplicate()});
            parcel.writeString(messageId);
        }

        public static final Parcelable.Creator<ParcelableMqttMessage> CREATOR = new Parcelable.Creator<ParcelableMqttMessage>() {

            /**
             * Creates a message from the parcel object
             */
            @Override
            public ParcelableMqttMessage createFromParcel(Parcel parcel) {
                return new ParcelableMqttMessage(parcel);
            }

            /**
             * creates an array of type {@link ParcelableMqttMessage}[]
             *
             */
            @Override
            public ParcelableMqttMessage[] newArray(int size) {
                return new ParcelableMqttMessage[size];
            }
        };
    }
}
