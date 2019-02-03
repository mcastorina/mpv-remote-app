package miccah.mpvremote;

import java.util.ArrayList;
import android.app.Activity;
import android.view.ViewGroup;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.BaseAdapter;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ViewFlipper;
import android.text.format.Formatter;
import android.text.InputType;
import android.net.wifi.WifiManager;
import org.json.JSONObject;
import java.util.HashMap;

public class SettingsAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private MainActivity activity;
    public ArrayList<ListItem> myItems = new ArrayList<ListItem>();

    public SettingsAdapter(MainActivity a) {
        activity = a;
        mInflater = (LayoutInflater)
            activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        String ipAddress = null;
        try {
            WifiManager wifiManager = (WifiManager)
                activity.getSystemService(Activity.WIFI_SERVICE);
            ipAddress = Formatter.formatIpAddress(
                    wifiManager.getConnectionInfo().getIpAddress());
        } catch (Exception e) {ipAddress = "Error";}

        /* Default Settings */
        // ipAddress first 3 octets of IPv4 address
        Settings.ipAddress = ipAddress.substring(0, ipAddress.lastIndexOf('.')+1);
        Settings.port = new Integer(28899);
        Settings.passwd = "";
        Settings.audio = 1;
        Settings.subtitle = 1;

        /* Search through subnet to see if the default port is running a server */
        for (int i = 1; i < 255; i++) {
            final String ipAddr = Settings.ipAddress + i;
            new UDPPacket(ipAddr, Settings.port, Settings.passwd, new Callback() {
                public void callback(boolean result, JSONObject obj) {
                    String addr = Settings.ipAddress;
                    boolean notFound = addr.charAt(addr.length() - 1) == '.';
                    if (result && notFound) {
                        Settings.ipAddress = ipAddr;
                        myItems.get(1).caption = Settings.ipAddress;
                        notifyDataSetChanged();
                        Toast.makeText(activity, "Found server",
                            Toast.LENGTH_SHORT).show();
                    }
                }
            }, false, 10).execute(null, "health");
        }

        /* Initialize list (Text, Hint, Focusable, InputType) */
        myItems.add( new ListItem(ListItem.TYPE.TEXT_VIEW,  null,                       "Settings",     InputType.TYPE_NULL) );
        myItems.add( new ListItem(ListItem.TYPE.EDIT_TEXT,  Settings.ipAddress,         "IP Address",   InputType.TYPE_CLASS_TEXT) );
        myItems.add( new ListItem(ListItem.TYPE.EDIT_TEXT,  Settings.port.toString(),   "Port",         InputType.TYPE_CLASS_NUMBER) );
        myItems.add( new ListItem(ListItem.TYPE.NULL,       null,                       null,           InputType.TYPE_NULL) );
        myItems.add( new ListItem(ListItem.TYPE.EDIT_TEXT,  Settings.passwd,            "Password",     InputType.TYPE_CLASS_TEXT) );
        myItems.add( new ListItem(ListItem.TYPE.NULL,       null,                       null,           InputType.TYPE_NULL) );
        myItems.add( new ListItem(ListItem.TYPE.NULL,       null,                       null,           InputType.TYPE_NULL) );
        myItems.add( new ListItem(ListItem.TYPE.SPINNER,    Settings.audio.toString(),  "Audio Track Number", InputType.TYPE_CLASS_NUMBER) );
        myItems.add( new ListItem(ListItem.TYPE.TEXT_VIEW,  null,                       "Audio Track",  InputType.TYPE_NULL) );
        myItems.add( new ListItem(ListItem.TYPE.SPINNER,    Settings.subtitle.toString(),  "Subtitle Track Number", InputType.TYPE_CLASS_NUMBER) );
        myItems.add( new ListItem(ListItem.TYPE.TEXT_VIEW,  null,                       "Subtitle Track", InputType.TYPE_NULL) );

        notifyDataSetChanged();
    }

    public int getCount() {
        return myItems.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.settings_item, null);
            holder.switcher = (ViewFlipper)
                convertView.findViewById(R.id.item_switcher);

            if (myItems.get(position).type == ListItem.TYPE.EDIT_TEXT) {
                while (holder.switcher.getDisplayedChild() != 1)
                    holder.switcher.showNext();
                holder.caption = (TextView)
                    holder.switcher.findViewById(R.id.item_edittext);
                holder.text = (EditText)
                    holder.switcher.findViewById(R.id.item_edittext);
            }
            else if (myItems.get(position).type == ListItem.TYPE.SPINNER) {
                while (holder.switcher.getDisplayedChild() != 2)
                    holder.switcher.showNext();
                holder.spinner = (Spinner)
                    holder.switcher.findViewById(R.id.item_spinner);
                // create a list of items for the spinner
                String[] items = new String[]{"1", "2", "three"};
                // create an adapter to describe how the items are displayed
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_dropdown_item, items);
                holder.spinner.setAdapter(adapter);
            }
            else {
                holder.caption = (TextView)
                    holder.switcher.findViewById(R.id.item_textview);
                holder.text = null;
            }
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

        if (myItems.get(position).type == ListItem.TYPE.SPINNER) {
            return convertView;
        }

        // Fill TextView with the value you have in data source
        holder.caption.setText(myItems.get(position).caption);
        holder.caption.setHint(myItems.get(position).hint);
        holder.caption.setFocusable(myItems.get(position).type == ListItem.TYPE.EDIT_TEXT);
        holder.caption.setId(position);
        holder.caption.setInputType(myItems.get(position).inputType);
        if (holder.text != null)
            holder.text.setSelection(holder.caption.length());
        holder.caption.setOnEditorActionListener(
            new TextView.OnEditorActionListener() {
                public boolean onEditorAction(
                        TextView view, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE ||
                        actionId == EditorInfo.IME_ACTION_NEXT) {
                            view.clearFocus();
                            View v = activity.getCurrentFocus();
                            if (v != null) {
                                InputMethodManager imm = (InputMethodManager)
                                    activity.getSystemService(
                                            Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(
                                        v.getWindowToken(), 0);
                            }
                            return true;
                    }
                    return false;
                }
            });

        // We need to update adapter once we finish with editing
        holder.caption.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    final int position = v.getId();
                    final TextView Caption = (TextView)v;
                    myItems.get(position).caption = Caption.getText().toString();
                    setInput(Caption);
                }
            }
        });

        return convertView;
    }

    private void setInput(TextView view) {
        String hint = view.getHint().toString();
        String value = view.getText().toString();
        if (hint.equals(myItems.get(1).hint)) {
            // IP Address
            Settings.ipAddress = value;
        }
        else if (hint.equals(myItems.get(2).hint)) {
            // Port
            Settings.port = Integer.parseInt(value);
        }
        else if (hint.equals(myItems.get(4).hint)) {
            // Password
            Settings.passwd = value;
        }
        else if (hint.equals(myItems.get(7).hint)) {
            // Audio Track
            Settings.audio = Integer.parseInt(value);
            // TODO: send track once connected
            sendCommand("set_audio", "track", Settings.audio);
        }
        else if (hint.equals(myItems.get(9).hint)) {
            // Subtitle Track
            Settings.subtitle = Integer.parseInt(value);
            // TODO: send track once connected
            sendCommand("set_subtitles", "track", Settings.subtitle);
        }
    }

    private void sendCommand(String cmd, Object... pairs) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("command", cmd);
        for (int i = 0; i < pairs.length - 1; i += 2) {
            map.put((String)pairs[i], pairs[i+1]);
        }

        send(map);
    }
    private void send(HashMap<String, Object> cmd) {
        // Check settings
        if (Settings.ipAddress != null &&
            Settings.port      != null &&
            Settings.passwd    != null) {
            new UDPPacket(Settings.ipAddress,
                          Settings.port,
                          Settings.passwd, null).execute(cmd);
        }
    }
}

class ViewHolder {
    ViewFlipper switcher;
    TextView caption;
    EditText text;
    Spinner spinner;
}

class ListItem {
    TYPE type;              // type of item
    /* text options */
    String caption;         // user input
    String hint;            // preview when view is empty
    int inputType;          // input type for text input
    /* spinner options */

    public enum TYPE {
        NULL,
        TEXT_VIEW,
        EDIT_TEXT,
        SPINNER;
    }

    public ListItem(TYPE t, String c, String h, int i) {
        type = t;
        caption = c;
        hint = h;
        inputType = i;
    }
}
