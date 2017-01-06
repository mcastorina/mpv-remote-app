package miccah.laptopremote;

import android.os.AsyncTask;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class UDPPacket extends AsyncTask {

    public static enum Task {SEND, SENDRECV};

    protected Object doInBackground(Object... objects) {
        Task task = (Task)objects[0];
        return send((String)objects[1],
                    (Integer)objects[2],
                    (String)objects[3],
                    task == Task.SENDRECV);
    }

    private Object send(String ip, Integer port, String message, boolean recv) {
        Object ret;
        try {
            DatagramSocket datagramSocket = new DatagramSocket();

            byte[] buffer = message.getBytes();
            InetAddress receiverAddress =
                InetAddress.getByName(ip);

            DatagramPacket packet = new DatagramPacket(
                    buffer, buffer.length, receiverAddress, port);
            datagramSocket.send(packet);
            if (recv) ret = recv(datagramSocket);
            else      ret = true;
            datagramSocket.disconnect();
        }
        catch (Exception e) {ret = false;};
        return ret;
    }
    private Object recv(DatagramSocket sock) {
        try {
            byte[] buffer = new byte[1500];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            sock.receive(packet);
            return new String(buffer, 0, packet.getLength());
        }
        catch (Exception e) {}
        return null;
    }

    protected void onPostExecute(Object result) {
    }
}
