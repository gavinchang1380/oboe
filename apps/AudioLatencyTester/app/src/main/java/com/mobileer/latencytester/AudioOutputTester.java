/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobileer.latencytester;

import android.util.Log;

public class AudioOutputTester extends AudioStreamTester {

    protected AudioOutputStream mAudioOutputStream;

    private static AudioOutputTester mInstance;

    public static synchronized AudioOutputTester getInstance() {
        if (mInstance == null) {
            mInstance = new AudioOutputTester();
        }
        return mInstance;
    }

    public static synchronized void release() {
        mInstance = null;
    }

    private AudioOutputTester() {
        super();
        Log.i(TapToToneActivity.TAG, "create OboeAudioOutputStream ---------");
        if (TestAudioActivity.isUseJavaInterface()) {
            mCurrentAudioStream = new JavaAudioOutputStream();
        } else {
            mCurrentAudioStream = new OboeAudioOutputStream();
        }
        mAudioOutputStream = (AudioOutputStream)mCurrentAudioStream;
        requestedConfiguration.setDirection(StreamConfiguration.DIRECTION_OUTPUT);
    }

    public void trigger() {
        mAudioOutputStream.trigger();
    }

    public void setChannelEnabled(int channelIndex, boolean enabled)  {
        mAudioOutputStream.setChannelEnabled(channelIndex, enabled);
    }

    public void setSignalType(int type) {
        mAudioOutputStream.setSignalType(type);
    }

    public int getLastErrorCallbackResult() {return mCurrentAudioStream.getLastErrorCallbackResult();};

    public long getFramesRead() {return mCurrentAudioStream.getFramesRead();};
}
