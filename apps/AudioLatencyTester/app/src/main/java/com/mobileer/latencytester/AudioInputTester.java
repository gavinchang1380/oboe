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

class AudioInputTester extends AudioStreamTester{
    private static AudioInputTester mInstance;

    private AudioInputTester() {
        super();
        Log.i(TapToToneActivity.TAG, "create OboeAudioStream ---------");

        mCurrentAudioStream = new OboeAudioInputStream();
        requestedConfiguration.setDirection(StreamConfiguration.DIRECTION_INPUT);
    }

    public static synchronized AudioInputTester getInstance() {
        if (mInstance == null) {
            mInstance = new AudioInputTester();
        }
        return mInstance;
    }

    public native double getPeakLevel(int i);
}