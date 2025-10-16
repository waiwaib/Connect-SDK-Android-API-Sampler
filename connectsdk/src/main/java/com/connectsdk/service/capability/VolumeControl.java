/*
 * VolumeControl
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

public interface VolumeControl extends CapabilityMethods {
    String Any = "VolumeControl.Any";

    String Volume_Get = "VolumeControl.Get";
    String Volume_Set = "VolumeControl.Set";
    String Volume_Up_Down = "VolumeControl.UpDown";
    String Volume_Subscribe = "VolumeControl.Subscribe";
    String Mute_Get = "VolumeControl.Mute.Get";
    String Mute_Set = "VolumeControl.Mute.Set";
    String Mute_Subscribe = "VolumeControl.Mute.Subscribe";

    String[] Capabilities = {
            Volume_Get,
            Volume_Set,
            Volume_Up_Down,
            Volume_Subscribe,
            Mute_Get,
            Mute_Set,
            Mute_Subscribe
    };

    VolumeControl getVolumeControl();

    CapabilityPriorityLevel getVolumeControlCapabilityLevel();

    void volumeUp(ResponseListener<Object> listener);

    void volumeDown(ResponseListener<Object> listener);

    void setVolume(float volume, ResponseListener<Object> listener);

    void getVolume(VolumeListener listener);

    void setMute(boolean isMute, ResponseListener<Object> listener);

    void getMute(MuteListener listener);

    ServiceSubscription<VolumeListener> subscribeVolume(VolumeListener listener);

    ServiceSubscription<MuteListener> subscribeMute(MuteListener listener);

    /**
     * Success block that is called upon successfully getting the device's system volume.
     * <p>
     * Passes the current system volume, value is a float between 0.0 and 1.0
     */
    interface VolumeListener extends ResponseListener<Float> {
    }

    /**
     * Success block that is called upon successfully getting the device's system mute status.
     * <p>
     * Passes current system mute status
     */
     interface MuteListener extends ResponseListener<Boolean> {
    }

    /**
     * Success block that is called upon successfully getting the device's system volume status.
     * <p>
     * Passes current system mute status
     */
    interface VolumeStatusListener extends ResponseListener<VolumeStatus> {
    }

    /**
     * Helper class used with the VolumeControl.Volume StatusListener to return the current volume status.
     */
    class VolumeStatus {
        public boolean isMute;
        public float volume;

        public VolumeStatus(boolean isMute, float volume) {
            this.isMute = isMute;
            this.volume = volume;
        }
    }
}