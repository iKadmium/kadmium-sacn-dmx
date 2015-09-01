package com.flynneffectmusic;

import com.flynneffectmusic.Transmitter.EnttecProTransmitter;
import gnu.io.CommPortIdentifier;


import net.freeutils.httpserver.HTTPServer;
import org.apache.commons.configuration.ConfigurationException;
import org.jdom2.DocType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

/**
 * Created by higginsonj on 1/06/2015.
 */
public class WebServer
{
    HTTPServer httpServer;
    private static Element serialPorts;

    public WebServer(int port)
    {
        httpServer = new HTTPServer(port);
        HTTPServer.VirtualHost host = httpServer.getVirtualHost(null);
        try
        {
            host.addContext("/realtime", new HTTPServer.FileContextHandler( new File("htdocs/realtime.html"), "/realtime"));
            host.addContext("/config", getConfigHandler(), "GET", "POST");
            host.addContext("/", new HTTPServer.FileContextHandler( new File("htdocs"), "/"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        try
        {
            httpServer.start();
            System.out.println("WebServer listening on port " + port);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    static String getIndexContent(String message)
    {
        Element html = new Element("html");
        DocType docType = new DocType("html");
        Document doc = new Document(html, docType);

        Element form = getForm();

        html.getChildren().add(getHead());
        html.getChildren().add(getBody(message, form));

        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        return outputter.outputString(doc);

    }

    private static Element getListenAdapters()
    {
        Element listenAdapter = new Element("select");
        listenAdapter.setAttribute("class", "form-control");
        listenAdapter.setAttribute("id", "listenAdapter");
        try
        {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces()))
            {
                for (InetAddress inetAddress : Collections.list(networkInterface.getInetAddresses()))
                {
                    if (inetAddress instanceof Inet4Address)
                    {
                        Element addressOption = new Element("option");
                        addressOption.setAttribute("value", networkInterface.getName() + "");
                        addressOption.setText(
                            networkInterface.getDisplayName() + " -> " + inetAddress.getHostAddress()
                        );
                        listenAdapter.addContent(addressOption);
                        if (Main.listener.getListenAdapter().equals(networkInterface.getName()))
                        {
                            addressOption.setAttribute("selected", "selected");
                        }
                    }
                }
            }
        }
        catch (SocketException e)
        {
            e.printStackTrace();
        }

        Element autoAddressOption = new Element("option");
        autoAddressOption.setAttribute("value", "auto" + "");
        autoAddressOption.setText("auto");
        listenAdapter.addContent(autoAddressOption);
        if (Main.listener.getListenAdapter().equals("auto"))
        {
            autoAddressOption.setAttribute("selected", "selected");
        }

        return listenAdapter;

    }

    private static Element getForm()
    {
        Element form = new Element("form");
        form.setAttribute("role", "form");
        form.setAttribute("method", "POST");
        form.setAttribute("action", "/config");
        form.setAttribute("class", "form-horizontal");

        form.addContent(createElement("legend", "Settings"));

        form.getChildren().addAll(getInputTextSet("Universe", "universe", Main.listener.getUniverse() + ""));

        form.getChildren().addAll(
            getInputElementSet(
                getListenAdapters(), "DMX Listen Adapter", "listenAdapter", Main.listener.getListenAdapter()
            )
        );

        form.getChildren().addAll(
            getInputElementSet(
                getSerialPorts(), "Serial Port", "serialPort", Main.transmitter.GetAddress()
            )
        );

        Element div = new Element("div");
        div.setAttribute("class", "col-sm-offset-2 col-sm-10");

        Element submitElement = new Element("button");
        submitElement.setAttribute("type", "submit");
        submitElement.setAttribute("class", "btn btn-default");
        submitElement.setText("Submit");
        div.addContent(submitElement);

        form.addContent(div);

        //form.getChildren().add(fieldset);

        return form;
    }

    public static Element createElement(String name, String value)
    {
        Element element = new Element(name);
        element.setText(value);
        return element;
    }

    private static Collection<Element> getInputElementSet(Element element, String displayName, String id, String value)
    {
        ArrayList<Element> elements = new ArrayList<>();
        Element div = new Element("div");
        div.setAttribute("class", "form-group");

        Element label = createLabel(element, displayName, id);
        label.setAttribute("class", label.getAttributeValue("class") + " col-sm-2");

        Element subDiv = new Element("div");
        subDiv.setAttribute("class", "col-sm-10");

        div.addContent(label);
        subDiv.addContent(element);
        div.addContent(subDiv);

        elements.add(div);
        return elements;
    }

    private static Collection<Element> getInputTextSet(String displayName, String id, String value)
    {
        return getInputElementSet(getInputText(value), displayName, id, value);
    }

    private static Element getInputText(String value)
    {
        Element element = new Element("input");
        element.setAttribute("type", "text");
        element.setAttribute("value", value);
        element.setAttribute("class", "form-control");
        return element;
    }

    private static Element createLabel(Element input, String displayText, String id)
    {
        Element label = createElement("label", displayText);
        label.setAttribute("for", id);
        label.setAttribute("class", "control-label");

        input.setAttribute("id", id);
        input.setAttribute("name", id);

        return label;
    }

    private static Element getBody(String message, Element form)
    {
        Element body = new Element("body");
        Element container = new Element("div");
        container.setAttribute("class", "container");
        Element h1 = new Element("h1");
        h1.setText("sACN to OPC");
        container.getChildren().add(h1);

        Element navigation = new Element("div");
        navigation.setAttribute("class", "btn-group btn-group-lg");

        Element navHome = new Element("a");
        navHome.setAttribute("href", "/");
        navHome.setAttribute("class", "btn btn-primary");
        navHome.setText("Home");
        navigation.addContent(navHome);

        Element navConfig = new Element("a");
        navConfig.setAttribute("href", "/config");
        navConfig.setAttribute("class", "btn btn-primary");
        navConfig.setText("Config");
        navigation.addContent(navConfig);

        Element navRealtime = new Element("a");
        navRealtime.setAttribute("href", "/realtime");
        navRealtime.setAttribute("class", "btn btn-primary");
        navRealtime.setText("Realtime");
        navigation.addContent(navRealtime);

        if(Main.newErrors.size() > 0)
        {
            Element errors = new Element("div");
            errors.setAttribute("class", "alert alert-danger");
            for (String error : Main.newErrors)
            {
                Element li = new Element("p");
                li.addContent(error);
                errors.getChildren().add(li);
            }
            Main.ClearErrors();
            container.addContent(errors);
        }

        if(Main.oldErrors.size() > 0)
        {
            Element errors = new Element("div");
            errors.setAttribute("class", "alert alert-warning");
            for (String error : Main.newErrors)
            {
                Element li = new Element("p");
                li.addContent(error);
                errors.getChildren().add(li);
            }
            Main.ClearErrors();
            container.addContent(errors);
        }

        if(message != null)
        {
            Element div = new Element("div");
            div.setAttribute("class", "alert alert-success");

            Element p = new Element("strong");
            p.setText(message);
            div.addContent(p);
            container.addContent(div);
        }


        container.addContent(navigation);
        container.addContent(form);

        body.addContent(container);

        return body;
    }

    private static Element getHead()
    {
        Element head = new Element("head");
        Element title = new Element("title");
        title.setText("sACN to OPC");
        head.getChildren().add(title);
        Element link = new Element("link");
        link.setAttribute("rel", "stylesheet");
        link.setAttribute("href", "/bootstrap/css/bootstrap.min.css");
        head.addContent(link);

        Element jQueryScript = new Element("script");
        jQueryScript.setAttribute("src", "/jquery/jquery-2.1.4.min.js");
        jQueryScript.setText("//nothing");
        Element bootstrapScript = new Element("script");
        bootstrapScript.setAttribute("src", "/bootstrap/js/bootstrap.min.js");
        bootstrapScript.setText("//nothing");

        head.addContent(jQueryScript);
        head.addContent(bootstrapScript);

        return head;
    }

    static HTTPServer.ContextHandler getConfigHandler()
    {
        HTTPServer.ContextHandler handler = (request, response) ->
        {
            String message = "";
            switch (request.getMethod())
            {
                default:
                case "GET":
                    message = null;
                    break;
                case "POST":
                    short universe = Short.parseShort(request.getParams().get("universe"));
                    if (universe != Main.listener.getUniverse())
                    {
                        Main.listener.setUniverse(universe);
                    }
                    Main.configuration.setProperty("Universe", universe);
                    Main.listener.setListenAdapter(request.getParams().get("listenAdapter"));
                    Main.configuration.setProperty("ListenAdapter", Main.listener.getListenAdapter());
                    String serialPort = request.getParams().get("serialPort");
                    if(!Main.transmitter.GetAddress().equals(serialPort))
                    {
                        Main.transmitter.close();
                        Main.transmitter = new EnttecProTransmitter(serialPort);
                    }
                    Main.configuration.setProperty("SerialPort", Main.transmitter.GetAddress());

                    try
                    {
                        Main.configuration.save();
                    }
                    catch (ConfigurationException e)
                    {
                        e.printStackTrace();
                    }
                    message = "Saved successfully";
                    break;
            }

            String responseContent = getIndexContent(message);
            response.send(200, responseContent);

            return 0;
        };

        return handler;
    }

    private static Element getSerialPorts()
    {
        Element serialPort = new Element("select");
        serialPort.setAttribute("class", "form-control");
        serialPort.setAttribute("id", "serialPort");
        Enumeration<CommPortIdentifier> portIdentifiers = CommPortIdentifier
            .getPortIdentifiers();
        List<CommPortIdentifier> identifiers = Collections
            .list(portIdentifiers);

        for (CommPortIdentifier port : identifiers)
        {
            Element addressOption = new Element("option");
            addressOption.setAttribute("value", port.getName() + "");
            addressOption.setText(port.getName());
            serialPort.addContent(addressOption);
            if (Main.transmitter.GetAddress().equals(port.getName()))
            {
                addressOption.setAttribute("selected", "selected");
            }
        }

        return serialPort;
    }
}
