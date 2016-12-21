package miccah.laptopremote;

import android.os.AsyncTask;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class SendUDP extends AsyncTask {

    /* FIXME: hard coded port and address should be user supplied */
    protected Object doInBackground(Object... objects) {
        for (Object object : objects) {
            String message = (String)object;
            try {
                DatagramSocket datagramSocket = new DatagramSocket();

                byte[] buffer = message.getBytes();
                InetAddress receiverAddress =
                    InetAddress.getByAddress(new byte[] {
                        (byte)192, (byte)168, (byte)1, (byte)96}
                        );

                DatagramPacket packet = new DatagramPacket(
                        buffer, buffer.length, receiverAddress, 12345);
                datagramSocket.send(packet);
                datagramSocket.disconnect();
            }
            catch (Exception e) {};
        }
        return null;
    }

    protected void onPostExecute(Object result) {
    }
}
