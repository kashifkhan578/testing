package com.remote.pro;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class MainActivity extends AppCompatActivity {
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        Button btnAcc = findViewById(R.id.btn_acc);
        Button btnScan = findViewById(R.id.btn_scan);

        btnAcc.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        btnScan.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Align PC QR inside the box");
            options.setBeepEnabled(true);
            options.setOrientationLocked(false);
            barcodeLauncher.launch(options);
        });
    }

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
        new ScanContract(), result -> {
            if (result.getContents() != null) {
                String data = result.getContents(); // Expecting "REMOTE_PAIR:192.168.x.x"
                if (data.startsWith("REMOTE_PAIR:")) {
                    tvStatus.setText("Connected to PC!");
                    tvStatus.setTextColor(android.graphics.Color.GREEN);
                    Toast.makeText(this, "Link Established!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    );
}
