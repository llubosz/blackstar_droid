package com.ca13b.blackdroid;

public interface AmpCommunicator {
    void setUpDevice();
    void SendData(byte[] data);
    void shutdown();
}
