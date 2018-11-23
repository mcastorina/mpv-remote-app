package miccah.mpvremote;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.ListView;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Loader;
import android.content.Context;
import android.app.ProgressDialog;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.view.View;

public class LibraryActivity extends Activity
    implements LoaderManager.LoaderCallbacks<DirectoryListing> {

    private ProgressDialog mDialog;
    private ArrayList<LibraryItem> items;
    private LibraryAdapter itemsAdapter;
    private DirectoryListing data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        data = new DirectoryListing(".");

        items = new ArrayList<LibraryItem>();
        itemsAdapter = new LibraryAdapter(this, items);
        ListView listView = (ListView) findViewById(R.id.library_list);
        listView.setAdapter(itemsAdapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                LibraryItem item = items.get(position);

                if (item.type == LibraryItem.FileType.DIRECTORY) {
                    data.changeDirectory(item.name);
                    getLoaderManager().initLoader(getID(data.parent),
                            null, LibraryActivity.this);
                }
                else if (item.type == LibraryItem.FileType.FILE) {
                    Toast.makeText(getApplicationContext(),
                            "Playing " + item.name,
                            Toast.LENGTH_SHORT).show();
                    /* Construct "play" command */
                    HashMap<String, Object> cmd = new HashMap<String, Object>();
                    cmd.put("command", "play");
                    cmd.put("path", data.parent + "/" + item.name);
                    new UDPPacket(Settings.ipAddress,
                            Settings.port,
                            Settings.passwd, null).execute(cmd);
                    finish();
                }
            }
        });

        getLoaderManager().initLoader(getID(data.parent), null, this);
    }
    @Override
    public Loader<DirectoryListing> onCreateLoader(int id, Bundle args) {
        mDialog = new ProgressDialog(this);
        mDialog.setMessage("Loading...");
        mDialog.setCancelable(false);
        mDialog.show();

        return new ListLoader(this, data);
    }

    @Override
    public void onLoadFinished(Loader<DirectoryListing> loader,
                               DirectoryListing data) {
        /* Sort our data */
        this.data = data;
        Collections.sort(this.data.files);
        Collections.sort(this.data.directories);

        mDialog.dismiss();

        /* Clear items */
        items.clear();
        itemsAdapter.clear();

        /* Add "..", directories, and files */
        if (!data.parent.equals(".")) {
            itemsAdapter.add(new LibraryItem("..",
                        LibraryItem.FileType.DIRECTORY));
        }
        for (String d : this.data.directories)
            itemsAdapter.add(new LibraryItem(d,
                        LibraryItem.FileType.DIRECTORY));
        for (String f : this.data.files)
            itemsAdapter.add(new LibraryItem(f, LibraryItem.FileType.FILE));
    }

    @Override
    public void onLoaderReset(Loader<DirectoryListing> loader) {
    }

    /* Return a unique value given a path */
    static int count = 0;
    private int getID(String path) {
        // FIXME: use hash code and get load caching to work
        return count++;
        // return path.hashCode();
    }
}

class ListLoader extends AsyncTaskLoader<DirectoryListing> {
    private DirectoryListing files;

    public ListLoader(Context context, DirectoryListing files) {
        super(context);
        this.files = files;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();

        forceLoad();
    }

    @Override
    public DirectoryListing loadInBackground() {
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
        if (name.equals("..")) {
            if (!parent.equals("."))
                parent = parent.substring(0, parent.lastIndexOf("/"));
        }
        else {
            parent = parent + "/" + name;
        }

        // TODO: caching
        this.files.clear();
        this.directories.clear();
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
