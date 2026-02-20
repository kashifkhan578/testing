package com.remote.pro;

import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {
    private TextView tvStatus, tvIp;
    private MediaProjectionManager mpm;
    private String myMobileIp = "Not Found";

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

        // Naya Advanced IP Scanner (Hotspot aur Wi-Fi dono ke liye)
        myMobileIp = getLocalIpAddress();
        if (myMobileIp != null) {
            tvIp.setText("IP: " + myMobileIp);
        } else {
            tvIp.setText("IP: Error (Check Wi-Fi/Hotspot)");
        }

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

    // Ye function Hotspot aur Wi-Fi dono ka IP nikalne mein expert hai
    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
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
                    Toast.makeText(this, "Screen Share Ready!", Toast.LENGTH_SHORT).show();
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
