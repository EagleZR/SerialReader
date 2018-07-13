package org.javelins;

import org.apache.commons.net.telnet.TelnetClient;

import java.io.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SerialReader implements Runnable {

    private static String[] servers = {"10.16.24.100", "10.16.24.102"};
    private static int startPort = 2001;
    private static int endPort = 2016;
    private static Pattern ipv4Pattern = Pattern.compile("\\d+\\x46\\d+\\x46\\d+\\x46\\d+");
    private static Pattern ipv6Pattern = Pattern.compile("[0-9a-z]+:[0-9a-z]+:[0-9a-z]+:[0-9a-z]+:[0-9a-z]+:[0-9a-z]+");
    private String server_ip_address;
    private String client_ip_address;
    private int port;

    public static void main(String[] args) {
        ArrayList<SerialReader> readers = new ArrayList<SerialReader>();
        ArrayList<Thread> threads = new ArrayList<Thread>();

        // Create Readers and Threads and start Threads
        for (String server : servers) {
            for (int port = startPort; port <= endPort; port++) {
                SerialReader reader = new SerialReader(server, port);
                readers.add(reader);
                Thread thread = new Thread(reader);
                threads.add(thread);
                thread.start();
            }
        }

        // Wait until all Threads are finished
        while (!threads.isEmpty()) {
            for (int i = 0; i < threads.size(); i++) {
                if (!threads.get(i).isAlive()) {
                    threads.remove(i);
                }
            }
        }

        // Print Reader data
        for (SerialReader reader : readers) {
            System.out.println(reader.getServer_ip_address() + ":" + reader.getPort() + "\t" + reader.getClient_ip_address());
        }

    }

    private SerialReader(String ip_address, int port) {
        this.server_ip_address = ip_address;
        this.port = port;
    }

    public void run() {
        TelnetClient telnetClient = new TelnetClient();
        telnetClient.setConnectTimeout(1000);
        try {
            // Connect
            telnetClient.connect(server_ip_address, port);
            BufferedReader in = new BufferedReader(new InputStreamReader(telnetClient.getInputStream()));

            // Send command
            telnetClient.getOutputStream().write("ifconfig eth0\n".getBytes());
            telnetClient.getOutputStream().flush();

            // Read Response
            long timeout = 2000;
            long startTime = System.currentTimeMillis();
            String readLine = "";
            do {
                if (in.ready() && telnetClient.isConnected()) {
                    readLine = in.readLine();
                }
            }
            while (!readLine.contains("txqueuelen") && !readLine.contains("root") /*if it's in the DTE console*/
                    && System.currentTimeMillis() - startTime < timeout);

            // Extract IP address
            Matcher ipv4Matcher = ipv4Pattern.matcher(readLine);
            Matcher ipv6Matcher = ipv6Pattern.matcher(readLine);

            this.client_ip_address = ipv4Matcher.find() ? ipv4Matcher.group(0) : (ipv6Matcher.find() ? ipv6Matcher.group(0) : "");

        } catch (IOException e) {
            System.out.println("The client could not be connected to " + server_ip_address + ":" + port);
            e.printStackTrace();
        } finally {
            if (telnetClient.isConnected()) {
                try {
                    telnetClient.disconnect();
                } catch (IOException e) {
                    System.out.println("The client" + server_ip_address + ":" + port + " could not be disconnected");
                    e.printStackTrace();
                }
            }
        }
    }

    private String getServer_ip_address() {
        return server_ip_address;
    }

    private String getClient_ip_address() {
        return client_ip_address;
    }

    private int getPort() {
        return port;
    }
}
