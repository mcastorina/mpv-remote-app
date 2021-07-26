package miccah.mpvremote;

import android.widget.ListView;

import java.util.ArrayList;

public class Settings {
    protected final static Integer MIN_VOLUME = 0;
    protected final static Integer MAX_VOLUME = 130;

    protected static String ipAddress;
    protected static Integer port;
    protected static String passwd;
    protected static Integer audio;
    protected static Integer subtitle;
    protected static ArrayList<String> audio_tracks;
    protected static ArrayList<String> subtitle_tracks;
    protected static ListView mDrawerList;
    protected static Integer volumeStep = 5;
}
