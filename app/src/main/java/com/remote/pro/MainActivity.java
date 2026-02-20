package com.remote.pro;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.Formatter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private MediaProjectionManager mpm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tvIp = findViewById(R.id.tv_ip);
        Button btnAcc = findViewById(R.id.btn_acc);
        Button btnStream = findViewById(R.id.btn_stream);

        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
            tvIp.setText("Mobile IP: " + ip + "\nVideo Port: 8080 | Touch Port: 9999");
        } catch (Exception e) {}

        mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        btnAcc.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        btnStream.setOnClickListener(v -> {
            Intent captureIntent = mpm.createScreenCaptureIntent();
            screenLauncher.launch(captureIntent);
        });
    }

    private final ActivityResultLauncher<Intent> screenLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent serviceIntent = new Intent(this, ScreenStreamService.class);
                    serviceIntent.putExtra("RESULT_CODE", result.getResultCode());
                    serviceIntent.putExtra("DATA", result.getData());
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent);
                    } else {
                        startService(serviceIntent);
                    }
                    Toast.makeText(this, "Screen Shared! Check PC.", Toast.LENGTH_LONG).show();
                }
            }
    );
}
