package com.mobileer.latencytester;

import android.annotation.SuppressLint;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Log;

import java.util.Arrays;


abstract class ActivityContext implements Runnable {
    public final static String TAG = "ActivityContext";
    short[] bufferShort;
    float[] bufferFloat;
    float[] bufferFloatOneChannel;
    double[] mPeakLevel = new double[2];

    boolean mThreadEnable = false;
    private Thread mThread;

    int mNextStreamHandle = 0;

    int mOutputIndex = -1;
    AudioTrack mOutputStream;

    int mInputIndex = -1;
    AudioRecord mInputStream;

    private int allocateStreamIndex() {
        return mNextStreamHandle++;
    }

    @SuppressLint("MissingPermission")
    synchronized public int open(int nativeApi,
                    int sampleRate,
                    int channelCount,
                    int format,
                    int sharingMode,
                    int performanceMode,
                    int inputPreset,
                    int usage,
                    int contentType,
                    int deviceId,
                    int sessionId,
                    int framesPerBurst,
                    boolean channelConversionAllowed,
                    boolean formatConversionAllowed,
                    int rateConversionQuality,
                    boolean isMMap,
                    boolean isInput) {
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        if (format == 2) {
            audioFormat = AudioFormat.ENCODING_PCM_FLOAT;
        }

        if (!isInput) {
            int channelMask;
            if (channelCount == 0) {
                channelMask = AudioFormat.CHANNEL_OUT_STEREO;
            } else if (channelCount == 1) {
                channelMask = AudioFormat.CHANNEL_OUT_MONO;
            } else if (channelCount == 2) {
                channelMask = AudioFormat.CHANNEL_OUT_STEREO;
            } else {
                return -1;
            }

            int audioPerformanceMode;
            if (performanceMode == 10) {
                audioPerformanceMode = AudioTrack.PERFORMANCE_MODE_NONE;
            } else if (performanceMode == 12) {
                audioPerformanceMode = AudioTrack.PERFORMANCE_MODE_LOW_LATENCY;
            } else if (performanceMode == 11) {
                audioPerformanceMode = AudioTrack.PERFORMANCE_MODE_POWER_SAVING;
            } else {
                return -1;
            }

            if (usage == 0) {
                usage = AudioAttributes.USAGE_MEDIA;
            }

            if (contentType == 0) {
                contentType = AudioAttributes.CONTENT_TYPE_MUSIC;
            }

            mOutputStream = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(usage)
                            .setContentType(contentType)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(audioFormat)
                            .setSampleRate(sampleRate)
                            .setChannelMask(channelMask)
                            .build())
                    .setPerformanceMode(audioPerformanceMode)
                    .build();
            if (mOutputStream != null) {
                mOutputIndex = allocateStreamIndex();
            }
            return mOutputIndex;
        } else {
            if (mOutputIndex != -1) {
                // set the same with output stream
                sampleRate = mOutputStream.getSampleRate();
                channelCount = mOutputStream.getChannelCount();
                audioFormat = mOutputStream.getAudioFormat();
            }

            if (sampleRate == 0) {
                sampleRate = 48000;
            }

            int channelConfig = (channelCount == 1)
                    ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
            int minRecordBuffSizeInBytes = AudioRecord.getMinBufferSize(sampleRate,
                    channelConfig,
                    audioFormat);

            mInputStream = new AudioRecord(
                    inputPreset,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    minRecordBuffSizeInBytes);
            mInputIndex = allocateStreamIndex();
            return mInputIndex;
        }
    }

    synchronized int start() {
        bufferShort = new short[AudioContext.mCallbackSize * 2];
        bufferFloat = new float[AudioContext.mCallbackSize * 2];
        bufferFloatOneChannel = new float[AudioContext.mCallbackSize];
        mPeakLevel[0] = 0.0;
        mPeakLevel[1] = 0.0;

        if (mOutputIndex != -1) {
            mOutputStream.play();
        }
        if (mInputIndex != -1) {
            mInputStream.startRecording();
        }

        mThreadEnable = true;
        mThread = new Thread(this);
        mThread.start();

        return 0;
    }

    int pause() {
        return -1;
    }

    synchronized int stop() {
        mThreadEnable = false;
        if (mThread != null) {
            try {
                mThread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mThread = null;
        }

        if (mOutputIndex != -1) {
            mOutputStream.stop();
            mOutputStream.release();
        } else if (mInputIndex != -1) {
            mInputStream.stop();
            mInputStream.release();
        }

        return 0;
    }

    synchronized int close(int streamIndex) {
        if (streamIndex == mOutputIndex) {
            mOutputIndex = -1;
            mOutputStream = null;
        } else if (streamIndex == mInputIndex) {
            mInputIndex = -1;
            mInputStream = null;
        }
        return 0;
    }

    int getFramesPerCallback() {
        return AudioContext.mCallbackSize;
    }

    int startPlayback() {
        return 0;
    }

    int setBufferSizeInFrames(int streamIndex, int threshold) {
        if (streamIndex == mOutputIndex) {
            return mOutputStream.setBufferSizeInFrames(threshold);
        }
        return 0;
    }

    int getBufferSizeInFrames(int streamIndex) {
        if (streamIndex == mOutputIndex) {
            return mOutputStream.getBufferSizeInFrames();
        } else if (streamIndex == mInputIndex) {
            return mInputStream.getBufferSizeInFrames();
        }
        return 0;
    }

    int getBufferCapacityInFrames(int streamIndex) {
        if (streamIndex == mOutputIndex) {
            return mOutputStream.getBufferCapacityInFrames();
        } else if (streamIndex == mInputIndex) {
            return mInputStream.getBufferSizeInFrames();
        }
        return 0;
    }

    int getNativeApi(int streamIndex) {
        return 0;
    }

    int getSharingMode(int streamIndex) {
        return 0;
    }

    int getPerformanceMode(int streamIndex) {
        if (streamIndex == mOutputIndex) {
            int performance = mOutputStream.getPerformanceMode();
            if (performance == AudioTrack.PERFORMANCE_MODE_NONE) {
                return 10;
            } else if (performance == AudioTrack.PERFORMANCE_MODE_LOW_LATENCY) {
                return 12;
            } else if (performance == AudioTrack.PERFORMANCE_MODE_POWER_SAVING) {
                return 11;
            }
        }
        return 10;
    }

    int getInputPreset(int streamIndex) {
        if (streamIndex == mInputIndex) {
            return mInputStream.getAudioSource();
        }
        return 0;
    }

    int getFramesPerBurst(int streamIndex) {
        if (streamIndex == -1) {
            return -886;
        }

        if (streamIndex == mOutputIndex) {
            return mOutputStream.getBufferSizeInFrames();
        } else if (streamIndex == mInputIndex) {
            return mInputStream.getBufferSizeInFrames();
        }

        return 1;
    }

    int getChannelCount(int streamIndex) {
        if (streamIndex == mOutputIndex) {
            return mOutputStream.getChannelCount();
        } else if (streamIndex == mInputIndex) {
            return mInputStream.getChannelCount();
        }
        return 0;
    }

    int getFormat(int streamIndex) {
        int format;
        if (streamIndex == mOutputIndex) {
            format = mOutputStream.getAudioFormat();
        } else if (streamIndex == mInputIndex) {
            format = mInputStream.getAudioFormat();
        } else {
            return 0;
        }
        if (format == AudioFormat.ENCODING_PCM_16BIT) {
            return 1;
        } else {
            return 2;
        }
    }

    int getUsage(int streamIndex) {
        if (streamIndex == mOutputIndex) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return mOutputStream.getAudioAttributes().getUsage();
            }
        }
        return 0;
    }

    int getContentType(int streamIndex) {
        if (streamIndex == mOutputIndex) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return mOutputStream.getAudioAttributes().getContentType();
            }
        }
        return 0;
    }

    int getDeviceId(int streamIndex) {
        return 0;
    }

    int getSessionId(int streamIndex) {
        return -1;
    }

    int getFramesWritten(int streamIndex) {
        return 0;
    }

    int getFramesRead(int streamIndex) {
        return 0;
    }

    int getXRunCount(int streamIndex) {
        if (streamIndex == mOutputIndex) {
            return mOutputStream.getUnderrunCount();
        }
        return 0;
    }

    int getCallbackCount() {
        return 0;
    }

    int getLastErrorCallbackResult(int streamIndex) {
        return 0;
    }

    int getTimestampLatency(int streamIndex) {
        return 0;
    }

    int getCpuLoad(int streamIndex) {
        return 0;
    }

    int getSampleRate(int streamIndex) {
        if (streamIndex == mOutputIndex) {
            return mOutputStream.getSampleRate();
        } else if (streamIndex == mInputIndex) {
            return mInputStream.getSampleRate();
        } else {
            return 0;
        }
    }

    String getCallbackTimeString() {
        return "";
    }

    int setWorkload(double workload) {
        return 0;
    }

    int getState(int streamIndex) {
        return 0;
    }

    double getPeakLevel(int index) {
        if (index == 0 || index == 1) {
            return mPeakLevel[index];
        }
        return 0;
    }

    void setCallbackReturnStop(boolean b) {
    }

    boolean isMMap(int streamIndex) {
        return false;
    }

    void trigger() {
    }

    void setChannelEnabled(int channelIndex, boolean enabled) {
    }

    int saveWaveFile(String fileName) {
        return 0;
    }

    void setSignalType(int type) {
    }

    void setMinimumFramesBeforeRead(int numFrames) {
    }

    int getColdStartInputMillis() {
        return 0;
    }

    int getColdStartOutputMillis() {
        return 0;
    }
}

