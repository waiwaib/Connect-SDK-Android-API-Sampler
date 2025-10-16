/*
 * WebOSTVMouseSocketConnection
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on 19 Jan 2014
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

package com.connectsdk.service.webos;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;


import android.util.Log;

import com.connectsdk.core.Util;

/** @noinspection ALL*/
public class WebOSTVMouseSocketConnection {
    public interface WebOSTVMouseSocketListener {
        void onConnected();
    }

    String socketPath;
    WebOSTVMouseSocketListener listener;
    WebOSTVTrustManager customTrustManager;

    public enum ButtonType {
        HOME,
        BACK,
        UP,
        DOWN,
        LEFT,
        RIGHT,
    }

    public WebOSTVMouseSocketConnection(String socketPath, WebOSTVMouseSocketListener listener) {
        Log.d("PtrAndKeyboardFragment", "got socketPath: " + socketPath);

        this.listener = listener; 
        this.socketPath = socketPath;

        try {
            URI uri = new URI(this.socketPath);
            connectPointer(uri);
        } catch (URISyntaxException e) {
            Log.e(Util.T,e.getMessage(),e);
        }
    }

    public void connectPointer(URI uri) {

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            customTrustManager = new WebOSTVTrustManager();
            sslContext.init(null, new WebOSTVTrustManager[] {customTrustManager}, null);
        } catch (KeyException | NoSuchAlgorithmException | RuntimeException e) {
            Log.d(Util.T, "Failed to create SSLContext",e);
        }
    }

    public void disconnect() {

    }

    public boolean isConnected() {
        return false;
    }

    public void click() {
    }

    public void button(ButtonType type) {
        String keyName; 
        switch (type) {
        case HOME:
            keyName = "HOME";
            break;
        case BACK:
            keyName = "BACK";
            break;
        case UP:
            keyName = "UP";
            break;
        case DOWN:
            keyName = "DOWN";
            break;
        case LEFT:
            keyName = "LEFT";
            break;
        case RIGHT:
            keyName = "RIGHT";
            break;

        default:
            keyName = "NONE";
            break;
        }

        button(keyName);
    }

    public void button(String keyName) {

    }

    public void move(double dx, double dy) {

    }

    public void move(double dx, double dy, boolean drag) {

    }

    public void scroll(double dx, double dy) {

    }
}