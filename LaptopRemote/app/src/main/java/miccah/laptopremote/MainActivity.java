package miccah.laptopremote;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.SearchManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import java.util.ArrayList;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

public class MainActivity extends Activity {
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Setup side menu */
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setItemsCanFocus(true);
        mDrawerList.setAdapter(new TextViewAdapter());

        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_menu_gallery,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
                ) {
            public void onDrawerClosed(View view) {
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        /* Setup volume bar */
        SeekBar volumeControl = (SeekBar)findViewById(R.id.volume_bar);
        volumeControl.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            int start = 0;
            public void onProgressChanged(SeekBar seekBar, int progress,
                boolean fromUser) {
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                start = seekBar.getProgress();
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                int diff = seekBar.getProgress() - start;
                diff &= ~1; // Make diff an even number
                seekBar.setProgress(diff + start);

                Toast.makeText(MainActivity.this, "Volume (" +
                        (start + diff) + ")", 
                        Toast.LENGTH_SHORT).show();

                /* Send UDP command until we reach appropriate volume */
                while (diff > 0) {
                    new SendUDP().execute("key 0");
                    diff -= 2;
                }
                while (diff < 0) {
                    new SendUDP().execute("key 9");
                    diff += 2;
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    public class TextViewAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        public ArrayList<ListItem> myItems = new ArrayList<ListItem>();

        public TextViewAdapter() {
            mInflater = (LayoutInflater)
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);


            String ipAddress = null;
            try {
                WifiManager wifiManager = (WifiManager)
                    getSystemService(WIFI_SERVICE);
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

    public void playPauseButton(View view) {
        new SendUDP().execute("key p");
    }
    public void rewindButton(View view) {
        new SendUDP().execute("key Left");
    }
    public void fastForwardButton(View view) {
        new SendUDP().execute("key Right");
    }
    public void subtitlesButton(View view) {
        new SendUDP().execute("key v");
    }
}
