package miccah.laptopremote;

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
import android.widget.TextView;
import android.text.format.Formatter;
import android.net.wifi.WifiManager;

public class SettingsAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private Activity activity;
    public ArrayList<ListItem> myItems = new ArrayList<ListItem>();

    public SettingsAdapter(Activity a) {
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

        /* Initialize list (Text, Hint, Focusable) */
        myItems.add(new ListItem(null, "Settings", false));
        myItems.add(new ListItem(null, "IP Address", true));
        myItems.add(new ListItem(null, "Port", true));
        myItems.add(new ListItem(null, null, false));
        myItems.add(new ListItem(null, "Your IP Address", false));
        myItems.add(new ListItem(ipAddress, null, false));
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
            convertView = mInflater.inflate(R.layout.item, null);
            holder.caption = (TextView) convertView
                .findViewById(R.id.item_caption);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }
        // Fill TextView with the value you have in data source
        holder.caption.setText(myItems.get(position).caption);
        holder.caption.setHint(myItems.get(position).hint);
        holder.caption.setFocusable(myItems.get(position).focusable);
        holder.caption.setId(position);
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
                    }
                    return true;
                }
            });

        // We need to update adapter once we finish with editing
        holder.caption.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    final int position = v.getId();
                    final TextView Caption = (TextView)v;
                    myItems.get(position).caption = Caption.getText().toString();
                }
            }
        });

        return convertView;
    }
}

class ViewHolder {
    TextView caption;
}

class ListItem {
    String caption;
    String hint;
    boolean focusable;

    public ListItem(String c, String h, boolean f) {
        caption = c;
        hint = h;
        focusable = f;
    }
}
