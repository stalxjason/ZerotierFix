package net.kaaass.zerotierfix.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import net.kaaass.zerotierfix.R;
import net.kaaass.zerotierfix.util.JoinTargetParser;

/**
 * 添加网络 fragment 的容器 activity
 */
public class JoinNetworkActivity extends SingleFragmentActivity {

    /**
     * 通过 Intent extra 预填的 Network ID 键名（与 JoinNetworkFragment 共用）。
     */
    public static final String EXTRA_NETWORK_ID = "net.kaaass.zerotierfix.EXTRA_NETWORK_ID";

    private final ActivityResultLauncher<ScanOptions> qrScanLauncher = registerForActivityResult(
            new ScanContract(), result -> {
                if (result.getContents() == null) {
                    // 用户取消扫码
                    return;
                }
                String networkId = JoinTargetParser.parse(result.getContents());
                if (networkId == null) {
                    Toast.makeText(this, R.string.scan_qr_invalid, Toast.LENGTH_LONG).show();
                    return;
                }
                JoinNetworkFragment fragment = (JoinNetworkFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.fragmentContainer);
                if (fragment != null) {
                    fragment.prefill(networkId);
                } else {
                    // 兜底：通过 extra 重新打开并预填
                    Intent intent = new Intent(this, JoinNetworkActivity.class);
                    intent.putExtra(EXTRA_NETWORK_ID, networkId);
                    startActivity(intent);
                }
            });

    @Override
    public Fragment createFragment() {
        return new JoinNetworkFragment();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        // 添加返回按钮
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_join_network, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 返回上一界面
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        // 启动扫码
        if (item.getItemId() == R.id.action_scan_qr) {
            ScanOptions options = new ScanOptions();
            options.setPrompt(getString(R.string.scan_qr_prompt));
            options.setBeepEnabled(false);
            options.setOrientationLocked(false);
            options.setCaptureActivity(CaptureActivity.class);
            qrScanLauncher.launch(options);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
