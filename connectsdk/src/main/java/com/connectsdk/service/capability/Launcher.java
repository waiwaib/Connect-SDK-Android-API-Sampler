/*
 * Launcher
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
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.sessions.LaunchSession;

import java.util.List;

public interface Launcher extends CapabilityMethods {
    String Any = "Launcher.Any";

    String Application = "Launcher.App";
    String Application_Params = "Launcher.App.Params";
    String Application_Close = "Launcher.App.Close";
    String Application_List = "Launcher.App.List";
    String Browser = "Launcher.Browser";
    String Browser_Params = "Launcher.Browser.Params";
    String Hulu = "Launcher.Hulu";
    String Hulu_Params = "Launcher.Hulu.Params";
    String Netflix = "Launcher.Netflix";
    String Netflix_Params = "Launcher.Netflix.Params";
    String YouTube = "Launcher.YouTube";
    String YouTube_Params = "Launcher.YouTube.Params";
    String AppStore = "Launcher.AppStore";
    String AppStore_Params = "Launcher.AppStore.Params";
    String AppState = "Launcher.AppState";
    String AppState_Subscribe = "Launcher.AppState.Subscribe";
    String RunningApp = "Launcher.RunningApp";
    String RunningApp_Subscribe = "Launcher.RunningApp.Subscribe";

    String[] Capabilities = {
            Application,
            Application_Params,
            Application_Close,
            Application_List,
            Browser,
            Browser_Params,
            Hulu,
            Hulu_Params,
            Netflix,
            Netflix_Params,
            YouTube,
            YouTube_Params,
            AppStore,
            AppStore_Params,
            AppState,
            AppState_Subscribe,
            RunningApp,
            RunningApp_Subscribe
    };

    Launcher getLauncher();

    CapabilityPriorityLevel getLauncherCapabilityLevel();

    void launchAppWithInfo(AppInfo appInfo, AppLaunchListener listener);

    void launchAppWithInfo(AppInfo appInfo, Object params, AppLaunchListener listener);

    void launchApp(String appId, AppLaunchListener listener);

    void closeApp(LaunchSession launchSession, ResponseListener<Object> listener);

    void getAppList(AppListListener listener);

    void getRunningApp(AppInfoListener listener);

    ServiceSubscription<AppInfoListener> subscribeRunningApp(AppInfoListener listener);

    void getAppState(LaunchSession launchSession, AppStateListener listener);

    ServiceSubscription<AppStateListener> subscribeAppState(LaunchSession launchSession, AppStateListener listener);

    void launchBrowser(String url, AppLaunchListener listener);

    void launchYouTube(String contentId, AppLaunchListener listener);

    void launchYouTube(String contentId, float startTime, AppLaunchListener listener);

    void launchNetflix(String contentId, AppLaunchListener listener);

    void launchHulu(String contentId, AppLaunchListener listener);

    void launchAppStore(String appId, AppLaunchListener listener);

    /**
     * Success listener that is called upon successfully launching an app.
     * <p>
     * Passes a LaunchSession Object containing important information about the app's launch session
     */
    interface AppLaunchListener extends ResponseListener<LaunchSession> {
    }

    /**
     * Success listener that is called upon requesting info about the current running app.
     * <p>
     * Passes an AppInfo object containing info about the running app
     */
    interface AppInfoListener extends ResponseListener<AppInfo> {
    }

    /**
     * Success block that is called upon successfully getting the app list.
     * <p>
     * Passes a List containing an AppInfo for each available app on the device
     */
    interface AppListListener extends ResponseListener<List<AppInfo>> {
    }

    interface AppCountListener extends ResponseListener<Integer> {
    }

    /**
     * Success block that is called upon successfully getting an app's state.
     * <p>
     * Passes an AppState object which contains information about the running app.
     */
    interface AppStateListener extends ResponseListener<AppState> {
    }

    /**
     * Helper class used with the AppStateListener to return the current state of an app.
     */
    class AppState {
        /**
         * Whether the app is currently running.
         */
        public boolean running;
        /**
         * Whether the app is currently visible.
         */
        public boolean visible;

        public AppState(boolean running, boolean visible) {
            this.running = running;
            this.visible = visible;
        }
    }
}