class ActivityTestInput extends ActivityContext {

    private void processPeak(double input, int i) {
        mPeakLevel[i] *= 0.99f; // exponential decay
        input = Math.abs(input);
        // never fall below the input signal
        if (input > mPeakLevel[i]) {
            mPeakLevel[i] = input;
        }
    }

    @Override
    public void run() {
        while (mThreadEnable) {
            if (mInputStream.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT) {
                if (mInputStream.getChannelCount() == 1) {
                    mInputStream.read(bufferShort, 0, bufferShort.length / 2, AudioRecord.READ_BLOCKING);
                    for (int i = 0; i < bufferShort.length / 2; ++i) {
                        processPeak((double)bufferShort[i] / 32768, 0);
                    }
                } else {
                    mInputStream.read(bufferShort, 0, bufferShort.length, AudioRecord.READ_BLOCKING);
                    for (int i = 0; i < bufferShort.length / 2; ++i) {
                        processPeak((double)bufferShort[2 * i] / 32768, 0);
                        processPeak((double)bufferShort[2 * i + 1] / 32768, 1);
                    }
                }
            } else if (mInputStream.getAudioFormat() == AudioFormat.ENCODING_PCM_FLOAT) {
                if (mInputStream.getChannelCount() == 1) {
                    mInputStream.read(bufferFloat, 0, bufferShort.length / 2, AudioRecord.READ_BLOCKING);
                    for (int i = 0; i < bufferFloat.length / 2; ++i) {
                        processPeak((double)bufferFloat[i], 0);
                    }
                } else {
                    mInputStream.read(bufferFloat, 0, bufferShort.length, AudioRecord.READ_BLOCKING);
                    for (int i = 0; i < bufferFloat.length / 2; ++i) {
                        processPeak((double)bufferFloat[2 * i], 0);
                        processPeak((double)bufferFloat[2 * i + 1], 1);
                    }
                }
            }
        }
    }
}

