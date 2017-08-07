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
        SystemClock.sleep(2000);
        HashMap<String, Object> cmd = new HashMap<String, Object>();
        cmd.put("command", "list");
        cmd.put("directory", this.parent);
        this.done = false;
        // send(cmd, this);
        callback(false, null);
        // TODO: timeout
        while (!done) SystemClock.sleep(100);
        // files.add("file1");
        // files.add("file2");
        // directories.add("dir");
    }
    public void fetch() {
        fetch(true);
    }

    @Override
    public void callback(boolean result, JSONObject obj) {
        if (result == false) {
            /* Failed */
            this.done = true;
            return;
        }
        try {
            files.add(obj.getString("message"));
        }
        catch (JSONException e) {};
        this.done = true;
    }
}
