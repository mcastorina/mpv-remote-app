package miccah.mpvremote;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class HMAC {

    private String keyString;
    private String message;
    private String algorithm;

    public HMAC(String key, String msg) {
        this(key, msg, "HmacMD5");
    }
    public HMAC(String key, String msg, String algo) {
        this.keyString = key;
        this.message = msg;
        this.algorithm = algo;
    }

    public String digest() {
        String digest = null;
        try {
            SecretKeySpec key = new SecretKeySpec(
                    keyString.getBytes("UTF-8"), algorithm);
            Mac mac = Mac.getInstance(algorithm);
            mac.init(key);

            byte[] bytes = mac.doFinal(message.getBytes("ASCII"));

            StringBuffer hash = new StringBuffer();
            for (int i = 0; i < bytes.length; i++) {
                String hex = Integer.toHexString(0xFF & bytes[i]);
                if (hex.length() == 1) {
                    hash.append('0');
                }
                hash.append(hex);
            }
            digest = hash.toString();
        } catch (Exception e) {}
        return digest;
    }
}
