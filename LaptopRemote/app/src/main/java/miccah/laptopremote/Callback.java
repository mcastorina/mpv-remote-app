package miccah.laptopremote;

import org.json.JSONObject;

public interface Callback {
    public void callback(boolean result, JSONObject obj);
}
