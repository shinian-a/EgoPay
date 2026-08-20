package com.vone.mq.controller;

import com.vone.mq.dao.SettingDao;
import com.vone.mq.dto.CommonRes;
import com.vone.mq.dto.CreateOrderRes;
import com.vone.mq.entity.Setting;
import com.vone.mq.service.WebService;
import com.vone.mq.utils.ResUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * 易支付接口控制器
 * 兼容标准易支付V1版本接口，包括submit.php、mapi.php和query.php
 */
@RestController
public class EpayController {

    @Autowired
    private WebService webService;

    @Autowired
    private SettingDao settingDao;

    /**
     * 易支付页面跳转支付接口 (submit.php)
     * 此接口可用于用户前台直接发起支付，使用form表单跳转或拼接成url跳转
     *
     * @param request  HTTP请求对象
     * @param response HTTP响应对象
     * @throws IOException IO异常
     */
    @RequestMapping("/submit.php")
    public void submit(HttpServletRequest request, HttpServletResponse response) throws IOException {
        processSubmit(request, response, true);
    }

    /**
     * 易支付API接口支付 (mapi.php)
     * 此接口可用于服务器后端发起支付请求，会返回支付二维码链接或支付跳转url
     *
     * @param request  HTTP请求对象
     * @param response HTTP响应对象
     * @throws IOException IO异常
     */
    @RequestMapping("/mapi.php")
    public void mapi(HttpServletRequest request, HttpServletResponse response) throws IOException {
        processSubmit(request, response, false);
    }

    /**
     * 易支付订单查询接口 (query.php)
     *
     * @param request  HTTP请求对象
     * @param response HTTP响应对象
     * @throws IOException IO异常
     */
    @RequestMapping("/query.php")
    public void query(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        PrintWriter out = response.getWriter();

        // 获取请求参数
        String pid = request.getParameter("pid"); // 商户ID
        String outTradeNo = request.getParameter("out_trade_no"); // 商户订单号
        String sign = request.getParameter("sign"); // 签名
        String signType = request.getParameter("sign_type"); // 签名类型

        try {
            // 验证商户ID
            String systemPid = getSettingValue("pid");
            if (systemPid == null || !systemPid.equals(pid)) {
                out.write("{\"code\":-1,\"msg\":\"商户ID不正确\"}");
                return;
            }

            // 验证签名
            if (!verifySign(request, sign, signType)) {
                out.write("{\"code\":-1,\"msg\":\"签名验证失败\"}");
                return;
            }

            // 调用原有订单查询接口
            CommonRes commonRes = webService.getOrderByPayId(outTradeNo);

            // 处理结果并返回
            if (commonRes.getCode() == 1) {
                // 成功查询订单
                Map<String, Object> result = new HashMap<>();
                result.put("code", 1);
                result.put("msg", "success");
                result.put("data", commonRes.getData());
                out.write(new com.google.gson.Gson().toJson(result));
            } else {
                // 查询订单失败
                Map<String, Object> result = new HashMap<>();
                result.put("code", -1);
                result.put("msg", commonRes.getMsg());
                out.write(new com.google.gson.Gson().toJson(result));
            }
        } catch (Exception e) {
            out.write("{\"code\":-1,\"msg\":\"系统错误\"}");
        }
    }

