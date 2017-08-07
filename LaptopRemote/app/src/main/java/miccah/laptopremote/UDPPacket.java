package miccah.laptopremote;

import android.os.AsyncTask;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import org.json.JSONObject;

public class UDPPacket extends AsyncTask {

    private static final int MAX_RETRIES = 3;       // max number of sends
    private static final int ACK_TIMEOUT = 800;     // milliseconds
    private static final int RECV_BUF_SIZE = 4096;  // bytes

    private String ip;
    private Integer port;
    private String password;
    private String response;
    private Callback cb;

    public UDPPacket(String ip, Integer port, String pass, Callback cb) {
        this.ip = ip;
        this.port = port;
        this.password = pass;
        this.cb = cb;
    }

    protected Boolean doInBackground(Object... objects) {
        // Expected inputs:
        //      Map<String, Object> message
        // Build message {"hmac": hmac, "message": message}
        HashMap<String, Object> map = (HashMap<String, Object>)objects[0];
        if (map == null) {
            return send(ip, port, (String)objects[1], 0);
        }

        // Add timestamp
        long time = System.currentTimeMillis();
        map.put("time", time);
        String message = new JSONObject(map).toString();

        // Construct hmac and message object
        map = new HashMap<String, Object>();
        map.put("hmac", new HMAC(password + time, message).digest());
        map.put("message", message);

        return send(ip, port, new JSONObject(map).toString(), time);
    }
    protected void onPostExecute(Object result) {
        if (cb != null) {
            try {
                cb.callback((Boolean)result, new JSONObject(response));
            } catch (Exception e) {cb.callback((Boolean)result, null);}
        }
    }

    private boolean send(String ip, Integer port, String message, long ts) {
        boolean ret = false;
        int count = 0;
        try {
            while (!ret && count < MAX_RETRIES) {
                DatagramSocket datagramSocket = new DatagramSocket();

                byte[] buffer = message.getBytes();
                InetAddress receiverAddress =
                    InetAddress.getByName(ip);

                DatagramPacket packet = new DatagramPacket(
                        buffer, buffer.length, receiverAddress, port);
                datagramSocket.send(packet);
                ret = recv(datagramSocket, ts);
                datagramSocket.disconnect();

                count++;
            }
        }
        catch (Exception e) {ret = false;}
        return ret;
    }
    private boolean recv(DatagramSocket sock, long timestamp) {
        try {
            byte[] buffer = new byte[RECV_BUF_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            sock.setSoTimeout(ACK_TIMEOUT);
            sock.receive(packet);       // Blocks for ACK_TIMEOUT ms

            response = new String(buffer, 0, packet.getLength());
            // Check response
            JSONObject obj = new JSONObject(response);
            String message = obj.getString("message");
            String hmac = obj.getString("hmac");
            JSONObject mobj = new JSONObject(message);

            boolean done = mobj.getLong("action") == timestamp &&
                               new HMAC(password + mobj.getLong("time"),
                               message).digest().equals(hmac);
            if (done) {
                response = message;
            }
            return done;
        }
        catch (SocketTimeoutException e) {}
        catch (Exception e) {}
        return false;
    }
}
