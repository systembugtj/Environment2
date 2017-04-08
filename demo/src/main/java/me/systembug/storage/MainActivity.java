package me.systembug.storage;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.percolate.caffeine.ViewUtils;

import me.systembug.device.Device;
import me.systembug.device.Environment2;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Device[] devices = Environment2.getDevices(null, true, true, false);

        TextView textView = ViewUtils.findViewById(this, R.id.information);

        StringBuilder content = new StringBuilder();

        for (Device device : devices) {
            content.append(device.getMountPoint() + "\n");
        }

        textView.setText(content.toString());
    }
}
