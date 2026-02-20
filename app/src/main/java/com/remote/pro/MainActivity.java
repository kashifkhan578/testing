package com.remote.pro;

import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.Formatter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {
    private TextView tvStatus, tvIp;
    private MediaProjectionManager mpm;
    private String myMobileIp = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        tvIp = findViewById(R.id.tv_ip);
        Button btnAcc = findViewById(R.id.btn_acc);
        Button btnStream = findViewById(R.id.btn_stream);
        Button btnScan = findViewById(R.id.btn_scan);

        mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            myMobileIp = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
            tvIp.setText("IP: " + myMobileIp);
        } catch (Exception e) {}

        btnAcc.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        btnStream.setOnClickListener(v -> {
            Intent captureIntent = mpm.createScreenCaptureIntent();
            screenLauncher.launch(captureIntent);
        });

        btnScan.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Scan PC QR Code");
            options.setBeepEnabled(true);
            options.setOrientationLocked(false);
            barcodeLauncher.launch(options);
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
                    Toast.makeText(this, "Screen Share Started!", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
        new ScanContract(), result -> {
            if (result.getContents() != null) {
                String data = result.getContents();
                if (data.startsWith("REMOTE_PAIR:")) {
                    String pcIp = data.split(":")[1];
                    tvStatus.setText("Connecting to PC...");
                    
                    // Asal Connection Code: PC ko hamara IP batana
                    new Thread(() -> {
                        try {
                            Socket s = new Socket(pcIp, 8888);
                            s.getOutputStream().write(("CONNECT:" + myMobileIp).getBytes());
                            s.close();
                            runOnUiThread(() -> {
                                tvStatus.setText("Connected successfully!");
                                tvStatus.setTextColor(android.graphics.Color.GREEN);
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> {
                                tvStatus.setText("Connection Failed! Is PC app running?");
                                tvStatus.setTextColor(android.graphics.Color.RED);
                            });
                        }
                    }).start();
                }
            }
        }
    );
}
