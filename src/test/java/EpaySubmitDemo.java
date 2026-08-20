import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

public class EpaySubmitDemo {

    // ===== 配置项 =====
    static final String EPAY_URL = "http://127.0.0.1:8080/submit.php";
    static final String PID = "1000";       // 对应 setting 表中的 pid
    static final String KEY = "b765b06283e3a3300c873fb1aead981a";     // 对应 setting 表中的 key

    public static void main(String[] args) throws Exception {

        // ===== 1. 构建请求参数 =====
        Map<String, String> params = new TreeMap<>();
        params.put("pid", PID);
        params.put("type", "wxpay");                        // wxpay / alipay / 空=收银台
        params.put("out_trade_no", "ORDER_" + System.currentTimeMillis());
        params.put("notify_url", "http://127.0.0.1:8080/pay/notify");   // 异步通知地址
        params.put("return_url", "http://127.0.0.1:8080/pay/return");   // 同步跳转地址
        params.put("name", "测试商品");
        params.put("money", "0.01");
        params.put("sitename", "测试站点");
        // params.put("param", "自定义参数");  // 可选

        // ===== 2. 生成MD5签名 =====
        String sign = createSign(params, KEY);
        params.put("sign", sign);
        params.put("sign_type", "MD5");

        // ===== 3. 拼接跳转URL（浏览器打开即可跳转到支付页） =====
        StringBuilder url = new StringBuilder(EPAY_URL).append("?");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            url.append(entry.getKey())
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), "UTF-8"))
                    .append("&");
        }
        url.deleteCharAt(url.length() - 1);

        System.out.println("支付跳转URL：");
        System.out.println(url.toString());
        // 用户在浏览器打开这个URL即可跳转到支付页面
    }

    /**
     * 生成MD5签名（与你的EpayController.verifySign逻辑一致）
     * 规则：参数按key字典序排列 → key1=value1&key2=value2 → 末尾拼接key → MD5 → 大写
     */
    public static String createSign(Map<String, String> params, String key) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            // 排除 sign、sign_type、空值
            if ("sign".equals(k) || "sign_type".equals(k) || v == null || v.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) sb.append("&");
            sb.append(k).append("=").append(v);
        }
        sb.append(key);  // 末尾拼接商户密钥

        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            hex.append(String.format("%02X", b));
        }
        return hex.toString();  // 大写
    }
}