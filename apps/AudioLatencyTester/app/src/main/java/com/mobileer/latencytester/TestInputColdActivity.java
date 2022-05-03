/*
 * Copyright 2015 The Android Open Source Project
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

import static com.mobileer.latencytester.MidiTapTester.NoteListener;

import android.content.pm.PackageManager;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mobileer.miditools.MidiOutputPortConnectionSelector;
import com.mobileer.miditools.MidiPortConnector;
import com.mobileer.miditools.MidiTools;

import java.io.IOException;

public class TestInputColdActivity extends TestAudioActivity {
    protected AudioInputTester mAudioInputTester;

    public static final int CLOSE_TEST_MSEC = 1000;
    public static final int ENABLE_TEST_MSEC = 3500;

    // Names from obsolete version of Oboetester.
    public static final String OLD_PRODUCT_NAME = "AudioLatencyTester";
    public static final String OLD_MANUFACTURER_NAME = "AndroidTest";
    public static final long POLL_DURATION_MILLIS = 1;

    private MidiManager mMidiManager;
    private MidiInputPort mInputPort;

    protected MidiTapTester mMidiTapTester;

    private Button mTriggerButton;
    private TextView mHelpView;
    private TextView mColdTimeView;

    private MidiOutputPortConnectionSelector mPortSelector;
    private final MyNoteListener mTestListener = new MyNoteListener();

    private Handler mHandler;

    @Override
    protected void inflateActivity() {
        setContentView(R.layout.activity_test_input_cold);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAudioInputTester = addAudioInputTester();

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_MIDI)) {
            setupMidi();
        } else {
            Toast.makeText(TestInputColdActivity.this,
                    "MIDI not supported!", Toast.LENGTH_LONG)
                    .show();
        }

        mTriggerButton = (Button) findViewById(R.id.button_trigger);
        mHandler = new Handler(Looper.getMainLooper());

        mHelpView = (TextView)findViewById(R.id.helpView);
        mColdTimeView = (TextView)findViewById(R.id.coldTimeView);

        WorkloadView mWorkloadView = (WorkloadView) findViewById(R.id.workload_view);
        if (mWorkloadView != null) {
            mWorkloadView.setAudioStreamTester(mAudioInputTester);
        }

        BufferSizeView bufferSizeView = findViewById(R.id.buffer_size_view);
        bufferSizeView.setVisibility(View.GONE);
        mHelpView.setText("1、先对Mic吹气，然后点击Trigger按钮，此时获取信号值时间为冷启动时延；\n2、因为部分手机有缓存，建议正式测试前前测试一遍确保提示超时！");

        updateButtons(true);
        updateEnabledWidgets();
    }

    private void updateButtons(boolean running) {
        mTriggerButton.setEnabled(running);
    }

    @Override
    int getActivityType() {
        return ACTIVITY_TEST_INPUT;
    }

    @Override
    protected void onDestroy() {
        mMidiTapTester.removeTestListener(mTestListener);
        closeMidiResources();
        super.onDestroy();
    }

    @Override boolean isOutput() { return false; }

    public void setupAEC(int sessionId) {
        AcousticEchoCanceler effect =  AcousticEchoCanceler.create(sessionId);
    }

    @Override
    public void setupEffects(int sessionId) {
        setupAEC(sessionId);
    }

    private void setupMidi() {
        // Setup MIDI
        mMidiManager = (MidiManager) getSystemService(MIDI_SERVICE);
        MidiDeviceInfo[] infos = mMidiManager.getDevices();

        // Warn if old version of OboeTester found.
        for (MidiDeviceInfo info : infos) {
            Log.i(TAG, "MIDI info = " + info);
            Bundle properties = info.getProperties();
            String product = properties
                    .getString(MidiDeviceInfo.PROPERTY_PRODUCT);
            String manufacturer = properties
                    .getString(MidiDeviceInfo.PROPERTY_MANUFACTURER);
            if (OLD_PRODUCT_NAME.equals(product) && OLD_MANUFACTURER_NAME.equals(manufacturer)) {
                showErrorToast("Please uninstall old version of OboeTester.");
                break;
            }
        }

        // Open the port now so that the MidiTapTester gets created.
        for (MidiDeviceInfo info : infos) {
            Bundle properties = info.getProperties();
            String product = properties
                    .getString(MidiDeviceInfo.PROPERTY_PRODUCT);
            if (MidiTapTester.PRODUCT_NAME.equals(product)) {
                String manufacturer = properties
                        .getString(MidiDeviceInfo.PROPERTY_MANUFACTURER);
                if (MidiTapTester.MANUFACTURER_NAME.equals(manufacturer)) {
                    openPortTemporarily(info);
                    break;
                }
            }
        }
    }

    // These should only be set after mAudioMidiTester is set.
    private void setSpinnerListeners() {
        MidiDeviceInfo synthInfo = MidiTools.findDevice(mMidiManager, MidiTapTester.MANUFACTURER_NAME,
                MidiTapTester.PRODUCT_NAME);
        Log.i(TAG, "found tester virtual device info: " + synthInfo);
        int portIndex = 0;
        mPortSelector = new MidiOutputPortConnectionSelector(mMidiManager, this,
                R.id.spinner_synth_sender, synthInfo, portIndex);
        mPortSelector.setConnectedListener(new MyPortsConnectedListener());

    }

    private class MyNoteListener implements NoteListener {
        @Override
        public void onNoteOn(final int pitch) {
            runOnUiThread(() -> {
                if (mTriggerButton.isEnabled()) {
                    mTriggerButton.callOnClick();
                    mStreamContexts.get(0).configurationView.setStatusText("MIDI pitch = " + pitch);
                }
            });
        }
    }

    private void openPortTemporarily(final MidiDeviceInfo info) {
        Log.i(TAG, "MIDI openPort() info = " + info);
        mMidiManager.openDevice(info, device -> {
            if (device == null) {
                Log.e(TAG, "could not open device " + info);
            } else {
                mInputPort = device.openInputPort(0);
                Log.i(TAG, "opened MIDI port = " + mInputPort + " on " + info);
                mMidiTapTester = MidiTapTester.getInstanceOrNull();
                if (mMidiTapTester == null) {
                    Log.e(TAG, "MidiTapTester Service was not created! info = " + info);
                    showErrorToast("MidiTapTester Service was not created!");
                } else {
                    Log.i(TAG, "openPort() mMidiTapTester = " + mMidiTapTester);
                    // Now that we have created the MidiTapTester, close the port so we can
                    // open it later.
                    try {
                        mInputPort.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mMidiTapTester.addTestListener(mTestListener);
                    setSpinnerListeners();
                }
            }
        }, new Handler(Looper.getMainLooper())
        );
    }

    // TODO Listen to the synth server
    // for open/close events and then disable/enable the spinner.
    private class MyPortsConnectedListener
            implements MidiPortConnector.OnPortsConnectedListener {
        @Override
        public void onPortsConnected(final MidiDevice.MidiConnection connection) {
            Log.i(TAG, "onPortsConnected, connection = " + connection);
            runOnUiThread(() -> {
                if (connection == null) {
                    Toast.makeText(TestInputColdActivity.this,
                            R.string.error_port_busy, Toast.LENGTH_LONG)
                            .show();
                    mPortSelector.clearSelection();
                } else {
                    Toast.makeText(TestInputColdActivity.this,
                            R.string.port_open_ok, Toast.LENGTH_LONG)
                            .show();
                }
            });
        }
    }

    private void closeMidiResources() {
        if (mPortSelector != null) {
            mPortSelector.close();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void displayLatencyMs() {
        try {
            StringBuilder sb = new StringBuilder();
            long startMillis = System.currentTimeMillis();

            if (isBluetoothSco()) {
                preOpenAudio();
                sb.append("Sco connected at ").append(System.currentTimeMillis() - startMillis).append("ms\n");
            }

            openAudio();
            sb.append("Opened at ").append(System.currentTimeMillis() - startMillis).append("ms\n");

            startAudio();
            while (mAudioInputTester.getCurrentAudioStream().getState() == StreamConfiguration.STREAM_STATE_STARTING) {
                Thread.sleep(POLL_DURATION_MILLIS);
            }
            sb.append("Started at ").append(System.currentTimeMillis() - startMillis).append("ms\n");

            while (mAudioInputTester.getPeakLevelWrapper(0) < 0.2 && mAudioInputTester.getPeakLevelWrapper(1) < 0.2 && (System.currentTimeMillis() - startMillis) <= 1000) {  // -26dB
                Thread.sleep(POLL_DURATION_MILLIS);
            }
            if (System.currentTimeMillis() - startMillis > 1000) {
                sb.append("Timout! Don't get -14dB signal!");
            } else {
                sb.append("Get -14dB signal at ").append(System.currentTimeMillis() - startMillis).append("ms\n");
            }

            mColdTimeView.setText(sb.toString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void triggerTest(View view) {
        mHelpView.setText("录音中");
        updateButtons(false);
        displayLatencyMs();
        mHandler.postDelayed(this::closeTest, CLOSE_TEST_MSEC);
    }

    private void closeTest() {
        stopAudio();
        closeAudio();
        postCloseAudio();
        mHelpView.setText("等待硬件关闭");
        mHandler.postDelayed(this::enableTest, ENABLE_TEST_MSEC);
    }

    private void enableTest() {
        updateButtons(true);
        mHelpView.setText("1、先对Mic吹气，然后点击Trigger按钮，此时获取信号值时间为冷启动时延；\n2、因为部分手机有缓存，建议正式测试前前测试一遍确保提示超时！");
    }
}
