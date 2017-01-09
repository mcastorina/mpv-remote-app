package miccah.laptopremote;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.ViewSwitcher;
import android.widget.ToggleButton;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.content.res.Configuration;
import android.text.TextUtils;
import java.util.HashMap;
import java.util.ArrayList;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    public String ipAddress;
    public String port;
    public String passwd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Setup side menu */
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setItemsCanFocus(true);
        mDrawerList.setAdapter(new SettingsAdapter(this));

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
            public void onProgressChanged(SeekBar seekBar, int progress,
                boolean fromUser) {
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(MainActivity.this, "Volume (" +
                        seekBar.getProgress() + ")",
                        Toast.LENGTH_SHORT).show();

                setProperty("volume", seekBar.getProgress());
                showProperty("volume", null, "%");
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
        // Pass any configuration change to the drawer toggle
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    public void playPauseButton(View view) {
        sendCommand("cycle pause");
    }
    public void rewindButton(View view) {
        sendCommand("seek", "-5");
    }
    public void fastForwardButton(View view) {
        sendCommand("seek", "5");
    }
    public void subtitlesButton(View view) {
        ToggleButton button = (ToggleButton)view;
        setProperty("sub", button.isChecked() ? 1 : 0);
    }

    private void sendCommand(String command, String ... args) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("command", command);
        map.put("args", args);
        send(map);
    }
    private void getProperty(String property) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("command", "get");
        map.put("property", property);
        send(map);
    }
    private void setProperty(String property, Object value) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("command", "set");
        map.put("property", property);
        map.put("value", value);
        send(map);
    }
    private void showProperty(String property, String pre, String post) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("command", "show");
        map.put("property", property);
        map.put("pre", pre);
        map.put("post", post);
        send(map);
    }
    private void send(HashMap<String, Object> cmd) {
        try {
            new UDPPacket(MainActivity.this,
                    ipAddress, Integer.parseInt(port), passwd).execute(cmd);
        } catch (Exception e) {}
    }
}
