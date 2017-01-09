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
import android.widget.EditText;
import android.widget.ViewSwitcher;
import android.text.format.Formatter;
import android.text.InputType;
import android.net.wifi.WifiManager;

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

        /* Initialize list (Text, Hint, Focusable, InputType) */
        myItems.add(new ListItem(null, "Settings", false, InputType.TYPE_NULL));
        myItems.add(new ListItem(
                    ipAddress.substring(0, ipAddress.lastIndexOf('.')+1),
                    "IP Address", true, InputType.TYPE_CLASS_TEXT));
        myItems.add(new ListItem("", "Port", true, InputType.TYPE_CLASS_NUMBER));
        myItems.add(new ListItem(null, null, false, InputType.TYPE_NULL));
        myItems.add(new ListItem("", "Password", true, InputType.TYPE_CLASS_TEXT));
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
            holder.switcher = (ViewSwitcher)
                convertView.findViewById(R.id.item_switcher);

            if (myItems.get(position).focusable) {
                if (holder.switcher.getDisplayedChild() == 0)
                    holder.switcher.showNext();
                holder.caption = (TextView)
                    holder.switcher.findViewById(R.id.item_edittext);
                holder.text = (EditText)
                    holder.switcher.findViewById(R.id.item_edittext);
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

        // Fill TextView with the value you have in data source
        holder.caption.setText(myItems.get(position).caption);
        holder.caption.setHint(myItems.get(position).hint);
        holder.caption.setFocusable(myItems.get(position).focusable);
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
            activity.ipAddress = value;
        }
        else if (hint.equals(myItems.get(2).hint)) {
            // Port
            activity.port = value;
        }
        else if (hint.equals(myItems.get(4).hint)) {
            // Password
            activity.passwd = value;
        }
    }
}

class ViewHolder {
    ViewSwitcher switcher;
    TextView caption;
    EditText text;
}

class ListItem {
    String caption;
    String hint;
    boolean focusable;
    int inputType;

    public ListItem(String c, String h, boolean f, int i) {
        caption = c;
        hint = h;
        focusable = f;
        inputType = i;
    }
}
