package com.vone.mq.utils;

import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 支付相关通用工具类
 * <p>
 * 集中提供 MD5 摘要、金额占用键（priceKey）拼装和 URL 编码能力，
 * 避免这些基础方法在多个 Service / 工具类中重复实现。
 * </p>
 */
public final class PayUtils {

    private PayUtils() {
    }

    /**
     * 对文本进行 MD5 摘要，返回 32 位小写十六进制字符串。
     *
     * @param text 原文
     * @return MD5 摘要字符串
     */
    public static String md5(String text) {
        return DigestUtils.md5DigestAsHex((text == null ? "" : text).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 构建金额占用记录的键，格式为：支付方式-去尾零的金额（如 1-10.01）。
     *
     * @param type  支付方式：1 微信 2 支付宝
     * @param price 实际支付金额
     * @return 金额占用键
     */
    public static String priceKey(int type, double price) {
        return type + "-" + BigDecimal.valueOf(price).stripTrailingZeros().toPlainString();
    }

    /**
     * 对查询参数值进行 UTF-8 URL 编码。
     *
     * @param value 原始值
     * @return 编码后的字符串
     */
    public static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
