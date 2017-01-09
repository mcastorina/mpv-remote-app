package miccah.laptopremote;

import android.os.AsyncTask;
// import android.widget.Toast;
import android.content.Context;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import org.json.JSONObject;

public class UDPPacket extends AsyncTask {

    private static final int MAX_RETRIES = 3;       // max number of sends
    private static final int ACK_TIMEOUT = 1000;    // milliseconds

    private Context context;
    private String ip;
    private Integer port;
    private String password;
    private String response;

    public UDPPacket(Context c, String ip, Integer port, String pass) {
        this.context = c;
        this.ip = ip;
        this.port = port;
        this.password = pass;
    }

    protected Object doInBackground(Object... objects) {
        // Expected inputs:
        //      Map<String, Object> message
        // Build message {"hmac": hmac, "message": message}
        HashMap<String, Object> map = (HashMap<String, Object>)objects[0];
        if (map == null) {
            send(ip, port, (String)objects[1]);
            return "";
        }

        // Add timestamp
        long time = System.currentTimeMillis();
        map.put("time", time);
        String message = new JSONObject(map).toString();

        map = new HashMap<String, Object>();
        map.put("hmac", new HMAC(password + time, message).digest());
        map.put("message", message);

        send(ip, port, new JSONObject(map).toString());
        return response;
    }
    protected void onPostExecute(Object result) {
        // Toast.makeText(context, result.toString(), Toast.LENGTH_LONG).show();
    }

    private boolean send(String ip, Integer port, String message) {
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
                ret = recv(datagramSocket);
                datagramSocket.disconnect();

                count++;
            }
        }
        catch (Exception e) {ret = false;}
        return ret;
    }
    private boolean recv(DatagramSocket sock) {
        try {
            byte[] buffer = new byte[1500];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            sock.setSoTimeout(ACK_TIMEOUT);
            sock.receive(packet);       // Blocks for ACK_TIMEOUT ms

            response = new String(buffer, 0, packet.getLength());
            // Check response
            JSONObject obj = new JSONObject(response);
            String message = obj.getString("message");
            String hmac = obj.getString("hmac");
            JSONObject mobj = new JSONObject(message);

            return mobj.getString("action").equals("ACK") &&
               new HMAC(password + mobj.getLong("time"),
                        message).digest().equals(hmac);
        }
        catch (SocketTimeoutException e) {}
        catch (Exception e) {}
        return false;
    }
}