class ActivityTapToTone extends ActivityContext {
    private float mPhase = -1.0f;
    private float mLevel = 0.0f;
    private float mPhaseIncreasement;
    private boolean mTrigger = false;

    @Override
    int start() {
        mPhase = -1.0f;
        final float FREQUENCY_SAW_PING = 800;
        mPhaseIncreasement = ((2.0f * FREQUENCY_SAW_PING) / mOutputStream.getSampleRate());
        return super.start();
    }

    @Override
    public void run() {
        final float AMPLITUDE_SAW_PING = 0.8f;

        while (mThreadEnable) {
            if (mTrigger) {
                mTrigger = false;
                mLevel = 1.0f;
                mPhase = -1.0f;
            }

            if (mLevel > 0.000001) {
                for (int i = 0; i < bufferFloatOneChannel.length; ++i) {
                    bufferFloatOneChannel[i] = mPhase * AMPLITUDE_SAW_PING * mLevel;
                    mPhase += mPhaseIncreasement;
                    if (mPhase > 1.0) {
                        mPhase -= 2.0;
                    }
                    mLevel *= 0.999;
                }
            } else {
                Arrays.fill(bufferFloatOneChannel, 0f);
            }

            if (mOutputStream.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT) {
                if (mOutputStream.getChannelCount() == 1) {
                    for (int i = 0; i < bufferShort.length / 2; ++i) {
                        bufferShort[i] = (short) (bufferFloatOneChannel[i] * 32767);
                    }
                    mOutputStream.write(bufferShort, 0, bufferShort.length / 2, AudioTrack.WRITE_BLOCKING);
                } else {
                    for (int i = 0; i < bufferShort.length / 2; ++i) {
                        bufferShort[2 * i] = (short) (bufferFloatOneChannel[i] * 32767);
                        bufferShort[2 * i + 1] = (short) (bufferFloatOneChannel[i] * 32767);
                    }
                    mOutputStream.write(bufferShort, 0, bufferShort.length, AudioTrack.WRITE_BLOCKING);
                }
            } else if (mOutputStream.getAudioFormat() == AudioFormat.ENCODING_PCM_FLOAT) {
                if (mOutputStream.getChannelCount() == 1) {
                    System.arraycopy(bufferFloatOneChannel, 0, bufferFloat, 0, bufferFloat.length / 2);
                    mOutputStream.write(bufferFloat, 0, bufferFloat.length / 2, AudioTrack.WRITE_BLOCKING);
                } else {
                    for (int i = 0; i < bufferFloat.length / 2; ++i) {
                        bufferFloat[2 * i] = bufferFloatOneChannel[i];
                        bufferFloat[2 * i + 1] = bufferFloatOneChannel[i];
                    }
                    mOutputStream.write(bufferFloat, 0, bufferFloat.length, AudioTrack.WRITE_BLOCKING);
                }
            }
        }
    }

