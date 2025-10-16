/*
 * Util
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Jeffrey Glenn on 27 Feb 2014
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

package com.connectsdk.core;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;

import com.connectsdk.service.capability.listeners.ErrorListener;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;

public final class Util {
    static public String T = "Connect SDK";

    static private Handler handler;

    static private final int NUM_OF_THREADS = 20;

    static private Executor executor;

    static {
        createExecutor();
    }

    static void createExecutor() {
        Util.executor = Executors.newFixedThreadPool(NUM_OF_THREADS, r -> {
            Thread th = new Thread(r);
            th.setName("2nd Screen BG");
            return th;
        });
    }

    public static void runOnUI(Runnable runnable) {
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }

        handler.post(runnable);
    }

    public static void runInBackground(Runnable runnable, boolean forceNewThread) {
        if (forceNewThread || isMain()) {
            executor.execute(runnable);
        } else {
            runnable.run();
        }

    }

    public static void runInBackground(Runnable runnable) {
        runInBackground(runnable, false);
    }

    public static Executor getExecutor() {
        return executor;
    }

    public static boolean isMain() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public static <T> void postSuccess(final ResponseListener<T> listener, final T object) {
        if (listener == null)
            return;

        Util.runOnUI(() -> listener.onSuccess(object));
    }

    public static void postError(final ErrorListener listener, final ServiceCommandError error) {
        if (listener == null)
            return;

        Util.runOnUI(() -> listener.onError(error));
    }

    public static byte[] convertIpAddress(int ip) {
        return new byte[] {
                (byte) (ip & 0xFF), 
                (byte) ((ip >> 8) & 0xFF), 
                (byte) ((ip >> 16) & 0xFF), 
                (byte) ((ip >> 24) & 0xFF)};
    }

    public static long getTime() {
        return TimeUnit.MILLISECONDS.toSeconds(new Date().getTime());
    }


    public static InetAddress getIpAddress(Context context) throws UnknownHostException {
        WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();

        if (ip == 0) {
            return null;
        }
        else {
            byte[] ipAddress = convertIpAddress(ip);
            return InetAddress.getByAddress(ipAddress);
        }
    }

    // IPv4 地址正则
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^" +
                    "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
                    "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

    // 简化版 IPv6 地址匹配
    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "^" +
                    "(" + // Start of main group
                    "([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}" + // 1:2:3:4:5:6:7:8
                    "|" +
                    "([0-9a-fA-F]{1,4}:){1,7}:" +              // ::1, ::2:3:4:...
                    "|" +
                    "([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}" + // 1::200
                    "|" +
                    "([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}" +
                    "|" +
                    "([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}" +
                    "|" +
                    "([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}" +
                    "|" +
                    "([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}" +
                    "|" +
                    "[0-9a-fA-F]{1,4}:(:[0-9a-fA-F]{1,4}){1,6}" +
                    "|:((:[0-9a-fA-F]{1,4}){1,7}|:)" +
                    ")" +
                    "(%[0-9a-zA-Z]{1,})?" + // zone index
                    "$");

    public static boolean isInetAddress(String ip) {
        return isIPv4Address(ip) || isIPv6Address(ip);
    }

    public static boolean isIPv4Address(String ip) {
        if (ip == null) {
            return false;
        }
        Matcher m = IPV4_PATTERN.matcher(ip);
        return m.matches();
    }

    public static boolean isIPv6Address(String ip) {
        if (ip == null) {
            return false;
        }

        // 去掉 zone index
        ip = ip.split("%", 2)[0];

        Matcher m = IPV6_PATTERN.matcher(ip);
        return m.matches();
    }
}