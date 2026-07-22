package net.kaaass.zerotierfix.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import net.kaaass.zerotierfix.R;
import net.kaaass.zerotierfix.util.JoinTargetParser;

/**
 * 网络列表 fragment 的容器 activity
 */
public class NetworkListActivity extends SingleFragmentActivity {
    @Override
    public Fragment createFragment() {
        return new NetworkListFragment();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // 处理来自扫码 / 网页的「加入网络」深链
        Uri data = getIntent() != null ? getIntent().getData() : null;
        if (data != null) {
            String networkId = JoinTargetParser.parse(data.toString());
            if (networkId != null) {
                Intent intent = new Intent(this, JoinNetworkActivity.class);
                intent.putExtra(JoinNetworkActivity.EXTRA_NETWORK_ID, networkId);
                startActivity(intent);
            } else {
                Toast.makeText(this, R.string.deep_link_invalid, Toast.LENGTH_LONG).show();
            }
        }
    }
}
