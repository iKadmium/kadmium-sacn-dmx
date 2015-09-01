package com.flynneffectmusic;

import com.flynneffectmusic.Listener.SACNListener;
import com.flynneffectmusic.Transmitter.DMXWebSocketServer;
import com.flynneffectmusic.Transmitter.EnttecProTransmitter;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.FileConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

import java.net.InetSocketAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by kadmi_000 on 30/08/2015.
 */
public class Main
{
    public static FileConfiguration configuration;
    static SACNListener listener;
    static WebServer webServer;
    static byte[] dmx;
    static EnttecProTransmitter transmitter;
    public static List<String> newErrors;
    public static List<String> oldErrors;

	public static void main(String[] args)
	{
        newErrors = new ArrayList<>();
        oldErrors = new ArrayList<>();

        try
        {
            configuration = new XMLConfiguration("config.xml");

        }
        catch (ConfigurationException e)
        {
            configuration = new XMLConfiguration();
            configuration.setFile(new java.io.File("config.xml"));
            configuration.setProperty("Universe", 1);
            configuration.setProperty("ListenAdapter", "auto");
            configuration.setProperty("SerialPort", "COM2");
        }

        short universe = configuration.getShort("Universe");
        String listenAdapter = configuration.getString("ListenAdapter");
        listener = new SACNListener(universe, listenAdapter);
        String comPort = configuration.getString("SerialPort");
        transmitter = new EnttecProTransmitter(comPort);

        webServer = new WebServer(6788);


        DMXWebSocketServer webSocketServer = new DMXWebSocketServer(new InetSocketAddress(6787));
        webSocketServer.start();

        /*for(int i = 0; i < 52; i++)
        {
            System.out.println("<tr>");
            System.out.println("\t<th>" + i + "</th>");
            for(int j = 0; (i == 51 && j < 3) || (i < 51 && j < 10); j++)
            {
                System.out.println("\t<td id=\"dmx" + ((i*10) + j) + "\">000</td>");
            }
            System.out.println("</tr>");
        }*/

        while(true)
        {
            if(listener.read())
            {
                dmx = listener.getDMX();
                transmitter.SendDMX(dmx);
                webSocketServer.SendDMX(dmx);
            }
        }
    }

    public static void LogError(String status)
    {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();

        newErrors.add(dateFormat.format(cal.getTime()) + " -> " + status);
    }

    public static void ClearErrors()
    {
        oldErrors.addAll(newErrors);
        newErrors.clear();
    }
}
