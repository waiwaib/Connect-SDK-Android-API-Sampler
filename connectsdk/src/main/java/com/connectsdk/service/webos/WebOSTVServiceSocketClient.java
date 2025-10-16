package com.connectsdk.service.webos;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.connectsdk.core.Util;
import com.connectsdk.discovery.DiscoveryManager;
import com.connectsdk.service.DeviceService.PairingType;
import com.connectsdk.service.WebOSTVService;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommand;
import com.connectsdk.service.command.ServiceCommand.ServiceCommandProcessor;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.command.URLServiceSubscription;
import com.connectsdk.service.config.WebOSTVServiceConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

import javax.net.ssl.SSLContext;

public class WebOSTVServiceSocketClient  implements ServiceCommandProcessor {

    static final String WEBOS_PAIRING_PROMPT = "PROMPT";
    static final String WEBOS_PAIRING_PIN = "PIN";
    static final String WEBOS_PAIRING_COMBINED = "COMBINED";

    public enum State {
        NONE,
        INITIAL,
        CONNECTING,
        REGISTERING,
        REGISTERED,
        DISCONNECTING
    };

    WebOSTVServiceSocketClientListener mListener;

    WebOSTVTrustManager customTrustManager;  // 1.6.1 patch

    int nextRequestId = 1;

    State state;

    JSONObject manifest;

    static final int PORT = 3001;
    static boolean verification_status = false;
    static final String Public_Key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2At7fSUHuMw6bm/z3Q+X4oY9KpDa1s06\n" +
            "mht9vNmSkZE5xMo9asOtZAWLLbJLxifY6qz6LWKgNw4Pyk6HVTLFdj4jrV//gNGQvYtCp3HRriqg\n" +
            "2YoceBNG59+SW3xNzuhUqy5/nerQPfNQiz9z9RqtGj/YWItlJcKrNOBecNmHc7Xmu+3yPN6kD1G2\n" +
            "6uU8wPBqzMdqFpPcubedIOmh4nNa2sNkfvMkbR4Pk/YupsDpic56dMxX0Twvg6SiaKGjv8NO9Lcv\n"+
            "hLt2dR2XXi/z2F6uVjP5oYPvlSAK9GHVo96khpafKGPvIwPSSGtlHI4is/yT7WEeLuQs5FD/vAs9\n"+
            "eqQNkQIDAQAB\n";

    // Queue of commands that should be sent once register is complete
    LinkedHashSet<ServiceCommand> commandQueue = new LinkedHashSet<>();

    public final Map<Integer, ServiceCommand> requests = new HashMap<>();
    WebOSTVServiceConfig mConfig;
    List<String> permissions;
    PairingType mPairingType;

    public String getClientKey() {
        return mConfig.getClientKey();
    }

    public WebOSTVServiceSocketClient(WebOSTVServiceConfig config, PairingType pairingType , List<String> permissions, URI uri) {

        this.mPairingType = pairingType;
        this.mConfig = config;
        state = State.INITIAL;
        this.permissions = permissions;
        setDefaultManifest();
    }

    public WebOSTVServiceSocketClient(WebOSTVService service, URI uri) {
        this.mPairingType = service.getPairingType();
        this.mConfig = service.getWebOSTVServiceConfig();
        this.permissions = service.getPermissions();
        state = State.INITIAL;
        setDefaultManifest();
    }

    public static URI getURI(WebOSTVService service) {
        String uriString = "wss://" + service.getServiceDescription().getIpAddress() + ":"
                + service.getServiceDescription().getPort();
        URI uri = null;

        try {
            uri = new URI(uriString);
        } catch (URISyntaxException e) {
            Log.e(Util.T, "error:",e);
        }

        return uri;
    }

    public static URI getURI(String IpAddress) {
        String uriString = "wss://" + IpAddress + ":" + PORT;
        URI uri = null;

        try {
            uri = new URI(uriString);
        } catch (URISyntaxException e) {
            Log.e(Util.T, "error:",e);
        }

        return uri;
    }

    public static URI getURI(String IpAddress, int port) {
        String uriString = "wss://" + IpAddress + ":" + port;
        URI uri = null;

        try {
            uri = new URI(uriString);
        } catch (URISyntaxException e) {
            Log.e(Util.T, "error:",e);
        }

        return uri;
    }

