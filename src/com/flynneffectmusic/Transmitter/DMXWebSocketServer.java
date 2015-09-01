package com.flynneffectmusic.Transmitter;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by higginsonj on 31/08/2015.
 */
public class DMXWebSocketServer extends WebSocketServer implements  IDMXTransmitter
{
    byte[] lastDMX;
    LocalDateTime lastUpdateTime;
    Timer updater;

    public DMXWebSocketServer(InetSocketAddress address)
    {
        super(address);
        lastDMX = new byte[512];
        lastUpdateTime = null;
        updater = new Timer("Websocket update timer", true);
        updater.scheduleAtFixedRate(
            new TimerTask()
            {
                @Override
                public void run()
                {
                    SendLastUpdateTime();
                }
            },
            1000,
            1000
        );
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake)
    {
        SendDMXInternal();
        SendLastUpdateTime();
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b)
    {
    }

    @Override
    public void onMessage(WebSocket webSocket, String s)
    {

    }

    @Override
    public void onError(WebSocket webSocket, Exception e)
    {

    }

    @Override
    public void SendDMX(byte[] data)
    {
        lastDMX = data;
        lastUpdateTime = LocalDateTime.now();
        SendDMXInternal();
        SendLastUpdateTime();
    }

    private void SendDMXInternal()
    {
        StringBuilder builder = new StringBuilder("dmx=");
        for(int i = 0; i < lastDMX.length - 1; i++)
        {
            builder.append((lastDMX[i] & 0xFF) +",");
            //output += 128 +",";
        }
        SendToAll(builder.toString());
    }

    private void SendToAll(String output)
    {
        for(WebSocket socket : connections())
        {
            socket.send(output);
        }
    }

    private void SendLastUpdateTime()
    {
        String output = "upt=";
        if(lastUpdateTime != null)
        {
            Duration timeSinceLastUpdate = Duration.between(lastUpdateTime, LocalDateTime.now() );
            output += timeSinceLastUpdate.getSeconds() +"s";
        }
        else
        {
            output += "never";
        }

        SendToAll(output);
    }

    @Override
    public void close()
    {
        try
        {
            stop();
        }
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
        updater.cancel();
    }
}
