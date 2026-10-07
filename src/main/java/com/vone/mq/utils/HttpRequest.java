package com.vone.mq.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
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
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        boolean completed = false;
        try {
            String urlNameString = appendQuery(url, param);
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            connection = (HttpURLConnection) realUrl.openConnection();
            connection.setRequestMethod("GET");
            // 设置通用的请求属性
            setCommonHeaders(connection);
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setInstanceFollowRedirects(true);
            // 建立实际的连接
            connection.connect();
            // 必须先拿到输入流，响应头（含字符集）才保证已解析
            inputStream = connection.getInputStream();
            Charset charset = resolveCharset(connection);
            String result = readResponse(inputStream, charset);
            completed = true;
            return result;
        } catch (Exception e) {
            log.warn("发送GET请求失败, url={}, msg={}", url, e.getMessage());
            return null;
        } finally {
            closeQuietly(inputStream);
            // 请求失败时断开连接，避免残留异常连接；成功时读完响应即自动归还连接池
            if (!completed && connection != null) {
                connection.disconnect();
            }
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
        HttpURLConnection conn = null;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        boolean completed = false;
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            conn = (HttpURLConnection) realUrl.openConnection();
            conn.setRequestMethod("POST");
            // 设置通用的请求属性
            setCommonHeaders(conn);
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setInstanceFollowRedirects(true);
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 关键：表单提交必须声明 Content-Type，否则对端（如 PHP 的 $_POST）不会解析请求体，
            // 发卡网只接收 POST 时会因此收不到参数，表现为“通知地址无响应”。
            byte[] body = (param == null ? "" : param).getBytes(StandardCharsets.UTF_8);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            // 直接以 UTF-8 写字节，避免 PrintWriter 使用系统默认字符集（中文 Windows 下为 GBK）
            outputStream = conn.getOutputStream();
            outputStream.write(body);
            outputStream.flush();
            // 必须先拿到输入流，响应头（含字符集）才保证已解析
            inputStream = conn.getInputStream();
            Charset charset = resolveCharset(conn);
            String result = readResponse(inputStream, charset);
            completed = true;
            return result;
        } catch (Exception e) {
            log.warn("发送POST请求失败, url={}, msg={}", url, e.getMessage());
            return null;
        } finally {
            closeQuietly(outputStream);
            closeQuietly(inputStream);
            if (!completed && conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 异步通知专用提交：优先使用 GET；当 GET 无响应（返回 null）或对端未返回
     * “success” 时，自动改用 POST（application/x-www-form-urlencoded）重新提交
     * 同一套参数。易支付订单与微免签订单均适用。
     *
     * @param url
     *            异步通知地址
     * @param param
     *            通知参数，name1=value1&name2=value2 的形式（值已做 URL 编码）
     * @return 最终一次请求的响应内容；两种方式都失败时返回最后一次响应（可能为 null）
     */
    public static String sendNotify(String url, String param) {
        String response = sendGet(url, param);
        if (isNotifySuccess(response)) {
            return response;
        }
        log.info("GET方式异步通知未成功（无响应或返回非success），改用POST重新提交, url={}, getResponse={}",
                url, response);
        return sendPost(url, param);
    }

    /**
     * 异步通知成功的判定：对端按约定返回去除空白后等于 success（大小写不敏感）。
     */
    private static boolean isNotifySuccess(String response) {
        return response != null && "success".equalsIgnoreCase(response.trim());
    }

    private static void setCommonHeaders(HttpURLConnection connection) {
        connection.setRequestProperty("accept", "*/*");
        connection.setRequestProperty("connection", "Keep-Alive");
        connection.setRequestProperty("user-agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36");
    }

    /**
     * 将查询串追加到 URL，自动判断 URL 上是否已有查询参数。
     */
    private static String appendQuery(String url, String param) {
        if (param == null || param.isEmpty()) {
            return url;
        }
        return url + (url.contains("?") ? "&" : "?") + param;
    }

    /**
     * 读取完整响应内容。
     */
    private static String readResponse(InputStream inputStream, Charset charset) throws IOException {
        try (Reader reader = new InputStreamReader(inputStream, charset)) {
            StringBuilder result = new StringBuilder();
            char[] buffer = new char[4096];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                result.append(buffer, 0, len);
            }
            return result.toString();
        }
    }

    /**
     * 从响应头 Content-Type 中解析字符集，缺省使用 UTF-8。
     */
    private static Charset resolveCharset(HttpURLConnection connection) {
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

    private static void closeQuietly(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                log.debug("关闭响应流异常: {}", e.getMessage());
            }
        }
    }

    private static void closeQuietly(OutputStream out) {
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                log.debug("关闭输出流异常: {}", e.getMessage());
            }
        }
    }
}