    public WebOSTVServiceSocketClientListener getListener() {
        return mListener;
    }

    public void setListener(WebOSTVServiceSocketClientListener mListener) {
        this.mListener = mListener;
    }

    public State getState() {
        return state;
    }

    public void connect() {
        synchronized (this) {
            if (state != State.INITIAL) {
                Log.d(Util.T, "already connecting; not trying to connect again: " + state);
                return; // don't try to connect again while connected
            }

            state = State.CONNECTING;
        }

        setupSSL();

//        super.connect();
    }

    public void disconnect() {
        disconnectWithError(null);
    }

    public void disconnectWithError(ServiceCommandError error) {
//        this.close();

        state = State.INITIAL;

        if (mListener != null)
            mListener.onCloseWithError(error);
    }

    public void clearRequests() {
        synchronized (requests) {
            requests.clear();
        }
    }

    private void setDefaultManifest() {
        manifest = new JSONObject();

        try {
            manifest.put("manifestVersion", 1);
//            manifest.put("appId", 1);
//            manifest.put("vendorId", 1);
//            manifest.put("localizedAppNames", 1);
            manifest.put("permissions",  convertStringListToJSONArray(permissions));
        } catch (JSONException e) {
            Log.e(Util.T, "error:",e);
        }
    }

    private JSONArray convertStringListToJSONArray(List<String> list) {
        JSONArray jsonArray = new JSONArray();

        for(String str: list) {
            jsonArray.put(str);
        }

        return jsonArray;
    }


    protected void handleConnected() {
        helloTV();
    }

    protected void handleConnectError(Exception ex) {
        System.err.println("connect error: " + ex.toString());

        if (mListener != null)
            mListener.onFailWithError(new ServiceCommandError(0, "connection error", null));
    }

    protected void handleMessage(String data) {
        try {
            JSONObject obj = new JSONObject(data);

            handleMessage(obj);
        } catch (JSONException e) {
            Log.e(Util.T, "error:",e);
        }
    }

    protected void handleMessage(JSONObject message) {
        Boolean shouldProcess = true;

        if (mListener != null)
            shouldProcess = mListener.onReceiveMessage(message);

        if (!shouldProcess)
            return;

        String type = message.optString("type");
        Object payload = message.opt("payload");

        String strId = message.optString("id");
        Integer id = null;
        ServiceCommand request = null;

        if (isInteger(strId)) {
            id = Integer.valueOf(strId);

            try
            {
                request = (ServiceCommand) requests.get(id);
            } catch (ClassCastException ex)
            {
                // since request is assigned to null, don't need to do anything here
            }
        }

        if (type.isEmpty())
            return;

        switch (type) {
            case "response":
                if (request != null) {
//                Log.d(Util.T, "Found requests need to handle response");
                    if (payload != null) {
                        Util.postSuccess(request.getResponseListener(), payload);
                    } else {
                        Util.postError(request.getResponseListener(), new ServiceCommandError(-1, "JSON parse error", null));
                    }

                    if (!(request instanceof URLServiceSubscription)) {
                        if (!(payload instanceof JSONObject && ((JSONObject) payload).has("pairingType")))
                            synchronized (requests) {
                                requests.remove(id);
                            }
                    }
                } else {
                    System.err.println("no matching request id: " + strId + ", payload: " + payload.toString());
                }
                break;
            case "registered":
                if (mConfig == null) {

                    mConfig = new WebOSTVServiceConfig(mConfig.getServiceUUID());
                }

                if (payload instanceof JSONObject) {
                    String clientKey = ((JSONObject) payload).optString("client-key");
                    mConfig.setClientKey(clientKey);
                    mListener.updateClientKey(clientKey);
                    // Track SSL certificate
                    // Not the prettiest way to get it, but we don't have direct access to the SSLEngine

                    sendVerification();
                    if (verification_status) {
                        ((WebOSTVServiceConfig) mConfig).setServerCertificate(customTrustManager.getLastCheckedCertificate());
                        handleRegistered();

                        if (id != null)
                            synchronized (requests) {
                                requests.remove(id);
                            }
                    } else {
                        Log.d(Util.T, "Certification Verification Failed");
                        mListener.onRegistrationFailed(new ServiceCommandError(0, "Certificate Registration failed", null));
                    }
                }
                break;
            case "error":
                String error = message.optString("error");
                if (error.length() == 0)
                    return;

                int errorCode = -1;
                String errorDesc = null;

                try {
                    String[] parts = error.split(" ", 2);
                    errorCode = Integer.parseInt(parts[0]);
                    errorDesc = parts[1];
                } catch (Exception e) {
                    Log.e(Util.T, "error:", e);
                }

                if (payload != null) {
                    Log.d(Util.T, "Error Payload: " + payload.toString());
                }

                if (message.has("id")) {
                    Log.d(Util.T, "Error Desc: " + errorDesc);

                    if (request != null) {
                        Util.postError(request.getResponseListener(), new ServiceCommandError(errorCode, errorDesc, payload));

                        if (!(request instanceof URLServiceSubscription))
                            synchronized (requests) {
                                requests.remove(id);
                            }

                    }
                }
                break;
            case "hello":
                JSONObject jsonObj = (JSONObject) payload;

                if (mConfig.getServiceUUID() != null) {
                    if (!mConfig.getServiceUUID().equals(jsonObj.optString("deviceUUID"))) {
                        mConfig.setClientKey(null);
                        mConfig.setServerCertificate((String) null);
                        mConfig.setServiceUUID(null);
                        mListener.updateClientKey(null);
                        mListener.updateUUID(null);
                        mListener.updateIPAddress(null);
                        mListener.updateIPAddress(null);
                        mListener.updateUUID(null);

                        disconnect();
                    }
                } else {
                    String uuid = jsonObj.optString("deviceUUID");
                    mConfig.setServiceUUID(uuid);
                    mListener.updateUUID(uuid);
                }

                state = State.REGISTERING;
                sendRegister();
                break;
        }
    }

