package miccah.mpvremote;

import java.util.ArrayList;
import android.content.Context;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.ArrayAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;

public class LibraryAdapter extends ArrayAdapter<LibraryItem> {

    public LibraryAdapter(Context context, ArrayList<LibraryItem> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
       // Get the data item for this position
       LibraryItem item = getItem(position);
       // Check if an existing view is being reused, otherwise inflate the view
       if (convertView == null) {
          convertView = LayoutInflater.from(getContext()).inflate(
                  R.layout.library_item, parent, false);
       }
       // Lookup view for data population
       TextView text = (TextView) convertView.findViewById(R.id.library_item_text);
       ImageView image = (ImageView) convertView.findViewById(R.id.library_item_image);
       // Populate the data into the template view using the data object
       text.setText(item.name);
       image.setImageResource(item.drawable);
       // Return the completed view to render on screen
       return convertView;
   }
}

class LibraryItem {
    public String name;
    public FileType type;
    public int drawable;

    public static final int directoryDrawable = R.drawable.folder;
    public static final int fileDrawable = R.drawable.file;

    public enum FileType {DIRECTORY, FILE};

    public LibraryItem(String name, FileType type) {
        this.name = name;
        this.type = type;

        /* Assign drawable */
        if (this.type == FileType.DIRECTORY)
            this.drawable = directoryDrawable;
        else if (this.type == FileType.FILE)
            this.drawable = fileDrawable;
    }
}
