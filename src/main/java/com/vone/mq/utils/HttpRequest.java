package com.vone.mq.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class HttpRequest {

    private static final Logger log = LoggerFactory.getLogger(HttpRequest.class);

    /** 连接建立超时：10 秒 */
    private static final int CONNECT_TIMEOUT = 10000;
    /** 数据读取超时：15 秒，避免对方无响应时线程无限等待 */
    private static final int READ_TIMEOUT = 15000;

    /**
     * 向指定URL发送GET方法的请求
     *
     * @param url
     *            发送请求的URL
     * @param param
     *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return URL 所代表远程资源的响应结果；请求失败或无响应时返回 null
     */
    public static String sendGet(String url, String param) {
        BufferedReader in = null;
        try {
            String urlNameString = url + "?" + param;
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            // 建立实际的连接
            connection.connect();
            // 定义 BufferedReader输入流来读取URL的响应
            Charset charset = resolveCharset(connection);
            in = new BufferedReader(new InputStreamReader(connection.getInputStream(), charset));
            String line;
            StringBuilder result = new StringBuilder();
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        } catch (Exception e) {
            log.warn("发送GET请求失败, url={}, msg={}", url, e.getMessage());
            return null;
        } finally {
            closeQuietly(in);
        }
    }

    /**
     * 向指定 URL 发送POST方法的请求
     *
     * @param url
     *            发送请求的 URL
     * @param param
     *            请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @return 所代表远程资源的响应结果；请求失败或无响应时返回 null
     */
    public static String sendPost(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(param);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            Charset charset = resolveCharset(conn);
            in = new BufferedReader(new InputStreamReader(conn.getInputStream(), charset));
            String line;
            StringBuilder result = new StringBuilder();
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        } catch (Exception e) {
            log.warn("发送POST请求失败, url={}, msg={}", url, e.getMessage());
            return null;
        } finally {
            if (out != null) {
                out.close();
            }
            closeQuietly(in);
        }
    }

    /**
     * 从响应头 Content-Type 中解析字符集，缺省使用 UTF-8。
     */
    private static Charset resolveCharset(URLConnection connection) {
        String contentType = connection.getContentType();
        if (contentType != null) {
            for (String item : contentType.split(";")) {
                String trimmed = item.trim();
                if (trimmed.toLowerCase().startsWith("charset=")) {
                    try {
                        return Charset.forName(trimmed.substring("charset=".length()));
                    } catch (Exception ignore) {
                        // 无法识别的字符集，回退到默认值
                    }
                }
            }
        }
        return StandardCharsets.UTF_8;
    }

    private static void closeQuietly(BufferedReader in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                log.debug("关闭响应流异常: {}", e.getMessage());
            }
        }
    }
}