    void trigger() {
        mTrigger = true;
    }
}

class ActivityEcho extends ActivityContext {
    private int mCountCallbacksToDrain = 20;
    private int mCountCallbacksToDiscard = 30;

    @Override
    synchronized int start() {
        mCountCallbacksToDrain = 20;
        mCountCallbacksToDiscard = 30;

        return super.start();
    }

    @Override
    public void run() {
        while (mThreadEnable) {
            if (mOutputStream.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT) {
                if (mOutputStream.getChannelCount() == 1) {
                    if (mCountCallbacksToDrain > 0) {
                        int totalFramesRead = 0;
                        int actualFramesRead;
                        do {
                            actualFramesRead = mInputStream.read(bufferShort, 0, bufferShort.length / 2, AudioRecord.READ_NON_BLOCKING);
                            totalFramesRead += actualFramesRead;
                        } while (actualFramesRead > 0);
                        if (totalFramesRead > 0) {
                            mCountCallbacksToDrain--;
                        }
                        Arrays.fill(bufferShort, (short) 0);
                        mOutputStream.write(bufferShort, 0, bufferShort.length / 2, AudioTrack.WRITE_BLOCKING);
                    } else if (mCountCallbacksToDiscard > 0) {
                        mInputStream.read(bufferShort, 0, bufferShort.length / 2, AudioRecord.READ_NON_BLOCKING);
                        Arrays.fill(bufferShort, (short) 0);
                        mOutputStream.write(bufferShort, 0, bufferShort.length / 2, AudioTrack.WRITE_BLOCKING);
                        mCountCallbacksToDiscard--;
                    } else {
                        int read = mInputStream.read(bufferShort, 0, bufferShort.length / 2, AudioRecord.READ_NON_BLOCKING);
                        mOutputStream.write(bufferShort, 0, bufferShort.length / 2, AudioTrack.WRITE_BLOCKING);
                    }
                } else {
                    if (mCountCallbacksToDrain > 0) {
                        int totalFramesRead = 0;
                        int actualFramesRead;
                        int count = 0;
                        do {
                            actualFramesRead = mInputStream.read(bufferShort, 0, bufferShort.length, AudioRecord.READ_NON_BLOCKING);
                            totalFramesRead += actualFramesRead;
                        } while (actualFramesRead > 0);
                        if (totalFramesRead > 0) {
                            mCountCallbacksToDrain--;
                        }
                        Arrays.fill(bufferShort, (short) 0);
                        mOutputStream.write(bufferShort, 0, bufferShort.length, AudioTrack.WRITE_BLOCKING);
                    } else if (mCountCallbacksToDiscard > 0) {
                        mInputStream.read(bufferShort, 0, bufferShort.length, AudioRecord.READ_NON_BLOCKING);
                        Arrays.fill(bufferShort, (short) 0);
                        mOutputStream.write(bufferShort, 0, bufferShort.length, AudioTrack.WRITE_BLOCKING);
                        mCountCallbacksToDiscard--;
                    } else {
                        int read = mInputStream.read(bufferShort, 0, bufferShort.length, AudioRecord.READ_NON_BLOCKING);
                        mOutputStream.write(bufferShort, 0, bufferShort.length, AudioTrack.WRITE_BLOCKING);
                    }
                }
            } else {
                if (mOutputStream.getChannelCount() == 1) {
                    if (mCountCallbacksToDrain > 0) {
                        int totalFramesRead = 0;
                        int actualFramesRead;
                        do {
                            actualFramesRead = mInputStream.read(bufferFloat, 0, bufferFloat.length / 2, AudioRecord.READ_NON_BLOCKING);
                            totalFramesRead += actualFramesRead;
                        } while (actualFramesRead > 0);
                        if (totalFramesRead > 0) {
                            mCountCallbacksToDrain--;
                        }
                        Arrays.fill(bufferFloat, (short) 0);
                        mOutputStream.write(bufferFloat, 0, bufferFloat.length / 2, AudioTrack.WRITE_BLOCKING);
                    } else if (mCountCallbacksToDiscard > 0) {
                        mInputStream.read(bufferFloat, 0, bufferFloat.length / 2, AudioRecord.READ_NON_BLOCKING);
                        Arrays.fill(bufferFloat, (short) 0);
                        mOutputStream.write(bufferFloat, 0, bufferFloat.length / 2, AudioTrack.WRITE_BLOCKING);
                        mCountCallbacksToDiscard--;
                    } else {
                        int read = mInputStream.read(bufferFloat, 0, bufferFloat.length / 2, AudioRecord.READ_NON_BLOCKING);
                        mOutputStream.write(bufferFloat, 0, bufferFloat.length / 2, AudioTrack.WRITE_BLOCKING);
                    }
                } else {
                    if (mCountCallbacksToDrain > 0) {
                        int totalFramesRead = 0;
                        int actualFramesRead;
                        int count = 0;
                        do {
                            actualFramesRead = mInputStream.read(bufferFloat, 0, bufferFloat.length, AudioRecord.READ_NON_BLOCKING);
                            totalFramesRead += actualFramesRead;
                        } while (actualFramesRead > 0);
                        if (totalFramesRead > 0) {
                            mCountCallbacksToDrain--;
                        }
                        Arrays.fill(bufferFloat, (short) 0);
                        mOutputStream.write(bufferFloat, 0, bufferFloat.length, AudioTrack.WRITE_BLOCKING);
                    } else if (mCountCallbacksToDiscard > 0) {
                        mInputStream.read(bufferFloat, 0, bufferFloat.length, AudioRecord.READ_NON_BLOCKING);
                        Arrays.fill(bufferFloat, (short) 0);
                        mOutputStream.write(bufferFloat, 0, bufferFloat.length, AudioTrack.WRITE_BLOCKING);
                        mCountCallbacksToDiscard--;
                    } else {
                        int read = mInputStream.read(bufferFloat, 0, bufferFloat.length, AudioRecord.READ_NON_BLOCKING);
                        mOutputStream.write(bufferFloat, 0, bufferFloat.length, AudioTrack.WRITE_BLOCKING);
                    }
                }
            }
        }
    }
}

