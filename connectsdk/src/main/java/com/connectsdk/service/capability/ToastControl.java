/*
 * ToastControl
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

import com.connectsdk.core.AppInfo;
import com.connectsdk.service.capability.listeners.ResponseListener;

import org.json.JSONObject;

public interface ToastControl extends CapabilityMethods {
    String Any = "ToastControl.Any";

    String Show_Toast = "ToastControl.Show";
    String Show_Clickable_Toast_App = "ToastControl.Show.Clickable.App";
    String Show_Clickable_Toast_App_Params = "ToastControl.Show.Clickable.App.Params";
    String Show_Clickable_Toast_URL = "ToastControl.Show.Clickable.URL";

    String[] Capabilities = {
            Show_Toast,
            Show_Clickable_Toast_App,
            Show_Clickable_Toast_App_Params,
            Show_Clickable_Toast_URL
    };

    ToastControl getToastControl();

    CapabilityPriorityLevel getToastControlCapabilityLevel();

    void showToast(String message, ResponseListener<Object> listener);

    void showToast(String message, String iconData, String iconExtension, ResponseListener<Object> listener);

    void showClickableToastForApp(String message, AppInfo appInfo, JSONObject params, ResponseListener<Object> listener);

    void showClickableToastForApp(String message, AppInfo appInfo, JSONObject params, String iconData, String iconExtension, ResponseListener<Object> listener);

    void showClickableToastForURL(String message, String url, ResponseListener<Object> listener);

    void showClickableToastForURL(String message, String url, String iconData, String iconExtension, ResponseListener<Object> listener);
}