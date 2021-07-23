package miccah.mpvremote;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.ViewSwitcher;
import android.widget.ToggleButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.text.TextUtils;
import android.graphics.LightingColorFilter;
import java.util.HashMap;
import java.util.ArrayList;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private Integer mVolume;
    private Toast mToast;
    private SeekBar mSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BackgroundToggleButton subtitles = (miccah.mpvremote.BackgroundToggleButton)
                findViewById(R.id.subtitles);
        subtitles.setChecked(PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("SUBTITLES", true));

        /* Setup side menu */
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        Settings.mDrawerList = (ListView) findViewById(R.id.left_drawer);
        Settings.mDrawerList.setItemsCanFocus(true);
        Settings.mDrawerList.setAdapter(new SettingsAdapter(this));

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

        /* Setup Toast */
        mToast = Toast.makeText(MainActivity.this, "", Toast.LENGTH_SHORT);

        /* Setup volume */
        mVolume = 100;

        /* Setup volume bar */
        mSeekBar = (SeekBar)findViewById(R.id.volume_bar);
        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress,
                boolean fromUser) {
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                mVolume = seekBar.getProgress();
                sendCommand(null, "set_volume", "volume", mVolume);
                displayVolume();
            }
        });

        /* Setup FF / REW */
        ((ImageButton)findViewById(R.id.fast_forward)).
            setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        Object[] args = new Object[3];
                        args[0] = "seek";
                        args[1] = "seconds";
                        args[2] = 5;
                        sendCommand(null, "repeat", "args", args);
                    }
                    else if (event.getAction() == MotionEvent.ACTION_UP) {
                        sendCommand(null, "stop");
                    }
                    return false;
                }
        });
        ((ImageButton)findViewById(R.id.rewind)).
            setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        Object[] args = new Object[3];
                        args[0] = "seek";
                        args[1] = "seconds";
                        args[2] = -5;
                        sendCommand(null, "repeat", "args", args);
                    }
                    else if (event.getAction() == MotionEvent.ACTION_UP) {
                        sendCommand(null, "stop");
                    }
                    return false;
                }
        });

        /* Setup BackgroundImageButton drawable states */
        ((BackgroundImageButton)findViewById(R.id.play_pause)).
            setDrawables(android.R.drawable.ic_media_pause,
                         android.R.drawable.ic_media_play);
        ((BackgroundImageButton)findViewById(R.id.full_screen)).
            setDrawables(R.drawable.fullscreen,
                         R.drawable.fullscreen_exit);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
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
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(Settings.mDrawerList);
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

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
            switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    if (mVolume < Settings.MAX_VOLUME) {
                        mVolume = Math.min(mVolume + Settings.volumeStep, Settings.MAX_VOLUME);
                        mSeekBar.setProgress(mVolume);
                        sendCommand(null, "set_volume", "volume", mVolume);
                    }
                    displayVolume();
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    if (mVolume > Settings.MIN_VOLUME) {
                        mVolume = Math.max(mVolume - Settings.volumeStep, Settings.MIN_VOLUME);
                        mSeekBar.setProgress(mVolume);
                        sendCommand(null, "set_volume", "volume", mVolume);
                    }
                    displayVolume();
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
            }
        }

    public void displayVolume() {
        try {mToast.cancel();} catch (Exception e){}
        mToast = Toast.makeText(MainActivity.this, "Volume: " + mVolume + "%", Toast.LENGTH_SHORT);
        mToast.show();
        showProperty("volume", null, "%");
    }
    public void libraryButton(View view) {
        Intent intent = new Intent(this, LibraryActivity.class);
        startActivity(intent);
    }
    public void playPauseButton(View view) {
        BackgroundImageButton button = (BackgroundImageButton)view;
        button.sendCommand("pause", "state", !button.getState());
    }
    public void subtitlesButton(View view) {
        BackgroundToggleButton button = (BackgroundToggleButton)view;
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putBoolean("SUBTITLES", button.isChecked()).apply();
        button.sendCommand("set_subtitles", "track", button.isChecked() ? Settings.subtitle : 0);
    }
    public void fullScreenButton(View view) {
        BackgroundImageButton button = (BackgroundImageButton)view;
        button.sendCommand("fullscreen", "state", !button.getState());
    }
    public void settingsButton(View view) {
        mDrawerLayout.openDrawer(Settings.mDrawerList);
    }

    private void sendCommand(Callback cb, String command, Object... pairs) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("command", command);
        for (int i = 0; i < pairs.length - 1; i += 2) {
            map.put((String)pairs[i], pairs[i+1]);
        }
        send(map, cb);
    }
    private void showProperty(String property, String pre, String post) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("command", "show");
        map.put("property", property);
        map.put("pre", pre);
        map.put("post", post);
        send(map, null);
    }
    private void send(HashMap<String, Object> cmd, Callback cb) {
        try {
            new UDPPacket(Settings.ipAddress,
                          Settings.port,
                          Settings.passwd, cb).execute(cmd);
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Please check settings",
                    Toast.LENGTH_SHORT).show();
            if (cb != null) cb.callback(false, null);
        }
    }
}
