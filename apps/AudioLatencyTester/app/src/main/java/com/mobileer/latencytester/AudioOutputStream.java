package com.mobileer.latencytester;

public interface AudioOutputStream {
    public void trigger();

    public void setChannelEnabled(int channelIndex, boolean enabled);

    public void setSignalType(int type);
}