    /**
     * 处理submit和mapi请求的通用方法
     *
     * @param request    HTTP请求对象
     * @param response   HTTP响应对象
     * @param isRedirect 是否为页面跳转模式
     * @throws IOException IO异常
     */
    private void processSubmit(HttpServletRequest request, HttpServletResponse response, boolean isRedirect) throws IOException {
        response.setContentType("text/html;charset=utf-8");
        PrintWriter out = response.getWriter();

        try {
            // 获取请求参数
            String pid = request.getParameter("pid"); // 商户ID
            String type = request.getParameter("type"); // 支付方式
            String outTradeNo = request.getParameter("out_trade_no"); // 商户订单号
            String notifyUrl = request.getParameter("notify_url"); // 异步通知地址
            String returnUrl = request.getParameter("return_url"); // 同步跳转地址
            String name = request.getParameter("name"); // 商品名称
            String money = request.getParameter("money"); // 订单金额
            String sitename = request.getParameter("sitename"); // 网站名称
            String param = request.getParameter("param"); // 自定义参数
            String sign = request.getParameter("sign"); // 签名
            String signType = request.getParameter("sign_type"); // 签名类型

            // 验证商户ID
            String systemPid = getSettingValue("pid");
            if (systemPid == null || !systemPid.equals(pid)) {
                if (isRedirect) {
                    out.write("商户ID不正确");
                } else {
                    out.write("{\"code\":-1,\"msg\":\"商户ID不正确\"}");
                }
                return;
            }

            // 验证签名
            if (!verifySign(request, sign, signType)) {
                if (isRedirect) {
                    out.write("签名验证失败");
                } else {
                    out.write("{\"code\":-1,\"msg\":\"签名验证失败\"}");
                }
                return;
            }

            // 转换支付方式
            Integer payType = convertPayType(type);
            if (payType == null) {
                if (isRedirect) {
                    out.write("支付方式不支持");
                } else {
                    out.write("{\"code\":-1,\"msg\":\"支付方式不支持\"}");
                }
                return;
            }

            // 调用原有创建订单接口
            // 注意：这里的sign是易支付传来的签名，我们需要生成我们系统自己的签名
            String systemKey = getSettingValue("key");
            String systemSign = generateSystemSign(outTradeNo, param != null ? param : "", payType, money, systemKey);

            CommonRes commonRes = webService.createOrder(outTradeNo, param != null ? param : "", payType, money,
                    notifyUrl, returnUrl, systemSign, name, true);

            if (isRedirect) {
                // 页面跳转模式
                if (commonRes.getCode() == 1) {
                    // 成功创建订单，跳转到支付页面
                    CreateOrderRes data = (CreateOrderRes) commonRes.getData();
                    String orderId = data.getOrderId();
                    response.sendRedirect("/payPage/pay.html?orderId=" + orderId);
                } else {
                    // 创建订单失败
                    out.write(commonRes.getMsg());
                }
            } else {
                // API接口模式
                if (commonRes.getCode() == 1) {
                    // 成功创建订单
                    CreateOrderRes data = (CreateOrderRes) commonRes.getData();
                    String payUrl = data.getPayUrl();
                    String orderId = data.getOrderId();

                    Map<String, Object> result = new HashMap<>();
                    result.put("code", 1);
                    result.put("msg", "success");
                    result.put("out_trade_no", outTradeNo);
                    result.put("orderId", orderId);
                    String requestUrl = request.getRequestURL().toString();
                    String baseUrl = requestUrl.substring(0, requestUrl.length() - "/mapi.php".length());
                    result.put("qrcode", baseUrl + "/enQrcode?url="
                            + java.net.URLEncoder.encode(payUrl, java.nio.charset.StandardCharsets.UTF_8));
                    result.put("url", baseUrl + "/payPage/pay.html?orderId="
                            + java.net.URLEncoder.encode(orderId, java.nio.charset.StandardCharsets.UTF_8));

                    out.write(new com.google.gson.Gson().toJson(result));
                } else {
                    // 创建订单失败
                    Map<String, Object> result = new HashMap<>();
                    result.put("code", -1);
                    result.put("msg", commonRes.getMsg());
                    out.write(new com.google.gson.Gson().toJson(result));
                }
            }
        } catch (Exception e) {
            if (isRedirect) {
                out.write("系统错误");
            } else {
                out.write("{\"code\":-1,\"msg\":\"系统错误\"}");
            }
        }
    }

    /**
     * 验证签名
     *
     * @param request  HTTP请求对象
     * @param sign     签名
     * @param signType 签名类型
     * @return 是否验证通过
     */
    private boolean verifySign(HttpServletRequest request, String sign, String signType) {
        try {
            // 获取所有参数
            Map<String, String> params = new HashMap<>();
            Enumeration<String> paramNames = request.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                // 排除sign和sign_type参数
                if (!"sign".equals(paramName) && !"sign_type".equals(paramName)) {
                    String paramValue = request.getParameter(paramName);
                    if (paramValue != null && !paramValue.isEmpty()) {
                        params.put(paramName, paramValue);
                    }
                }
            }

            // 生成待签名字符串
            String signStr = generateSignString(params);

            // 获取系统配置的商户密钥
            String key = getSettingValue("key");
            if (key == null || key.isEmpty()) {
                return false;
            }

            // 根据签名类型进行验证
            if ("MD5".equalsIgnoreCase(signType)) {
                // MD5签名验证
                String signContent = signStr + key;
                String calculatedSign = DigestUtils.md5DigestAsHex(signContent.getBytes()).toUpperCase();
                return calculatedSign.equals(sign.toUpperCase());
            }

            // 默认使用MD5验证
            String signContent = signStr + key;
            String calculatedSign = DigestUtils.md5DigestAsHex(signContent.getBytes()).toUpperCase();
            return calculatedSign.equals(sign.toUpperCase());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 生成待签名字符串
     * 按照参数名ASCII码递增排序（字典序），使用URL键值对的格式（即key1=value1&key2=value2…）拼接成字符串
     *
     * @param params 参数Map
     * @return 待签名字符串
     */
    private String generateSignString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        // 按照参数名ASCII码递增排序
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        // 拼接参数
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = params.get(key);
            if (i == 0) {
                sb.append(key).append("=").append(value);
            } else {
                sb.append("&").append(key).append("=").append(value);
            }
        }

        return sb.toString();
    }

    /**
     * 生成系统签名
     *
     * @param payId 商户订单号
     * @param param 自定义参数
     * @param type  支付类型
     * @param price 订单金额
     * @param key   系统密钥
     * @return 系统签名
     */
    private String generateSystemSign(String payId, String param, Integer type, String price, String key) {
        String signContent = payId + param + type + price + key;
        return DigestUtils.md5DigestAsHex(signContent.getBytes());
    }

    /**
     * 转换支付方式
     *
     * @param type 易支付的支付方式
     * @return 系统的支付方式
     */
    private Integer convertPayType(String type) {
        if (type == null || type.isEmpty()) {
            return null; // 不传支付方式，跳转到收银台
        }
        if ("wxpay".equals(type)) {
            return 1; // 微信支付
        } else if ("alipay".equals(type)) {
            return 2; // 支付宝支付
        }
        return null; // 不支持的支付方式
    }

    /**
     * 获取系统配置值
     *
     * @param key 配置键
     * @return 配置值
     */
    private String getSettingValue(String key) {
        try {
            Setting setting = settingDao.findById(key).orElse(null);
            return setting != null ? setting.getVvalue() : null;
        } catch (Exception e) {
            return null;
        }
    }
}