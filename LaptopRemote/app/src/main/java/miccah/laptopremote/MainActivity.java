package miccah.laptopremote;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Toast;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
