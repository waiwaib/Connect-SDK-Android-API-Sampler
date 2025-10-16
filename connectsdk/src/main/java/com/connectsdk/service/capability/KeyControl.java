/*
 * KeyControl
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

public interface KeyControl extends CapabilityMethods {
    String Any = "KeyControl.Any";

    String Up = "KeyControl.Up";
    String Down = "KeyControl.Down";
    String Left = "KeyControl.Left";
    String Right = "KeyControl.Right";
    String OK = "KeyControl.OK";
    String Back = "KeyControl.Back";
    String Home = "KeyControl.Home";
    String Send_Key = "KeyControl.SendKey";
    String KeyCode = "KeyControl.KeyCode";

    public enum KeyCode {
        NUM_0(0),
        NUM_1(1),
        NUM_2(2),
        NUM_3(3),
        NUM_4(4),
        NUM_5(5),
        NUM_6(6),
        NUM_7(7),
        NUM_8(8),
        NUM_9(9),

        DASH(10),
        ENTER(11);

        private final int code;

        private static final KeyCode[] codes = {
                NUM_0, NUM_1, NUM_2, NUM_3, NUM_4, NUM_5, NUM_6, NUM_7, NUM_8, NUM_9, DASH, ENTER
        };

        KeyCode(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static KeyCode createFromInteger(int keyCode) {
            if (keyCode >= 0 && keyCode < codes.length) {
                return codes[keyCode];
            }
            return null;
        }
    }

    public final static String[] Capabilities = {
            Up,
            Down,
            Left,
            Right,
            OK,
            Back,
            Home,
            KeyCode,
    };

    KeyControl getKeyControl();

    CapabilityPriorityLevel getKeyControlCapabilityLevel();

    void up(ResponseListener<Object> listener);

    void down(ResponseListener<Object> listener);

    void left(ResponseListener<Object> listener);

    void right(ResponseListener<Object> listener);

    void ok(ResponseListener<Object> listener);

    void back(ResponseListener<Object> listener);

    void home(ResponseListener<Object> listener);

    void sendKeyCode(KeyCode keycode, ResponseListener<Object> listener);
}