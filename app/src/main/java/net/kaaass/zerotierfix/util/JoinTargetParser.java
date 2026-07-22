package net.kaaass.zerotierfix.util;

import android.net.Uri;
import android.text.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析「加入网络」目标，兼容以下形式（与官网 ZeroTier One 1.16.0 行为对齐）：
 * <ul>
 *     <li>官网分享链接 {@code https://joinzt.com/addnetwork?v=1&nwid=<id>}</li>
 *     <li>自定义深链 {@code zerotier://addnetwork?nwid=<id>}</li>
 *     <li>官网邀请页 {@code https://join.zerotier.com/<id>}、{@code https://my.zerotier.com/network/<id>}</li>
 *     <li>直接给出的 16 位十六进制 Network ID（如 {@code a1b2c3d4e5f60718}）</li>
 *     <li>扫码 / 粘贴得到的任意包含 Network ID 的文本</li>
 * </ul>
 */
public final class JoinTargetParser {

    /**
     * ZeroTier Network ID 固定为 16 位十六进制字符。
     */
    private static final Pattern NETWORK_ID_PATTERN = Pattern.compile("[0-9a-fA-F]{16}");

    /**
     * 官网 App Link 域名（与 ZeroTier One 官方一致）。
     */
    private static final String APP_LINK_HOST = "joinzt.com";

    private JoinTargetParser() {
    }

    /**
     * 判断给定的 16 位字符串是否为合法的 Network ID（能作为无符号 16 进制长整型解析）。
     */
    public static boolean isValidNetworkId(String id) {
        if (TextUtils.isEmpty(id) || id.length() != 16) {
            return false;
        }
        try {
            Long.parseUnsignedLong(id, 16);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 校验是否为合法的「加入网络」App Link（与官网 {@code UriUtils.isValidAppLinkUri} 对齐）。
     */
    public static boolean isValidAppLinkUri(Uri uri) {
        if (uri == null) {
            return false;
        }
        String scheme = uri.getScheme();
        if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)
                && !"zerotier".equalsIgnoreCase(scheme)) {
            return false;
        }
        String host = uri.getHost();
        if (!APP_LINK_HOST.equalsIgnoreCase(host)
                && !"join.zerotier.com".equalsIgnoreCase(host)
                && !"my.zerotier.com".equalsIgnoreCase(host)) {
            return false;
        }
        String path = uri.getPath();
        if ("/addnetwork".equals(path) || (path != null && path.endsWith("/addnetwork"))) {
            return isValidNetworkId(uri.getQueryParameter("nwid"));
        }
        return false;
    }

    /**
     * 从原始输入中解析出 Network ID。
     *
     * @param raw 用户输入 / 扫码结果 / 深链 URI
     * @return 解析到的 16 位小写 Network ID；无法解析时返回 {@code null}
     */
    public static String parse(String raw) {
        if (TextUtils.isEmpty(raw)) {
            return null;
        }
        // 1) 尝试作为链接解析（官网二维码 / 深链均为链接形式）
        Uri uri = null;
        try {
            uri = Uri.parse(raw);
        } catch (Exception ignored) {
            // 忽略，走纯文本回退
        }
        if (uri != null && !TextUtils.isEmpty(uri.getScheme())) {
            // 1a) 标准 App Link：提取 nwid 参数
            if (isValidAppLinkUri(uri)) {
                String nwid = uri.getQueryParameter("nwid");
                if (isValidNetworkId(nwid)) {
                    return nwid.toLowerCase();
                }
            }
            // 1b) 官网邀请页路径末尾的 16 位 ID（join.zerotier.com/<id>、my.zerotier.com/network/<id>）
            String last = uri.getLastPathSegment();
            if (isValidNetworkId(last)) {
                return last.toLowerCase();
            }
        }
        // 2) 回退：从文本中取首个 16 位连续十六进制串（兼容裸 ID 与任意包含 ID 的文本/链接）
        Matcher matcher = NETWORK_ID_PATTERN.matcher(raw);
        if (matcher.find()) {
            return matcher.group().toLowerCase();
        }
        return null;
    }
}
