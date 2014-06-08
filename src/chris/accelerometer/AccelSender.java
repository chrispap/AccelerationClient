package chris.accelerometer;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Map;

import android.util.Log;

public class AccelSender extends Thread implements AccelListener {

    private static final String USER_AGENT = "Mozilla/5.0";

    private final int           PORT       = 55888;
    private final int           BUF_SIZE   = 32;
    private byte[]              mBufTx;
    private int                 mBufTxSize;
    private boolean             mRunning;
    private boolean             mHostResolved;
    private boolean             mBufInFull;
    private String              mBufIn;
    private String              mHostIP;
    private InetAddress         mHostAddress;
    private DatagramSocket      mSocket;
    private DatagramPacket      mPacket;

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

    public static int sendPost(Map<String, String> dataMap, String urlStr) throws Exception {

        URL obj = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        /* Add reuqest header */
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

        /* Construct the post data */
        StringBuffer post_data = new StringBuffer();
        for (Map.Entry<String, String> entry : dataMap.entrySet())
            post_data.append(entry.getKey() + "=" + entry.getValue() + "&");
        if (post_data.length() > 0) post_data.setLength(post_data.length() - 1);

        /* Send post request */
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(post_data.toString());
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return responseCode;
    }

}
