package chris.accelerometer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.util.Log;

public class Sender extends Thread {

    final int PORT = 55832;
    final int BUF_SIZE = 256;

    private String IP = "192.168.2.12";

    boolean hostResolved = false;
    InetAddress address = null;
    DatagramSocket socket = null;
    DatagramPacket packet = null;

    byte[] sendBuffer = null;
    int sendBufferSize = 0;
    String dataBuffer = null;
    private boolean bufferFull = false;

    private boolean running = false;

    public Sender(String ip) {
        IP = ip;
        sendBuffer = new byte[BUF_SIZE];
        resolveHost();
    }

    public synchronized void changeIP(String ip) {
        IP = ip;
        hostResolved = false;
        resolveHost();
    }

    private boolean resolveHost() {
        if (!hostResolved) {
            try {
                Log.println(Log.INFO, "Sender", "Trying to init UDP Packet");
                socket = new DatagramSocket();
                address = InetAddress.getByName(IP);
            } catch (SocketException exc) {
                Log.println(Log.ERROR, "Sender", exc.getMessage());
            } catch (UnknownHostException exc) {
                Log.println(Log.ERROR, "Sender", exc.getMessage());
            }
            hostResolved = true;
        }
        return hostResolved;
    }

    public synchronized void startSending() {
        if (hostResolved) {
            running = true;
            if (!isAlive()) start();
            notify();
            Log.println(Log.INFO, "Sender", "Started sending");
        } else {
            Log.println(Log.ERROR, "Sender", "Host isnt resolved");
        }
    }

    public synchronized void stopSending() {
        running = false;
    }

    public synchronized void putDataToBuffer(String data) {
        dataBuffer = data;
        data = "2.0:2.0:3.0";
        bufferFull = true;
        notify();
    }

    private synchronized void moveDataToSendBuffer() {
        byte[] db = dataBuffer.getBytes();
        int i;
        for (i = 0; i < dataBuffer.length(); i++)
            sendBuffer[i] = db[i];
        sendBufferSize = i - 1;
        bufferFull = false;
        notify();
    }

    private void send() {

        packet = new DatagramPacket(sendBuffer, sendBufferSize, address, 55832);

        if (socket != null && packet != null) try {
            socket.send(packet);
            Log.println(Log.INFO, "Sender", "Sent data");
        } catch (IOException exc) {
            Log.println(Log.ERROR, "Sender", exc.getMessage());
        }
    }

    public void run() {
        while (true) {

            while (!running || !bufferFull) {
                synchronized (this) {
                    try {
                        Log.println(Log.DEBUG, "Sender", "Buffer empty. Going to sleep");
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
                Log.println(Log.DEBUG, "Sender", "Someone woke me up");
            }

            moveDataToSendBuffer();
            send();

        }
    }
}
