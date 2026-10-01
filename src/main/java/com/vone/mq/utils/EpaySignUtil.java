package com.vone.mq.utils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 标准易支付 MD5 签名工具
 * <p>
 * 统一实现易支付规范的签名 / 验签逻辑：剔除 sign、sign_type 及空值参数，
 * 按参数名 ASCII 字典序排列，以 key=value&amp;key=value 拼接，末尾追加商户密钥后 MD5。
 * 原先散落在 WebService、EpayController、WebController 中的三套签名实现统一收敛到这里。
 * </p>
 */
public final class EpaySignUtil {

    private EpaySignUtil() {
    }

    /**
     * 按易支付规范生成签名（32 位小写十六进制）。
     *
     * @param params 全部请求参数（可包含 sign/sign_type，内部自动剔除）
     * @param key    商户通讯密钥
     * @return 签名串
     */
    public static String sign(Map<String, String> params, String key) {
        return PayUtils.md5(buildSignContent(params) + (key == null ? "" : key));
    }

    /**
     * 验证签名是否正确（大小写不敏感）。
     *
     * @param params 全部请求参数
     * @param sign   待校验的签名
     * @param key    商户通讯密钥
     * @return 是否通过
     */
    public static boolean verify(Map<String, String> params, String sign, String key) {
        if (sign == null || sign.isEmpty() || key == null || key.isEmpty()) {
            return false;
        }
        return sign(params, key).equalsIgnoreCase(sign);
    }

    /**
     * 从 HTTP 请求中提取待签名参数（剔除 sign、sign_type 及空值）。
     *
     * @param request HTTP 请求
     * @return 参数 Map
     */
    public static Map<String, String> paramsFromRequest(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> names = request.getParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if ("sign".equals(name) || "sign_type".equals(name)) {
                continue;
            }
            String value = request.getParameter(name);
            if (value != null && !value.isEmpty()) {
                params.put(name, value);
            }
        }
        return params;
    }

    /**
     * 构建带签名的请求查询串：参数按字典序 URL 编码拼接，末尾追加 sign。
     *
     * @param params 通知参数
     * @param key    商户通讯密钥
     * @return name1=value1&amp;name2=value2&amp;sign=xxx 形式的查询串
     */
    public static String buildSignedQuery(Map<String, String> params, String key) {
        String sign = sign(params, key);

        Map<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                sorted.put(entry.getKey(), entry.getValue());
            }
        }

        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (query.length() > 0) {
                query.append("&");
            }
            query.append(PayUtils.encode(entry.getKey()))
                    .append("=")
                    .append(PayUtils.encode(entry.getValue()));
        }
        query.append("&sign=").append(sign);
        return query.toString();
    }

    /**
     * 生成待签名原文：有效参数按字典序以 key=value&amp;key=value 拼接（不含密钥）。
     */
    private static String buildSignContent(Map<String, String> params) {
        List<String> keys = new ArrayList<>();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String name = entry.getKey();
                String value = entry.getValue();
                if ("sign".equals(name) || "sign_type".equals(name)
                        || value == null || value.isEmpty()) {
                    continue;
                }
                keys.add(name);
            }
        }
        keys.sort(String::compareTo);

        StringBuilder content = new StringBuilder();
        for (String name : keys) {
            if (content.length() > 0) {
                content.append("&");
            }
            content.append(name).append("=").append(params.get(name));
        }
        return content.toString();
    }
}
