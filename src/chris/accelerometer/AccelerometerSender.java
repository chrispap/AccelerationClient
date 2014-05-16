package chris.accelerometer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.util.Log;

public class AccelerometerSender extends Thread {

    private final int      PORT     = 55888;
    private final int      BUF_SIZE = 32;
    private byte[]         mSendBuffer;
    private int            mSendBufferSize;
    private boolean        mBufferFull;
    private boolean        mRunning;
    private boolean        mHostResolved;
    private String         mIP;
    private String         mDataBuffer;
    private InetAddress    mAddress;
    private DatagramSocket mSocket;
    private DatagramPacket mPacket;

    public AccelerometerSender(String ip) {
        mIP = ip;
        mSendBuffer = new byte[BUF_SIZE];
        resolveHost();
    }

    private boolean resolveHost() {
        if (!mHostResolved) {
            try {
                Log.println(Log.INFO, "Sender", "Trying to init UDP Packet");
                mSocket = new DatagramSocket();
                mAddress = InetAddress.getByName(mIP);
            } catch (SocketException exc) {
                Log.println(Log.ERROR, "Sender", exc.getMessage());
            } catch (UnknownHostException exc) {
                Log.println(Log.ERROR, "Sender", exc.getMessage());
            }
            mHostResolved = true;
        }
        return mHostResolved;
    }

    private synchronized void moveDataToSendBuffer() {
        byte[] db = mDataBuffer.getBytes();
        int i;
        for (i = 0; i < mDataBuffer.length(); i++)
            mSendBuffer[i] = db[i];
        mSendBufferSize = i - 1;
        mBufferFull = false;
        notify();
    }

    private void send() {

        mPacket = new DatagramPacket(mSendBuffer, mSendBufferSize, mAddress, PORT);

        if (mSocket != null && mPacket != null)
            try {
                mSocket.send(mPacket);
                Log.println(Log.INFO, "Sender",
                    "Sent data to ip: " + mPacket.getAddress().toString() + ":" + mPacket.getPort());
            } catch (IOException exc) {
                Log.println(Log.ERROR, "Sender", exc.getMessage());
            }
    }

    public synchronized void startSending() {
        if (mHostResolved) {
            mRunning = true;
            if (!isAlive()) start();
            notify();
        }
    }

    public synchronized void stopSending() {
        mRunning = false;
    }

    public synchronized void putDataToBuffer(String data) {
        mDataBuffer = data;
        mBufferFull = true;
        notify();
    }

    public synchronized void changeIP(String ip) {
        mIP = ip;
        mHostResolved = false;
        resolveHost();
    }

    public void run() {
        while (true) {

            while (!mRunning || !mBufferFull) {
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
