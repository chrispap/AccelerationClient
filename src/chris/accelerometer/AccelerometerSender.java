package chris.accelerometer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Locale;

import android.util.Log;

public class AccelerometerSender extends Thread implements AccelerometerListener {

    private final int      PORT     = 55888;
    private final int      BUF_SIZE = 32;
    private byte[]         mBufTx;
    private int            mBufTxSize;
    private boolean        mRunning;
    private boolean        mHostResolved;
    private boolean        mBufInFull;
    private String         mBufIn;
    private String         mHostIP;
    private InetAddress    mHostAddress;
    private DatagramSocket mSocket;
    private DatagramPacket mPacket;

    public AccelerometerSender(String ip) {
        mHostIP = ip;
        mBufTx = new byte[BUF_SIZE];
        resolveHost();
    }

    private boolean resolveHost() {
        if (!mHostResolved) {
            try {
                Log.println(Log.INFO, "Sender", "Trying to init UDP Packet");
                mSocket = new DatagramSocket();
                mHostAddress = InetAddress.getByName(mHostIP);
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
        byte[] db = mBufIn.getBytes();
        int i;
        for (i = 0; i < mBufIn.length(); i++)
            mBufTx[i] = db[i];
        mBufTxSize = i - 1;
        mBufInFull = false;
        notify();
    }

    private void send() {
        mPacket = new DatagramPacket(mBufTx, mBufTxSize, mHostAddress, PORT);

        if (mSocket != null && mPacket != null)
            try {
                mSocket.send(mPacket);
                Log.println(Log.INFO, "Sender",
                    "Sent data to ip: " + mPacket.getAddress().toString() + ":" + mPacket.getPort());
            } catch (IOException exc) {
                Log.println(Log.ERROR, "Sender", exc.getMessage());
            }
    }

    @Override
    public void run() {
        while (!mRunning || !mBufInFull) {
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

    private synchronized void putDataToBuffer(String data) {
        mBufIn = data;
        mBufInFull = true;
        notify();
    }

    public synchronized void changeIP(String ip) {
        mHostIP = ip;
        mHostResolved = false;
        resolveHost();
    }

    @Override
    public void onAccelerationChanged(float gx, float gy, float gz) {
        putDataToBuffer(String.format(Locale.US, "%5.3f:%5.3f:%5.3f", gx, gy, gz));
    }

}