    private void helloTV() {
        Context context = DiscoveryManager.getInstance().getContext();
        PackageManager packageManager = context.getPackageManager();

        // app Id
        String packageName = context.getPackageName();

        // SDK Version
        String sdkVersion = DiscoveryManager.CONNECT_SDK_VERSION;

        // Device Model
        String deviceModel = Build.MODEL;

        // OS Version
        String OSVersion = String.valueOf(Build.VERSION.SDK_INT);

        // resolution
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        @SuppressWarnings("deprecation")
        int width = display.getWidth(); // deprecated, but still needed for supporting API levels 10-12

        @SuppressWarnings("deprecation")
        int height = display.getHeight(); // deprecated, but still needed for supporting API levels 10-12

        String screenResolution = String.format("%dx%d", width, height);

        // app Name
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
        } catch (final NameNotFoundException e) {
            applicationInfo = null;
        }
        String applicationName = (String) (applicationInfo != null ? packageManager.getApplicationLabel(applicationInfo) : "(unknown)");

        // app Region
        Locale current = context.getResources().getConfiguration().locale;
        String appRegion = current.getDisplayCountry();

        JSONObject payload = new JSONObject();
        try {
            payload.put("sdkVersion", sdkVersion);
            payload.put("deviceModel", deviceModel);
            payload.put("OSVersion", OSVersion);
            payload.put("resolution", screenResolution);
            payload.put("appId", packageName);
            payload.put("appName", applicationName);
            payload.put("appRegion", appRegion);
        } catch (JSONException e) {
            Log.e(Util.T, "error:",e);
        }

        int dataId = this.nextRequestId++;

        JSONObject sendData = new JSONObject();
        try {
            sendData.put("id", dataId);
            sendData.put("type", "hello");
            sendData.put("payload", payload);
        } catch (JSONException e) {
            Log.e(Util.T, "error:",e);
        }

