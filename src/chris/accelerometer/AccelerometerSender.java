package chris.accelerometer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.util.Log;

public class AccelerometerSender extends Thread {

    final int       PORT           = 55888;
    final int       BUF_SIZE       = 32;
    private String  IP             = null;
    boolean         hostResolved   = false;
    InetAddress     address        = null;
    DatagramSocket  socket         = null;
    DatagramPacket  packet         = null;
    byte[]          sendBuffer     = null;
    int             sendBufferSize = 0;
    String          dataBuffer     = null;
    private boolean bufferFull     = false;
    private boolean running        = false;

    public AccelerometerSender(String ip) {
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
        }
    }

    public synchronized void stopSending() {
        running = false;
    }

    public synchronized void putDataToBuffer(String data) {
        dataBuffer = data;
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

        packet = new DatagramPacket(sendBuffer, sendBufferSize, address, PORT);

        if (socket != null && packet != null)
            try {
                socket.send(packet);
                Log.println(Log.INFO, "Sender",
                    "Sent data to ip: " + packet.getAddress().toString() + ":" + packet.getPort());
            } catch (IOException exc) {
                Log.println(Log.ERROR, "Sender", exc.getMessage());
            }
    }

    public void run() {
        while (true) {

            while (!running || !bufferFull) {
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
            }

            moveDataToSendBuffer();
            send();

        }
    }
}
