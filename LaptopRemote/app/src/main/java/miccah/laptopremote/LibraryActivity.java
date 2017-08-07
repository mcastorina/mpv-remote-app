package miccah.laptopremote;

import java.util.ArrayList;
import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Loader;
import android.content.Context;
import android.app.ProgressDialog;
import java.util.HashMap;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import android.widget.Toast;

public class LibraryActivity extends Activity
    implements LoaderManager.LoaderCallbacks<DirectoryListing> {
    public final int LOADER_ID = 28899;

    private ProgressDialog mDialog;
    private ArrayList<String> items;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        items = new ArrayList<String>();
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }
    @Override
    public Loader<DirectoryListing> onCreateLoader(int id, Bundle args) {
        mDialog = new ProgressDialog(this);
        mDialog.setMessage("Loading...");
        mDialog.setCancelable(false);
        mDialog.show();
        return new ListLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<DirectoryListing> loader,
                               DirectoryListing data) {
        mDialog.dismiss();
        for (String f : data.files) items.add(f);
        for (String d : data.directories) items.add(d);
        ArrayAdapter<String> itemsAdapter =
            new ArrayAdapter<String>(this, R.layout.library_item, items);
        ListView listView = (ListView) findViewById(R.id.library_list);
        listView.setAdapter(itemsAdapter);
    }

    @Override
    public void onLoaderReset(Loader<DirectoryListing> loader) {

    }
}

class ListLoader extends AsyncTaskLoader<DirectoryListing> {
    public ListLoader(Context context) {
        super(context);
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();

        forceLoad();
    }

    @Override
    public DirectoryListing loadInBackground() {
        return loadData();
    }

    private DirectoryListing loadData() {
        DirectoryListing files = new DirectoryListing(".");
        files.fetch();
        return files;
    }
}

class DirectoryListing implements Callback {
    public ArrayList<String> files;
    public ArrayList<String> directories;
    public String parent;
    private volatile boolean done;

    public DirectoryListing(String parent) {
        this.parent = parent;
        this.files = new ArrayList<String>();
        this.directories = new ArrayList<String>();
    }

    public void changeDirectory(String name) {
        // TODO: proper path handling
        parent = parent + "/" + name;
    }

    /* Fetch data from server */
    public void fetch(boolean ignoreHidden) {
        HashMap<String, Object> cmd = new HashMap<String, Object>();
        cmd.put("command", "list");
        cmd.put("directory", this.parent);
        this.done = false;
        // Send command
        try {
            new UDPPacket(Settings.ipAddress,
                          Settings.port,
                          Settings.passwd, this).execute(cmd);
        } catch (Exception e) {
            this.callback(false, null);
        }
        // TODO: timeout
        while (!done) SystemClock.sleep(100);

        if (ignoreHidden) {
            /* Remove files and directories starting with '.' */
            for (int i = files.size()-1; i >= 0; i--) {
                if (files.get(i).charAt(0) == '.')
                    files.remove(i);
            }
            for (int i = directories.size()-1; i >= 0; i--) {
                if (directories.get(i).charAt(0) == '.')
                    directories.remove(i);
            }
        }
    }
    public void fetch() {
        fetch(true);
    }

    @Override
    public void callback(boolean result, JSONObject obj) {
        boolean success = false;
        try {success = obj.getBoolean("result");} catch (Exception e) {}
        if (result && success) {
            try {
                obj = new JSONObject(obj.getString("message"));
                JSONArray jfiles = obj.getJSONArray("files");
                JSONArray jdirs = obj.getJSONArray("directories");

                for (int i = 0; i < jfiles.length(); i++)
                    files.add(jfiles.getString(i));
                for (int i = 0; i < jdirs.length(); i++)
                    directories.add(jdirs.getString(i));
            }
            catch (JSONException e) {};
        }
        this.done = true;
    }
}