        ServiceCommand request = new ServiceCommand(this, null, sendData, true, null);
        this.sendCommandImmediately(request);
    }

    protected void sendVerification() {
        ResponseListener<Object> listener = new ResponseListener<Object>() {

            @Override
            public void onError(ServiceCommandError error) {
                state = State.INITIAL;

                if (mListener != null)
                    mListener.onRegistrationFailed(error);
            }

            @Override
            public void onSuccess(Object any) {
                if (any instanceof JSONObject) {

                }
            }
        };

        int dataId = this.nextRequestId++;

        ServiceCommand command = new ServiceCommand(this, null, null, listener);
        command.setRequestId(dataId);

        JSONObject headers = new JSONObject();
        JSONObject payload = new JSONObject();
        int public_key_value = 0;
        int valid_value = 0;

        try {

            headers.put("type", "verification");
            headers.put("id", dataId);

            X509Certificate cert = customTrustManager.getLastCheckedCertificate();
            PublicKey pk = null;

            pk = cert.getPublicKey();
            String pubKey = Base64.encodeToString(pk.getEncoded(),Base64.DEFAULT);

            try {
                cert.verify(pk);
                verification_status = true;
            } catch (CertificateException|SignatureException e) {
                if (!(Public_Key == null || Public_Key.isEmpty())) {
                    boolean verified = pubKey.trim().equalsIgnoreCase(Public_Key.trim());
                    if (verified) {
                        payload.put("public-key", 1);
                        public_key_value = 1;
                    } else {
                        payload.put("public-key", -1);
                        public_key_value = -1;
                    }
                } else {
                    payload.put("public-key", 1);
                    public_key_value = 1;
                }

                try {
                    ((X509Certificate) cert).checkValidity();
                    payload.put("validity", 1);
                    valid_value = 1;
                } catch (CertificateExpiredException | CertificateNotYetValidException error) {
                    payload.put("validity", -1);
                    valid_value = -1;
                    error.printStackTrace();
                }

                if (public_key_value == 1 && valid_value == 1) {
                    verification_status = true;
                }
                synchronized (requests) {
                    requests.put(dataId, command);
                }
                sendMessage(headers, payload);

            } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException e) {
                Log.e(Util.T, "error:",e);
            }

        } catch (JSONException e) {
            Log.e(Util.T, "error:",e);
        }
    }

    protected void sendRegister() {
        ResponseListener<Object> listener = new ResponseListener<Object>() {

            @Override
            public void onError(ServiceCommandError error) {
                state = State.INITIAL;

                if (mListener != null)
                    mListener.onRegistrationFailed(error);
            }

            @Override
            public void onSuccess(Object any) {
                if (any instanceof JSONObject) {
                    PairingType pairingType = PairingType.NONE;
                    
                    JSONObject jsonObj = (JSONObject) any;
                    String type = jsonObj.optString("pairingType");
                    if (type.equalsIgnoreCase("PROMPT")) {
                        pairingType = PairingType.FIRST_SCREEN;
                    }
                    else if (type.equalsIgnoreCase("PIN")) {
                        pairingType = PairingType.PIN_CODE;
                    }
                    if (mListener != null)
                        mListener.onBeforeRegister(pairingType);
                }
            }
        };

        int dataId = this.nextRequestId++;

        ServiceCommand command = new ServiceCommand(this, null, null, listener);
        command.setRequestId(dataId);

        JSONObject headers = new JSONObject();
        JSONObject payload = new JSONObject();

        try {
            headers.put("type", "register");
            headers.put("id", dataId);

            if (!(mConfig instanceof WebOSTVServiceConfig)) {
                mConfig = new WebOSTVServiceConfig(mConfig.getServiceUUID());
            }

            if (mConfig.getClientKey() != null) {
                payload.put("client-key", mConfig.getClientKey());
            }

            if (PairingType.PIN_CODE.equals(mPairingType)) {
                payload.put("pairingType", "PIN");
            }

            if (manifest != null) {
                payload.put("manifest", manifest);
            }
        } catch (JSONException e) {
            Log.e(Util.T, "error:",e);
        }
        synchronized (requests) {
            requests.put(dataId, command);
        }

        sendMessage(headers, payload);
    }

    public void sendPairingKey(String pairingKey) {
        ResponseListener<Object> listener = new ResponseListener<Object>() {

            @Override
            public void onError(ServiceCommandError error) {
                state = State.INITIAL;

                if (mListener != null)
                    mListener.onFailWithError(error);
            }

            @Override
            public void onSuccess(Object any) { }
        };

        String uri = "ssap://pairing/setPin";

        int dataId = this.nextRequestId++;

        ServiceCommand command = new ServiceCommand(this, null, null, listener);
        command.setRequestId(dataId);

        JSONObject headers = new JSONObject();
        JSONObject payload = new JSONObject();

        try {
            headers.put("type", "request");
            headers.put("id", dataId);
            headers.put("uri", uri);

            payload.put("pin", pairingKey);
        } catch (JSONException e) {
            Log.e(Util.T, "error:",e);
        }
        synchronized (requests) {
            requests.put(dataId, command);
        }

        sendMessage(headers, payload);
    }

    protected void handleRegistered() {
        state = State.REGISTERED;

        if (!commandQueue.isEmpty()) {
            LinkedHashSet<ServiceCommand> tempHashSet = new LinkedHashSet<ServiceCommand>(commandQueue);
            for (ServiceCommand command : tempHashSet) {
                Log.d(Util.T, "executing queued command for " + command.getTarget());

                sendCommandImmediately(command);
                commandQueue.remove(command);
            }
        }

        if (mListener != null)
            mListener.onConnect();

//        ConnectableDevice storedDevice = connectableDeviceStore.getDevice(mService.getServiceConfig().getServiceUUID());
//        if (storedDevice == null) {
//            storedDevice = new ConnectableDevice(
//                    mService.getServiceDescription().getIpAddress(),
//                    mService.getServiceDescription().getFriendlyName(),
//                    mService.getServiceDescription().getModelName(),
//                    mService.getServiceDescription().getModelNumber());
//        }
//        storedDevice.addService(WebOSTVService.this);
//        connectableDeviceStore.addDevice(storedDevice);
    }

    @SuppressWarnings("unchecked")
    public void sendCommand(ServiceCommand command) {
        Integer requestId;
        if (command.getRequestId() == -1) {
            requestId = this.nextRequestId++;
            command.setRequestId(requestId);
        }
        else {
            requestId = command.getRequestId();
        }
        synchronized (requests) {
            requests.put(requestId, command);
        }

        if (state == State.REGISTERED) {
            this.sendCommandImmediately(command);
        } else if (state == State.CONNECTING || state == State.DISCONNECTING){
            Log.d(Util.T, "queuing command for " + command.getTarget());
            commandQueue.add((ServiceCommand) command);
        } else {
            Log.d(Util.T, "queuing command and restarting socket for " + command.getTarget());
            commandQueue.add((ServiceCommand) command);
            connect();
        }
    }

    public void unsubscribe(URLServiceSubscription<?> subscription) {
        int requestId = subscription.getRequestId();

        if (requests.get(requestId) != null) {
            JSONObject headers = new JSONObject();

            try{
                headers.put("type", "unsubscribe");
                headers.put("id", String.valueOf(requestId));
            } catch (JSONException e)
            {
                // Safe to ignore
                Log.e(Util.T, "error:",e);
            }

            sendMessage(headers, null);
            synchronized (requests) {
                requests.remove(requestId);
            }
        }
    }

    public void unsubscribe(ServiceSubscription<?> subscription) { }

    protected void sendCommandImmediately(ServiceCommand command) {
        JSONObject headers = new JSONObject();
        JSONObject payload = (JSONObject) command.getPayload();
        String payloadType = "";

        try
        {
            payloadType = payload.getString("type");
        } catch (Exception ex)
        {
            // ignore
        }

        if (payloadType.equals("p2p"))
        {
            Iterator<?> iterator = payload.keys();

            while (iterator.hasNext())
            {
                String key = (String) iterator.next();

                try
                {
                    headers.put(key, payload.get(key));
                } catch (JSONException ex)
                {
                    // ignore
                }
            }

            this.sendMessage(headers, null);
        }
        else if (payloadType.equals("hello")) {
//            this.send(payload.toString());
        }
        else {
            try
            {
                headers.put("type", command.getHttpMethod());
                headers.put("id", String.valueOf(command.getRequestId()));
                headers.put("uri", command.getTarget());
            } catch (JSONException ex)
            {
                // TODO: handle this
            }


            this.sendMessage(headers, payload);
        }
    }

    private void setSSLContext(SSLContext sslContext) {
        //Web-Socket 1.3.7 patch
        /*try {
            setSocket(sslContext.getSocketFactory().createSocket());
			setConnectionLostTimeout(0);
        } catch (IOException e) {
            Log.e(Util.T, "error:",e);
        } catch (RuntimeException e) {
            Log.e(Util.T, "error:",e);
        }*/
        //patch ends
    }

    protected void setupSSL() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            customTrustManager = new WebOSTVTrustManager();
            sslContext.init(null, new WebOSTVTrustManager [] {customTrustManager}, null);
            setSSLContext(sslContext);

            if (!(mConfig instanceof WebOSTVServiceConfig)) {
                mConfig = new WebOSTVServiceConfig(mConfig.getServiceUUID());
            }
            customTrustManager.setExpectedCertificate(mConfig.getServerCertificate());
        } catch (KeyException e) {
        } catch (NoSuchAlgorithmException e) {
        }
    }

    public boolean isConnected() {
        return false;
    }

    public void sendMessage(JSONObject packet, JSONObject payload) {
//        JSONObject packet = new JSONObject();

        try {
//            for (Map.Entry<String, String> entry : headers.entrySet()) {
//                packet.put(entry.getKey(), entry.getValue());
//            }

            if (payload != null) {
                packet.put("payload", payload);
            }
        } catch (JSONException e) {
            throw new Error(e);
        }

        if (isConnected()) {
            String message = packet.toString();

            Log.d(Util.T, "webOS Socket [OUT] : " + message);

//            this.send(message);
        }
        else {
            System.err.println("connection lost");
            handleConnectionLost(false, null);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleConnectionLost(boolean cleanDisconnect, Exception ex) {
        ServiceCommandError error = null;
        if (ex != null || !cleanDisconnect)
            error = new ServiceCommandError(0, "conneciton error", ex);

        if (mListener != null)
            mListener.onCloseWithError(error);
        synchronized (requests) {
            for (Map.Entry<Integer, ServiceCommand> entry : requests.entrySet()) {
                ServiceCommand request = (ServiceCommand) entry.getValue();
                if (request != null)
                    Util.postError(request.getResponseListener(), new ServiceCommandError(0, "connection lost", null));
            }
        }

        clearRequests();
    }

    public void setServerCertificate(X509Certificate cert) {
        if (!(mConfig instanceof WebOSTVServiceConfig)) {
            mConfig = new WebOSTVServiceConfig(mConfig.getServiceUUID());
        }

        mConfig.setServerCertificate(cert);
    }

    public void setServerCertificate(String cert) {
        if (!(mConfig instanceof WebOSTVServiceConfig)) {
            mConfig = new WebOSTVServiceConfig(mConfig.getServiceUUID());
        }

        mConfig.setServerCertificate(loadCertificateFromPEM(cert));
    }

    public X509Certificate getServerCertificate() {
        if (!(mConfig instanceof WebOSTVServiceConfig)) {
            mConfig = new WebOSTVServiceConfig(mConfig.getServiceUUID());
        }

        return mConfig.getServerCertificate();
    }

    public String getServerCertificateInString() {
        if (!(mConfig instanceof WebOSTVServiceConfig)) {
            mConfig = new WebOSTVServiceConfig(mConfig.getServiceUUID());
        }

        return exportCertificateToPEM(mConfig.getServerCertificate());
    }

    private String exportCertificateToPEM(X509Certificate cert) {
        try {
            return Base64.encodeToString(cert.getEncoded(), Base64.DEFAULT);
        } catch (CertificateEncodingException e) {
            Log.e(Util.T, "error:",e);
            return null;
        }
    }

    private X509Certificate loadCertificateFromPEM(String pemString) {
        CertificateFactory certFactory;
        try {
            certFactory = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream inputStream = new ByteArrayInputStream(pemString.getBytes("US-ASCII"));

            return (X509Certificate)certFactory.generateCertificate(inputStream);
        } catch (CertificateException e) {
            Log.e(Util.T, "error:",e);
            return null;
        } catch (UnsupportedEncodingException e) {
            Log.e(Util.T, "error:",e);
            return null;
        }
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch(NumberFormatException e) {
            return false;
        }
        // only got here if we didn't return false
        return true;
    }

    public interface WebOSTVServiceSocketClientListener {

        public void onConnect();
        public void onCloseWithError(ServiceCommandError error);
        public void onFailWithError(ServiceCommandError error);

        public void onBeforeRegister(PairingType pairingType);
        public void onRegistrationFailed(ServiceCommandError error);
        public Boolean onReceiveMessage(JSONObject message);

         public void updateClientKey(String ClientKey);
         public void updateUUID(String UUID);
         public void updateIPAddress(String IPAddress);
    }
}