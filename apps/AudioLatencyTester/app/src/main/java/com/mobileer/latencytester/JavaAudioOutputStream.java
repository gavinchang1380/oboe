package com.mobileer.latencytester;

class JavaAudioOutputStream extends JavaAudioStream implements AudioOutputStream {
    @Override
    public boolean isInput() {
        return false;
    }

    public void trigger() {
        AudioContext.getInstance().getCurrentActivity().trigger();
    }

    public void setChannelEnabled(int channelIndex, boolean enabled) {
        AudioContext.getInstance().getCurrentActivity().setChannelEnabled(channelIndex, enabled);
    }

    public void setSignalType(int type) {
        AudioContext.getInstance().getCurrentActivity().setSignalType(type);
    }
}
