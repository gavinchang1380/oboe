package com.mobileer.latencytester;

import java.io.IOException;

abstract class JavaAudioStream extends AudioStreamBase {
    private static final int INVALID_STREAM_INDEX = -1;
    int streamIndex = INVALID_STREAM_INDEX;

    @Override
    public void startPlayback() throws IOException {
        int result = AudioContext.getInstance().getCurrentActivity().startPlayback();
        if (result < 0) {
            throw new IOException("Start Playback failed! result = " + result);
        }
    }

    // Write disabled because the synth is in native code.
    @Override
    public int write(float[] buffer, int offset, int length) {
        return 0;
    }

    @Override
    public void open(StreamConfiguration requestedConfiguration,
                     StreamConfiguration actualConfiguration, int bufferSizeInFrames) throws IOException {
        super.open(requestedConfiguration, actualConfiguration, bufferSizeInFrames);
        int result = AudioContext.getInstance().getCurrentActivity().open(requestedConfiguration.getNativeApi(),
                requestedConfiguration.getSampleRate(),
                requestedConfiguration.getChannelCount(),
                requestedConfiguration.getFormat(),
                requestedConfiguration.getSharingMode(),
                requestedConfiguration.getPerformanceMode(),
                requestedConfiguration.getInputPreset(),
                requestedConfiguration.getUsage(),
                requestedConfiguration.getContentType(),
                requestedConfiguration.getDeviceId(),
                requestedConfiguration.getSessionId(),
                requestedConfiguration.getFramesPerBurst(),
                requestedConfiguration.getChannelConversionAllowed(),
                requestedConfiguration.getFormatConversionAllowed(),
                requestedConfiguration.getRateConversionQuality(),
                requestedConfiguration.isMMap(),
                isInput()
        );
        if (result < 0) {
            streamIndex = INVALID_STREAM_INDEX;
            throw new IOException("Open failed! result = " + result);
        } else {
            streamIndex = result;
        }
        actualConfiguration.setNativeApi(getNativeApi());
        actualConfiguration.setSampleRate(getSampleRate());
        actualConfiguration.setSharingMode(getSharingMode());
        actualConfiguration.setPerformanceMode(getPerformanceMode());
        actualConfiguration.setInputPreset(getInputPreset());
        actualConfiguration.setUsage(getUsage());
        actualConfiguration.setContentType(getContentType());
        actualConfiguration.setFramesPerBurst(getFramesPerBurst());
        actualConfiguration.setBufferCapacityInFrames(getBufferCapacityInFrames());
        actualConfiguration.setChannelCount(getChannelCount());
        actualConfiguration.setDeviceId(getDeviceId());
        actualConfiguration.setSessionId(getSessionId());
        actualConfiguration.setFormat(getFormat());
        actualConfiguration.setMMap(isMMap());
        actualConfiguration.setDirection(isInput()
                ? StreamConfiguration.DIRECTION_INPUT
                : StreamConfiguration.DIRECTION_OUTPUT);
    }

    @Override
    public void close() {
        if (streamIndex >= 0) {
            AudioContext.getInstance().getCurrentActivity().close(streamIndex);
            streamIndex = INVALID_STREAM_INDEX;
        }
    }

    @Override
    public int getBufferCapacityInFrames() {
        return AudioContext.getInstance().getCurrentActivity().getBufferCapacityInFrames(streamIndex);
    }

    @Override
    public int getBufferSizeInFrames() {
        return AudioContext.getInstance().getCurrentActivity().getBufferSizeInFrames(streamIndex);
    }

    @Override
    public boolean isThresholdSupported() {
        return true;
    }

    @Override
    public int setBufferSizeInFrames(int thresholdFrames) {
        return AudioContext.getInstance().getCurrentActivity().setBufferSizeInFrames(streamIndex, thresholdFrames);
    }

    private int getNativeApi() {
        return AudioContext.getInstance().getCurrentActivity().getNativeApi(streamIndex);
    }

    @Override
    public int getFramesPerBurst() {
        return AudioContext.getInstance().getCurrentActivity().getFramesPerBurst(streamIndex);
    }

    private int getSharingMode() {
        return AudioContext.getInstance().getCurrentActivity().getSharingMode(streamIndex);
    }

    private int getPerformanceMode() {
        return AudioContext.getInstance().getCurrentActivity().getPerformanceMode(streamIndex);
    }

    private int getInputPreset() {
        return AudioContext.getInstance().getCurrentActivity().getInputPreset(streamIndex);
    }

    @Override
    public int getSampleRate() {
        return AudioContext.getInstance().getCurrentActivity().getSampleRate(streamIndex);
    }

    private int getFormat() {
        return AudioContext.getInstance().getCurrentActivity().getFormat(streamIndex);
    }

    private int getUsage() {
        return AudioContext.getInstance().getCurrentActivity().getUsage(streamIndex);
    }

    private int getContentType() {
        return AudioContext.getInstance().getCurrentActivity().getContentType(streamIndex);
    }

    @Override
    public int getChannelCount() {
        return AudioContext.getInstance().getCurrentActivity().getChannelCount(streamIndex);
    }

    private int getDeviceId() {
        return AudioContext.getInstance().getCurrentActivity().getDeviceId(streamIndex);
    }

    private int getSessionId() {
        return AudioContext.getInstance().getCurrentActivity().getSessionId(streamIndex);
    }

    private boolean isMMap() {
        return AudioContext.getInstance().getCurrentActivity().isMMap(streamIndex);
    }

    @Override
    public int getLastErrorCallbackResult() {
        return AudioContext.getInstance().getCurrentActivity().getLastErrorCallbackResult(streamIndex);
    }

    @Override
    public long getFramesWritten() {
        return AudioContext.getInstance().getCurrentActivity().getFramesWritten(streamIndex);
    }

    @Override
    public long getFramesRead() {
        return AudioContext.getInstance().getCurrentActivity().getFramesRead(streamIndex);
    }

    @Override
    public int getXRunCount() {
        return AudioContext.getInstance().getCurrentActivity().getXRunCount(streamIndex);
    }

    @Override
    public double getLatency() {
        return AudioContext.getInstance().getCurrentActivity().getTimestampLatency(streamIndex);
    }

    @Override
    public double getCpuLoad() {
        return AudioContext.getInstance().getCurrentActivity().getCpuLoad(streamIndex);
    }

    @Override
    public String getCallbackTimeStr() {
        return AudioContext.getInstance().getCurrentActivity().getCallbackTimeString();
    }

    @Override
    public void setWorkload(double workload) {
        AudioContext.getInstance().getCurrentActivity().setWorkload(workload);
    }

    @Override
    public int getState() {
        return AudioContext.getInstance().getCurrentActivity().getState(streamIndex);
    }
}
