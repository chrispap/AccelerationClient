package chris.accelerometer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Locale;

import android.util.Log;

public class AccelSender extends Thread implements AccelListener {

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

    public AccelSender(String ip) {
        mHostIP = ip;
        mBufTx = new byte[BUF_SIZE];
        resolveHost();
    }

    private boolean resolveHost() {
        if (!mHostResolved) {
            try {
                mSocket = new DatagramSocket();
                mHostAddress = InetAddress.getByName(mHostIP);
            } catch (SocketException exc) {
                Log.e("accel.sender", exc.getMessage());
            } catch (UnknownHostException exc) {
                Log.e("accel.sender", exc.getMessage());
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
        notifyAll();
    }

    private void send() {
        mPacket = new DatagramPacket(mBufTx, mBufTxSize, mHostAddress, PORT);

        if (mSocket != null && mPacket != null)
            try {
                mSocket.send(mPacket);
            } catch (IOException exc) {
                Log.e("accel.sender", exc.getMessage());
            }
    }

    @Override
    public void run() {
        do {
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
        } while (true);
    }

    public synchronized void startSending() {
        if (mHostResolved) {
            mRunning = true;
            if (!isAlive()) start();
            notifyAll();
        }
    }

    public synchronized void stopSending() {
        mRunning = false;
    }

    private synchronized void putDataToBuffer(String data) {
        mBufIn = data;
        mBufInFull = true;
        notifyAll();
    }

    public synchronized void changeIP(String ip) {
        mHostIP = ip;
        mHostResolved = false;
        resolveHost();
    }

    @Override
    public void onAccelChanged(float gx, float gy, float gz) {
        putDataToBuffer(String.format(Locale.US, "%5.3f:%5.3f:%5.3f", gx, gy, gz));
    }

}
