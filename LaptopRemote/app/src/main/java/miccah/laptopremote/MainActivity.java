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
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.content.res.Configuration;

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
                    sendCommand("key 0");
                    diff -= 2;
                }
                while (diff < 0) {
                    sendCommand("key 9");
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

    public void playPauseButton(View view) {
        sendCommand("key p");
    }
    public void rewindButton(View view) {
        sendCommand("key Left");
    }
    public void fastForwardButton(View view) {
        sendCommand("key Right");
    }
    public void subtitlesButton(View view) {
        sendCommand("key v");
    }

    private void sendCommand(String cmd) {
        try {
            /*
             * FIXME: not intuitive using getChild
             * Very dependent on SettingsAdapter
             */
            ViewSwitcher vs;
            vs = (ViewSwitcher) ((LinearLayout) mDrawerList.getChildAt(1)).getChildAt(1);
            String ip = ((TextView) vs.getChildAt(1)).getText().toString();

            vs = (ViewSwitcher) ((LinearLayout) mDrawerList.getChildAt(2)).getChildAt(1);
            Integer port = Integer.parseInt(
                    ((TextView) vs.getChildAt(1)).getText().toString());
            new SendUDP().execute(ip, port, cmd);
        } catch (Exception e) {}
    }

    public static void log(String message) {
        new SendUDP().execute("192.168.254.22", new Integer(12345), message);
    }
}