class ActivityRoundTripLatency extends ActivityContext {

    @Override
    public void run() {

    }
}

public class AudioContext {
    static int mCallbackSize = 240;

    public ActivityContext getCurrentActivity() {
        return currentActivity;
    }

    public void setActivityType(int activityType) {
        switch (activityType) {
            default:
            case TestAudioActivity.ACTIVITY_TEST_INPUT:
                currentActivity = mActivityTestInput;
                break;
            case TestAudioActivity.ACTIVITY_TAP_TO_TONE:
                currentActivity = mActivityTapToTone;
                break;
            case TestAudioActivity.ACTIVITY_ECHO:
                currentActivity = mActivityEcho;
                break;
            case TestAudioActivity.ACTIVITY_RT_LATENCY:
                currentActivity = mActivityRoundTripLatency;
                break;
        }
    }

    public static AudioContext getInstance() {
        return mInstance;
    }

    private final ActivityTestInput mActivityTestInput = new ActivityTestInput();
    private final ActivityTapToTone mActivityTapToTone = new ActivityTapToTone();
    private final ActivityEcho mActivityEcho = new ActivityEcho();
    private final ActivityRoundTripLatency mActivityRoundTripLatency = new ActivityRoundTripLatency();

    private ActivityContext currentActivity = mActivityTestInput;

    private static final AudioContext mInstance = new AudioContext();

    void setCallbackSize(int callbackSize) {
        if (callbackSize == 0) {
            callbackSize = 240;
        }
        mCallbackSize = callbackSize;
    }
}
