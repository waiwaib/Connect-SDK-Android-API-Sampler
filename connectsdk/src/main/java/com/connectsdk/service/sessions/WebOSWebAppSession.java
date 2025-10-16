/*
 * WebOSWebAppSession
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Jeffrey Glenn on 07 Mar 2014
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.connectsdk.service.sessions;

import androidx.annotation.NonNull;
import android.util.Log;

import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.connectsdk.core.ImageInfo;
import com.connectsdk.core.MediaInfo;
import com.connectsdk.core.SubtitleInfo;
import com.connectsdk.core.Util;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.WebOSTVService;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.PlaylistControl;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommand;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.command.URLServiceSubscription;
import com.connectsdk.service.sessions.LaunchSession.LaunchSessionType;

public class WebOSWebAppSession extends WebAppSession {
    private static final String namespaceKey = "connectsdk.";
    private static final String ENABLED_SUBTITLE_ID = "1";

    protected WebOSTVService service;

    ResponseListener<ServiceCommand> mConnectionListener;

    public URLServiceSubscription<ResponseListener<Object>> appToAppSubscription;

    private ServiceSubscription<PlayStateListener> mPlayStateSubscription;
    private ServiceSubscription<MessageListener> mMessageSubscription;
    private final ConcurrentHashMap<String, ServiceCommand> mActiveCommands;

    private ServiceSubscription<WebAppPinStatusListener> mWebAppPinnedSubscription;

    String mFullAppId;

    private int UID;
    private boolean connected;

    public WebOSWebAppSession(LaunchSession launchSession, DeviceService service) {
        super(launchSession, service);

        UID = 0;
        mActiveCommands = new ConcurrentHashMap<>(0, 0.75f, 10);
        connected = false;

        this.service = (WebOSTVService) service;
    }

    private int getNextId() {
        return ++UID;
    }

    public Boolean isConnected() {
        return connected;
    }

    public void setConnected(Boolean connected) {
        this.connected = connected;
    }

    /** @noinspection unused*/
    public void handleMediaEvent(JSONObject payload) {
        String type = payload.optString("type");
        if (type.isEmpty()) {
            String errorMsg = payload.optString("error");

            if (errorMsg.isEmpty()) {
                return;
            } else {
                Log.d(Util.T, "Play State Error: " + errorMsg);
                if (mPlayStateSubscription != null) {
                    for (PlayStateListener listener : mPlayStateSubscription.getListeners()) {
                        Util.postError(listener, new ServiceCommandError(errorMsg));
                    }
                }
            }
        }

        if (type.equals("playState")) {
            if (mPlayStateSubscription == null)
                return;

            String playStateString = payload.optString(type);
            if (playStateString.isEmpty())
                return;

            final PlayStateStatus playState = parsePlayState(playStateString);

            for (PlayStateListener listener : mPlayStateSubscription.getListeners()) {
                Util.postSuccess(listener, playState);
            }
        }
    }

    public String getFullAppId() {
        if (mFullAppId == null) {
            if (launchSession.getSessionType() != LaunchSessionType.WebApp)
                mFullAppId = launchSession.getAppId();
            else {
                Enumeration<String> enumeration = service.getWebAppIdMappings().keys();

                while (enumeration.hasMoreElements()) {
                    String mappedFullAppId = enumeration.nextElement();
                    String mappedAppId = service.getWebAppIdMappings().get(mappedFullAppId);

                    if (mappedAppId != null && mappedAppId.equalsIgnoreCase(launchSession.getAppId())) {
                        mFullAppId = mappedAppId;
                        break;
                    }
                }
            }
        }

        if (mFullAppId == null)
            return launchSession.getAppId();
        else
            return mFullAppId;
    }

    public void setFullAppId(String fullAppId) {
        mFullAppId = fullAppId;
    }



    /** @noinspection unused*/
    public void handleMediaCommandResponse(final JSONObject payload) {
        String requestID = payload.optString("requestId");
        if (requestID.isEmpty())
            return;

        final ServiceCommand command = mActiveCommands.get(requestID);

        if (command == null)
            return;

        String mError = payload.optString("error");

        if (!mError.isEmpty()) {
            Util.postError(command.getResponseListener(), new ServiceCommandError(0, mError, null));
        } else {
            Util.postSuccess(command.getResponseListener(), payload);
        }

        mActiveCommands.remove(requestID);
    }

    /** @noinspection unused*/
    public void handleMessage(final Object message) {
        Util.runOnUI(() -> {
            if (getWebAppSessionListener() != null)
                getWebAppSessionListener().onReceiveMessage(WebOSWebAppSession.this, message);
        });

    }

    public PlayStateStatus parsePlayState(String playStateString) {
        switch (playStateString) {
            case "playing":
                return PlayStateStatus.Playing;
            case "paused":
                return PlayStateStatus.Paused;
            case "idle":
                return PlayStateStatus.Idle;
            case "buffering":
                return PlayStateStatus.Buffering;
            case "finished":
                return PlayStateStatus.Finished;
        }

        return PlayStateStatus.Unknown;
    }

    public void connect(ResponseListener<Object> connectionListener) {
        connect(false, connectionListener);
    }

    @Override
    public void join(ResponseListener<Object> connectionListener) {
        connect(true, connectionListener);
    }

    private void connect(final Boolean joinOnly,
            final ResponseListener<Object> connectionListener) {


        if (isConnected()) {
            if (connectionListener != null)
                connectionListener.onSuccess(null);

            return;
        }

        mConnectionListener = new ResponseListener<ServiceCommand>() {

            @Override
            public void onError(ServiceCommandError error) {

                if (connectionListener != null) {
                    if (error == null) {
                        error = new ServiceCommandError(0, "Unknown error connecting to web app",
                                null);
                    }

                    connectionListener.onError(error);
                }
            }

            @Override
            public void onSuccess(ServiceCommand any) {
                ResponseListener<Object> finalConnectionListener = new ResponseListener<Object>() {

                    @Override
                    public void onError(ServiceCommandError error) {
                        disconnectFromWebApp();

                        if (connectionListener != null)
                            connectionListener.onError(error);
                    }

                    @Override
                    public void onSuccess(Object any) {
                        connected = true;

                        if (connectionListener != null)
                            connectionListener.onSuccess(any);
                    }
                };

                service.connectToWebApp(WebOSWebAppSession.this, joinOnly, finalConnectionListener);
            }
        };

    }

    public void disconnectFromWebApp() {
        connected = false;
        mConnectionListener = null;

        if (appToAppSubscription != null) {
            appToAppSubscription.removeListeners();
            appToAppSubscription = null;
        }
    }

    @Override
    public void sendMessage(final String message,
            final ResponseListener<Object> listener) {
        if (message == null || message.isEmpty()) {
            Util.postError(listener, new ServiceCommandError(0, "Cannot send an Empty Message",
                    null));
            return;
        }

        sendP2PMessage(message, listener);
    }

    @Override
    public void sendMessage(final JSONObject message,
            final ResponseListener<Object> listener) {
        if (message == null || message.length() == 0) {
            Util.postError(listener, new ServiceCommandError(0, "Cannot send an Empty Message",
                    null));
            return;
        }

        sendP2PMessage(message, listener);
    }

    private void sendP2PMessage(final Object message,
            final ResponseListener<Object> listener) {
        JSONObject _payload = new JSONObject();

        try {
            _payload.put("type", "p2p");
            _payload.put("to", getFullAppId());
            _payload.put("payload", message);
        } catch (JSONException ex) {
            // do nothing
        }

        if (isConnected()) {
//            socket.sendMessage(_payload, null);

            Util.postSuccess(listener, null);
        } else {
            ResponseListener<Object> connectListener = new ResponseListener<Object>() {

                @Override
                public void onError(ServiceCommandError error) {
                    Util.postError(listener, error);
                }

                @Override
                public void onSuccess(Object any) {
                    sendP2PMessage(message, listener);
                }
            };

            connect(connectListener);
        }
    }

    @Override
    public void close(ResponseListener<Object> listener) {
        mActiveCommands.clear();

        if (mPlayStateSubscription != null) {
            mPlayStateSubscription.unsubscribe();
            mPlayStateSubscription = null;
        }

        if (mMessageSubscription != null) {
            mMessageSubscription.unsubscribe();
            mMessageSubscription = null;
        }

        if (mWebAppPinnedSubscription != null) {
            mWebAppPinnedSubscription.unsubscribe();
            mWebAppPinnedSubscription = null;
        }

        service.getWebAppLauncher().closeWebApp(launchSession, listener);
    }

    @Override
    public void pinWebApp(String webAppId, ResponseListener<Object> listener) {
        service.getWebAppLauncher().pinWebApp(webAppId, listener);
    }

    @Override
    public void unPinWebApp(String webAppId, ResponseListener<Object> listener) {
        service.getWebAppLauncher().unPinWebApp(webAppId, listener);
    }

    @Override
    public void isWebAppPinned(String webAppId, WebAppPinStatusListener listener) {
        service.getWebAppLauncher().isWebAppPinned(webAppId, listener);
    }

    @Override
    public ServiceSubscription<WebAppPinStatusListener> subscribeIsWebAppPinned(
            String webAppId, WebAppPinStatusListener listener) {
        mWebAppPinnedSubscription = service.getWebAppLauncher().subscribeIsWebAppPinned(webAppId,
                listener);
        return mWebAppPinnedSubscription;
    }

    @Override
    public void seek(final long position, ResponseListener<Object> listener) {
        if (position < 0) {
            Util.postError(listener, new ServiceCommandError(0, "Must pass a valid positive value",
                    null));
            return;
        }

        int requestIdNumber = getNextId();
        final String requestId = String.format(Locale.US, "req%d", requestIdNumber);

        JSONObject message = null;
        try {
            message = new JSONObject() {
                {
                    put("contentType", namespaceKey + "mediaCommand");
                    put("mediaCommand", new JSONObject() {
                        {
                            put("type", "seek");
                            put("position", position / 1000);
                            put("requestId", requestId);
                        }
                    });
                }
            };
        } catch (JSONException e) {
            Util.postError(listener, new ServiceCommandError(0, "JSON Parse error", null));
        }

        ServiceCommand command = new ServiceCommand(
                null, null, null, listener);

        mActiveCommands.put(requestId, command);

        sendMessage(message, listener);
    }

    @Override
    public void getPosition(final PositionListener listener) {
        int requestIdNumber = getNextId();
        final String requestId = String.format(Locale.US, "req%d", requestIdNumber);

        JSONObject message = null;
        try {
            message = new JSONObject() {
                {
                    put("contentType", namespaceKey + "mediaCommand");
                    put("mediaCommand", new JSONObject() {
                        {
                            put("type", "getPosition");
                            put("requestId", requestId);
                        }
                    });
                }
            };
        } catch (JSONException e) {
            Util.postError(listener, new ServiceCommandError(0, "JSON Parse error", null));
        }

        ServiceCommand command = new ServiceCommand(
                null, null, null, new ResponseListener<Object>() {

                    @Override
                    public void onSuccess(Object any) {
                        try {
                            long position = ((JSONObject) any).getLong("position");
                            Util.postSuccess(listener, position * 1000);
                        } catch (JSONException e) {
                            this.onError(new ServiceCommandError(0, "JSON Parse error", null));
                        }
                    }

                    @Override
                    public void onError(ServiceCommandError error) {
                        Util.postError(listener, error);
                    }
                });

        mActiveCommands.put(requestId, command);

        sendMessage(message, new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object any) {
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        });
    }

    @Override
    public void getDuration(final DurationListener listener) {
        int requestIdNumber = getNextId();
        final String requestId = String.format(Locale.US, "req%d", requestIdNumber);

        JSONObject message = null;
        try {
            message = new JSONObject() {
                {
                    put("contentType", namespaceKey + "mediaCommand");
                    put("mediaCommand", new JSONObject() {
                        {
                            put("type", "getDuration");
                            put("requestId", requestId);
                        }
                    });
                }
            };
        } catch (JSONException e) {
            Util.postError(listener, new ServiceCommandError(0, "JSON Parse error", null));
        }

        ServiceCommand command = new ServiceCommand(
                null, null, null, new ResponseListener<Object>() {

                    @Override
                    public void onSuccess(Object any) {
                        try {
                            long position = ((JSONObject) any).getLong("duration");
                            Util.postSuccess(listener, position * 1000);
                        } catch (JSONException e) {
                            Util.postError(listener, new ServiceCommandError(0, "JSON Parse error",
                                    null));
                        }
                    }

                    @Override
                    public void onError(ServiceCommandError error) {
                        Util.postError(listener, error);
                    }
        });

        mActiveCommands.put(requestId, command);

        sendMessage(message, new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object any) {
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        });
    }

    @Override
    public void getPlayState(final PlayStateListener listener) {
        int requestIdNumber = getNextId();
        final String requestId = String.format(Locale.US, "req%d", requestIdNumber);

        JSONObject message = null;
        try {
            message = new JSONObject() {
                {
                    put("contentType", namespaceKey + "mediaCommand");
                    put("mediaCommand", new JSONObject() {
                        {
                            put("type", "getPlayState");
                            put("requestId", requestId);
                        }
                    });
                }
            };
        } catch (JSONException e) {
            Util.postError(listener, new ServiceCommandError(0, "JSON Parse error", null));
        }

        ServiceCommand command = new ServiceCommand(
                null, null, null, new ResponseListener<Object>() {

                    @Override
                    public void onSuccess(Object any) {
                        try {
                            String playStateString = ((JSONObject) any)
                                    .getString("playState");
                            PlayStateStatus playState = parsePlayState(playStateString);
                            Util.postSuccess(listener, playState);
                        } catch (JSONException e) {
                            this.onError(new ServiceCommandError(0,
                                    "JSON Parse error", null));
                        }
                    }

                    @Override
                    public void onError(ServiceCommandError error) {
                        Util.postError(listener, error);
                    }
                });

        mActiveCommands.put(requestId, command);

        sendMessage(message, new ResponseListener<Object>() {

            @Override
            public void onSuccess(Object any) {
            }

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }
        });
    }

    @Override
    public ServiceSubscription<PlayStateListener> subscribePlayState(
            final PlayStateListener listener) {
        if (mPlayStateSubscription == null)
            mPlayStateSubscription = new URLServiceSubscription<>(
                    null, null, null, null);

        if (!connected) {
            connect(new ResponseListener<Object>() {

                @Override
                public void onError(ServiceCommandError error) {
                    Util.postError(listener, error);
                }

                @Override
                public void onSuccess(Object any) {
                }
            });
        }

        if (!mPlayStateSubscription.getListeners().contains(listener))
            mPlayStateSubscription.addListener(listener);

        return mPlayStateSubscription;
    }

    /*****************
     * Media Control *
     *****************/
    @Override
    public MediaControl getMediaControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getMediaControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    /****************
     * Media Player *
     ****************/
    @Override
    public MediaPlayer getMediaPlayer() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getMediaPlayerCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void displayImage(final String url, final String mimeType,
            final String title, final String description, final String iconSrc,
            final MediaPlayer.LaunchListener listener) {
        int requestIdNumber = getNextId();
        final String requestId = String.format(Locale.US, "req%d", requestIdNumber);

        JSONObject message;
        try {
            message = new JSONObject() {
                {
                    putOpt("contentType", namespaceKey + "mediaCommand");
                    putOpt("mediaCommand", new JSONObject() {
                        {
                            putOpt("type", "displayImage");
                            putOpt("mediaURL", url);
                            putOpt("iconURL", iconSrc);
                            putOpt("title", title);
                            putOpt("description", description);
                            putOpt("mimeType", mimeType);
                            putOpt("requestId", requestId);
                        }
                    });
                }
            };
        } catch (JSONException e) {
            Util.postError(listener, new ServiceCommandError(0, "JSON Parse error", null));
            return;
        }

        ServiceCommand command = getCommand(listener);

        mActiveCommands.put(requestId, command);

        sendP2PMessage(message, new ResponseListener<Object>() {

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }

            @Override
            public void onSuccess(Object any) {
            }
        });
    }

    @NonNull
    private ServiceCommand getCommand(MediaPlayer.LaunchListener listener) {
        ResponseListener<Object> response = new ResponseListener<Object>() {

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }

            @Override
            public void onSuccess(Object any) {
                Util.postSuccess(listener, new MediaLaunchObject(launchSession, getMediaControl()));
            }
        };

        return new ServiceCommand(null, null, null, response);
    }

    @Override
    public void displayImage(MediaInfo mediaInfo, MediaPlayer.LaunchListener listener) {
        String mediaUrl = null;
        String mimeType = null;
        String title = null;
        String desc = null;
        String iconSrc = null;

        if (mediaInfo != null) {
            mediaUrl = mediaInfo.getUrl();
            mimeType = mediaInfo.getMimeType();
            title = mediaInfo.getTitle();
            desc = mediaInfo.getDescription();

            if (mediaInfo.getImages() != null && !mediaInfo.getImages().isEmpty()) {
                ImageInfo imageInfo = mediaInfo.getImages().get(0);
                iconSrc = imageInfo.getUrl();
            }
        }

        displayImage(mediaUrl, mimeType, title, desc, iconSrc, listener);
    }

    @Override
    public void playMedia(String url, String mimeType, String title, String description,
                          String iconSrc, boolean shouldLoop, MediaPlayer.LaunchListener listener) {
        MediaInfo mediaInfo = new MediaInfo.Builder(url, mimeType)
                .setTitle(title)
                .setDescription(description)
                .setIcon(iconSrc)
                .build();
        playMedia(mediaInfo, shouldLoop, listener);
    }

    @Override
    public void playMedia(final MediaInfo mediaInfo,
                          final boolean shouldLoop, final MediaPlayer.LaunchListener listener) {
        int requestIdNumber = getNextId();
        final String requestId = String.format(Locale.US, "req%d", requestIdNumber);
        JSONObject message;
        ImageInfo iconImage = null;
        List<ImageInfo> images = mediaInfo.getImages();

        if (images != null && !images.isEmpty()) {
            iconImage = images.get(0);
        }

        final String iconSrc = iconImage == null ? null : iconImage.getUrl();
        final SubtitleInfo subtitleInfo = mediaInfo.getSubtitleInfo();

        try {
            message = createPlayMediaJsonRequest(mediaInfo, shouldLoop, requestId, iconSrc,
                    subtitleInfo);
        } catch (JSONException e) {
            Util.postError(listener, new ServiceCommandError(0, "JSON Parse error", null));
            return;
        }

        ServiceCommand command = getServiceCommand(listener);

        mActiveCommands.put(requestId, command);

        sendMessage(message, new ResponseListener<Object>() {

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }

            @Override
            public void onSuccess(Object any) {
            }
        });
    }

    @NonNull
    private ServiceCommand getServiceCommand(MediaPlayer.LaunchListener listener) {
        ResponseListener<Object> response = new ResponseListener<Object>() {

            @Override
            public void onError(ServiceCommandError error) {
                Util.postError(listener, error);
            }

            @Override
            public void onSuccess(Object any) {
                Util.postSuccess(listener, new MediaLaunchObject(launchSession, getMediaControl(), getPlaylistControl()));
            }
        };

        return new ServiceCommand(null, null, null, response);
    }

    @NonNull
    private JSONObject createPlayMediaJsonRequest(final MediaInfo mediaInfo, final boolean
            shouldLoop, final String requestId, final String iconSrc, final SubtitleInfo
            subtitleInfo) throws JSONException {
        return new JSONObject() {{
            putOpt("contentType", namespaceKey + "mediaCommand");
            putOpt("mediaCommand", new JSONObject() {{
                putOpt("type", "playMedia");
                putOpt("mediaURL", mediaInfo.getUrl());
                putOpt("iconURL", iconSrc);
                putOpt("title", mediaInfo.getTitle());
                putOpt("description", mediaInfo.getDescription());
                putOpt("mimeType", mediaInfo.getMimeType());
                putOpt("shouldLoop", shouldLoop);
                putOpt("requestId", requestId);
                if (subtitleInfo != null) {
                    putOpt("subtitles", new JSONObject() {{
                        putOpt("default", ENABLED_SUBTITLE_ID);
                        putOpt("enabled", ENABLED_SUBTITLE_ID);
                        putOpt("tracks", new JSONArray() {{
                            put(new JSONObject() {{
                                putOpt("id", ENABLED_SUBTITLE_ID);
                                putOpt("language", subtitleInfo.getLanguage());
                                putOpt("source", subtitleInfo.getUrl());
                                putOpt("label", subtitleInfo.getLabel());
                            }});
                        }});
                    }});
                }
            }});
        }};
    }

    /****************
     * Playlist Control *
     ****************/
    public PlaylistControl getPlaylistControl() {
        return this;
    }

    @Override
    public CapabilityPriorityLevel getPlaylistControlCapabilityLevel() {
        return CapabilityPriorityLevel.HIGH;
    }

    @Override
    public void jumpToTrack(final long index, final ResponseListener<Object> listener) {
        int requestIdNumber = getNextId();
        final String requestId = String.format(Locale.US, "req%d", requestIdNumber);

        JSONObject message;
        try {
            message = new JSONObject() {
                {
                    put("contentType", namespaceKey + "mediaCommand");
                    put("mediaCommand", new JSONObject() {
                        {
                            put("type", "jumpToTrack");
                            put("requestId", requestId);
                            put("index", (int)index);
                        }
                    });
                }
            };
        } catch (JSONException e) {
            Util.postError(listener, new ServiceCommandError(0, "JSON Parse error", null));
            return;
        }

        ServiceCommand command =
                new ServiceCommand(null, null, null, listener);
        mActiveCommands.put(requestId, command);
        sendMessage(message, listener);
    }

    @Override
    public void previous(final ResponseListener<Object> listener) {
        int requestIdNumber = getNextId();
        final String requestId = String.format(Locale.US, "req%d", requestIdNumber);

        JSONObject message;
        try {
            message = new JSONObject() {
                {
                    put("contentType", namespaceKey + "mediaCommand");
                    put("mediaCommand", new JSONObject() {
                        {
                            put("type", "playPrevious");
                            put("requestId", requestId);
                        }
                    });
                }
            };
        } catch (JSONException e) {
            Util.postError(listener, new ServiceCommandError(0, "JSON Parse error", null));
            return;
        }

        ServiceCommand command =
                new ServiceCommand(null, null, null, listener);
        mActiveCommands.put(requestId, command);
        sendMessage(message, listener);
    }

    @Override
    public void next(final ResponseListener<Object> listener) {
        int requestIdNumber = getNextId();
        final String requestId = String.format(Locale.US, "req%d", requestIdNumber);

        JSONObject message;
        try {
            message = new JSONObject() {
                {
                    put("contentType", namespaceKey + "mediaCommand");
                    put("mediaCommand", new JSONObject() {
                        {
                            put("type", "playNext");
                            put("requestId", requestId);
                        }
                    });
                }
            };
        } catch (JSONException e) {
            Util.postError(listener, new ServiceCommandError(0, "JSON Parse error", null));
            return;
        }

        ServiceCommand command =
                new ServiceCommand(null, null, null, listener);
        mActiveCommands.put(requestId, command);
        sendMessage(message, listener);
    }
}