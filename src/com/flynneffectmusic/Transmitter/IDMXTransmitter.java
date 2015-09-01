package com.flynneffectmusic.Transmitter;

/**
 * Created by higginsonj on 31/08/2015.
 */
public interface IDMXTransmitter
{
    void SendDMX(byte[] data);
    void close();
}
