package miccah.laptopremote;

import android.os.AsyncTask;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class SendUDP extends AsyncTask {

    private byte[] IPv4ToBytes(String ip) {
        String[] bytes = ip.split("\\.");
        byte[] arr = new byte[4];
        for (int i = 0; i < 4; i++) {
            arr[i] = (byte)Integer.parseInt(bytes[i]);
        }
        return arr;
    }

    protected Object doInBackground(Object... objects) {
        String ip = (String) objects[0];
        Integer port = (Integer) objects[1];
        String message = (String) objects[2];
        try {
            DatagramSocket datagramSocket = new DatagramSocket();

            byte[] buffer = message.getBytes();
            InetAddress receiverAddress =
                InetAddress.getByAddress(IPv4ToBytes(ip));

            DatagramPacket packet = new DatagramPacket(
                    buffer, buffer.length, receiverAddress, port);
            datagramSocket.send(packet);
            datagramSocket.disconnect();
        }
        catch (Exception e) {};
        return null;
    }

    protected void onPostExecute(Object result) {
    }
}
