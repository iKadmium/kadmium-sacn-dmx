package com.flynneffectmusic.Transmitter;

import com.flynneffectmusic.Main;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

public class EnttecProTransmitter implements IDMXTransmitter
{
    private int DMX_PRO_MIN_DATA_SIZE = 25;
    private byte DMX_PRO_MESSAGE_START = (byte) (0x7E);
    private byte DMX_PRO_MESSAGE_END = (byte) (0xE7);
    private byte DMX_PRO_SEND_PACKET = (byte) (6);

    private SerialPort dmx;
    private OutputStream outputStream;
    private String address;

    public EnttecProTransmitter(String comPort)
    {
        address = comPort;

        @SuppressWarnings("unchecked")
        Enumeration<CommPortIdentifier> portIdentifiers = CommPortIdentifier
                .getPortIdentifiers();
        List<CommPortIdentifier> identifiers = Collections
                .list(portIdentifiers);

        System.out.println("Seeking serial port " + comPort);
        for (CommPortIdentifier port : identifiers)
        {
            if (port.getName().equals(comPort))
            {
                System.out.println("Found serial port [" + port.getName()
                                           + "], using");
                try
                {
                    dmx = (SerialPort) port.open("DMXControlApp", 2000);
                    dmx.setSerialPortParams(115200, SerialPort.DATABITS_8,
                                            SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                    outputStream = dmx.getOutputStream();
                }
                catch (PortInUseException | IOException | UnsupportedCommOperationException e)
                {
                    Main.LogError("Error accessing serialPort [" + comPort + "]");
                    Main.newErrors.add(e.getMessage());
                    e.printStackTrace();
                }
            }
            else
            {
                System.out.println("Ignoring serial port [" + port.getName()
                                           + "]");
            }
        }
        if(dmx == null)
        {

            Main.LogError("Could not find serial port [" + comPort + "]");
        }
    }

    public static List<String> GetCommPorts()
    {
        @SuppressWarnings("unchecked")
        Enumeration<CommPortIdentifier> portIdentifiers = CommPortIdentifier
                .getPortIdentifiers();
        List<CommPortIdentifier> identifiers = Collections
                .list(portIdentifiers);
        return identifiers
                .stream()
                .map(CommPortIdentifier::getName)
                .collect(Collectors.toList());
    }

    public void close()
    {
        if (dmx != null)
        {
            dmx.close();
        }
    }

    private void DmxMessage(byte messageType, byte[] data)
    {
        byte[] metadata = new byte[5];
        int dataSize = data.length + 2;
        if(dataSize < DMX_PRO_MIN_DATA_SIZE)
        {
            dataSize = DMX_PRO_MIN_DATA_SIZE;
        }

        metadata[0] = DMX_PRO_MESSAGE_START;
        metadata[1] = messageType;
        metadata[2] = (byte) (dataSize & 255);
        metadata[3] = (byte) ((dataSize >> 8) & 255);
        metadata[4] = (byte) 0; // DMX Command byte

        byte[] outputData = new byte[dataSize + metadata.length];
        System.arraycopy(metadata, 0, outputData, 0, metadata.length);
        System.arraycopy(data, 0, outputData, metadata.length, data.length);

        outputData[outputData.length - 1] = DMX_PRO_MESSAGE_END;

        if (dmx != null)
        {
            try
            {
                outputStream.write(outputData);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public String GetAddress()
    {
        return address;
    }

    public void SendDMX (byte[] data)
    {
        DmxMessage(DMX_PRO_SEND_PACKET, data);
    }

    public String GetType()
    {
        return "enttec";
    }

    @Override
    public String toString()
    {
        return "Serial Transmitter - " + GetAddress();
    }
}