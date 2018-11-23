/*
 * Wrapper for ToggleButton class.
 *
 * This class will disable the button when pressed until it receives a
 * result from a UDPPacket.
 */
package miccah.mpvremote;

import android.widget.ToggleButton;
import android.widget.Toast;
import android.content.Context;
import android.util.AttributeSet;
import android.graphics.LightingColorFilter;
import org.json.JSONObject;
import java.util.HashMap;

public class BackgroundToggleButton extends ToggleButton implements Callback {
    private Context context = null;
    private boolean buttonState;
    private int onDrawable;
    private int offDrawable;

    public BackgroundToggleButton(Context c) {
        super(c);
        this.context = c;
        buttonState = this.isChecked();
    }
    public BackgroundToggleButton(Context c, AttributeSet a) {
        super(c, a);
        this.context = c;
        buttonState = this.isChecked();
    }
    public BackgroundToggleButton(Context c, AttributeSet a, int dsa) {
        super(c, a, dsa);
        this.context = c;
        buttonState = this.isChecked();
    }
    public BackgroundToggleButton(Context c, AttributeSet a, int dsa, int dsr) {
        super(c, a, dsa, dsr);
        this.context = c;
        buttonState = this.isChecked();
    }

    public void setDrawables(int on, int off) {
        this.onDrawable = on;
        this.offDrawable = off;
    }
    public boolean getState() {
        return buttonState;
    }

    public void setProperty(String property, Object value) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("command", "set");
        map.put("property", property);
        map.put("value", value);

        setButtonState(false);
        send(map);
    }

    private void send(HashMap<String, Object> cmd) {
        // Check settings
        if (Settings.ipAddress != null &&
            Settings.port      != null &&
            Settings.passwd    != null) {
            new UDPPacket(Settings.ipAddress,
                          Settings.port,
                          Settings.passwd, this).execute(cmd);
        }
        else {
            Toast.makeText(context, "Please check settings",
                Toast.LENGTH_SHORT).show();
            callback(false, null);
        }
    }
    private void setButtonState(boolean enabled) {
        this.setClickable(enabled);
        if (enabled) {
            this.getBackground().clearColorFilter();
        }
        else {
            this.getBackground().setColorFilter(
                new LightingColorFilter(0x01010101, 0x00808080));
        }
    }

    public void callback(boolean result, JSONObject message) {
        setButtonState(true);
        boolean success = false;
        try {success = message.getBoolean("result");} catch (Exception e) {}

        if (result && success) {
            buttonState = !buttonState;
        }
        else {
            Toast.makeText(context, "Failed",
                Toast.LENGTH_SHORT).show();
        }
        this.setChecked(buttonState);
    }
}
