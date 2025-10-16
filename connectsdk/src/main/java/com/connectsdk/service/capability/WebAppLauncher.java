/*
 * WebAppLauncher
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

package com.connectsdk.service.capability;

import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.sessions.LaunchSession;
import com.connectsdk.service.sessions.WebAppSession.LaunchListener;
import com.connectsdk.service.sessions.WebAppSession.WebAppPinStatusListener;

import org.json.JSONObject;


public interface WebAppLauncher extends CapabilityMethods {
    String Any = "WebAppLauncher.Any";

    String Launch = "WebAppLauncher.Launch";
    String Launch_Params = "WebAppLauncher.Launch.Params";
    String Message_Send = "WebAppLauncher.Message.Send";
    String Message_Receive = "WebAppLauncher.Message.Receive";
    String Message_Send_JSON = "WebAppLauncher.Message.Send.JSON";
    String Message_Receive_JSON = "WebAppLauncher.Message.Receive.JSON";
    String Connect = "WebAppLauncher.Connect";
    String Disconnect = "WebAppLauncher.Disconnect";
    String Join = "WebAppLauncher.Join";
    String Close = "WebAppLauncher.Close";
    String Pin = "WebAppLauncher.Pin";

    String[] Capabilities = {
            Launch,
            Launch_Params,
            Message_Send,
            Message_Receive,
            Message_Send_JSON,
            Message_Receive_JSON,
            Connect,
            Disconnect,
            Join,
            Close,
            Pin
    };

    WebAppLauncher getWebAppLauncher();

    CapabilityPriorityLevel getWebAppLauncherCapabilityLevel();

    void launchWebApp(String webAppId, LaunchListener listener);

    void launchWebApp(String webAppId, boolean relaunchIfRunning, LaunchListener listener);

    void launchWebApp(String webAppId, JSONObject params, LaunchListener listener);

    void launchWebApp(String webAppId, JSONObject params, boolean relaunchIfRunning, LaunchListener listener);

    void joinWebApp(LaunchSession webAppLaunchSession, LaunchListener listener);

    void joinWebApp(String webAppId, LaunchListener listener);

    void closeWebApp(LaunchSession launchSession, ResponseListener<Object> listener);

    void pinWebApp(String webAppId, ResponseListener<Object> listener);

    void unPinWebApp(String webAppId, ResponseListener<Object> listener);

    void isWebAppPinned(String webAppId, WebAppPinStatusListener listener);

    ServiceSubscription<WebAppPinStatusListener> subscribeIsWebAppPinned(String webAppId, WebAppPinStatusListener listener);
}