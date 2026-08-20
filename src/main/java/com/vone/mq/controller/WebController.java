package com.vone.mq.controller;

import com.google.gson.Gson;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.vone.mq.dao.SettingDao;
import com.vone.mq.dto.CommonRes;
import com.vone.mq.dto.CreateOrderRes;
import com.vone.mq.entity.PayOrder;
import com.vone.mq.dao.PayOrderDao;
import com.vone.mq.service.WebService;
import com.vone.mq.utils.ResUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
public class WebController {
    private static final String KEY_CONFIG = "key";
    private static final String ERROR_SIGN = "error_sign";
    private static final String ERROR_RESPONSE = "error";
    @Autowired
    private WebService webService;
    @Autowired
    private SettingDao settingDao;
    @Autowired
    private PayOrderDao payOrderDao;
    @RequestMapping("/enQrcode")
    public void enQrcode(HttpServletResponse resp, String url) throws IOException {
        if (url != null && !"".equals(url)) {
            ServletOutputStream stream = null;
            try {
                int width = 200;//图片的宽度
                int height = 200;//高度
                stream = resp.getOutputStream();
                QRCodeWriter writer = new QRCodeWriter();
                Map<EncodeHintType, Object> hints = new HashMap<>();
                hints.put(EncodeHintType.MARGIN, 1);
                BitMatrix m = writer.encode(url, BarcodeFormat.QR_CODE, width, height, hints);
                resp.setContentType("image/png");
                MatrixToImageWriter.writeToStream(m, "png", stream);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (stream != null) {
                    stream.flush();
                    stream.close();
                }
            }
        }
    }

    @RequestMapping("/deQrcode")
    public CommonRes deQrcode(String base64) {
        if (base64 != null && !"".equals(base64)) {
            try {
                MultiFormatReader multiFormatReader = new MultiFormatReader();
                byte[] bytes1 = Base64.getDecoder().decode(base64);
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes1);
                BufferedImage image = ImageIO.read(bais);
                //定义二维码参数
                Map hints = new HashMap();
                hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
                //获取读取二维码结果
                BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
                Result result = multiFormatReader.decode(binaryBitmap, hints);
                return ResUtil.success(result.getText());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ResUtil.error();
    }

    @RequestMapping("/deQrcode2")
    public CommonRes deQrcode2(@RequestParam("file") MultipartFile file) {
        if (file != null) {
            try {
                MultiFormatReader multiFormatReader = new MultiFormatReader();
                byte[] bytes1 = file.getBytes();
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes1);
                BufferedImage image = ImageIO.read(bais);
                //定义二维码参数
                Map hints = new HashMap();
                hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
                //获取读取二维码结果
                BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
                Result result = multiFormatReader.decode(binaryBitmap, hints);

                return ResUtil.success(result.getText());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ResUtil.error();
    }

    /**
     * 支付结果回调接口，兼容原接口和易支付接口的异步/同步回调。
     *
     * @param payId 商户订单号
     * @param param 创建订单时传入的参数
     * @param type 支付方式：微信支付为1 支付宝支付为2
     * @param price 订单金额
     * @param reallyPrice 实际支付金额
     * @param sign 校验签名
     * @return
     */
    @RequestMapping({"/returnNotify", "/pay/notify", "/pay/return"})
    public String returnNotify(
            String payId, String param, String type, String price, String reallyPrice, String sign,
            String out_trade_no, String trade_no, String name, String money, String trade_status,
            String sign_type, String pid, HttpServletRequest request) {
        try {
            String key = settingDao.findById(KEY_CONFIG).map(setting -> setting.getVvalue()).orElse("");
            if (key.isEmpty()) {
                return ERROR_RESPONSE;
            }

            if (out_trade_no != null && !out_trade_no.isEmpty()) {
                if (!settingDao.findById("pid").map(setting -> setting.getVvalue())
                        .map(configuredPid -> configuredPid.equals(pid)).orElse(false)
                        || !"TRADE_SUCCESS".equalsIgnoreCase(trade_status)
                        || !verifyEpayCallbackSign(request, sign, key)) {
                    return ERROR_RESPONSE;
                }

                PayOrder order = payOrderDao.findByPayId(out_trade_no);
                if (order == null || !decimalEquals(money, order.getPrice())
                        || !typeMatches(type, order.getType())) {
                    return ERROR_RESPONSE;
                }
            } else {
                PayOrder order = payOrderDao.findByPayId(payId);
                String expectedSign = WebService.md5(nullToEmpty(payId) + nullToEmpty(param)
                        + nullToEmpty(type) + nullToEmpty(price) + nullToEmpty(reallyPrice) + key);
                if (order == null || payId == null || !payId.equals(order.getPayId())
                        || !decimalEquals(price, order.getPrice())
                        || !decimalEquals(reallyPrice, order.getReallyPrice())
                        || !typeMatches(type, order.getType())
                        || sign == null || !expectedSign.equalsIgnoreCase(sign)) {
                    return ERROR_SIGN;
                }
            }

            return "success";
        } catch (Exception e) {
            e.printStackTrace();
            return ERROR_RESPONSE;
        }
    }

    private boolean decimalEquals(String value, double expected) {
        try {
            return new BigDecimal(value).compareTo(BigDecimal.valueOf(expected)) == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean typeMatches(String value, int expectedType) {
        return String.valueOf(expectedType).equals(value)
                || (expectedType == 1 && "wxpay".equalsIgnoreCase(value))
                || (expectedType == 2 && "alipay".equalsIgnoreCase(value));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean verifyEpayCallbackSign(HttpServletRequest request, String sign, String key) {
        if (sign == null || sign.isEmpty() || key == null || key.isEmpty()) {
            return false;
        }

        Map<String, String> params = new HashMap<>();
        java.util.Enumeration<String> names = request.getParameterNames();
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

        java.util.List<String> namesInOrder = new java.util.ArrayList<>(params.keySet());
        java.util.Collections.sort(namesInOrder);
        StringBuilder content = new StringBuilder();
        for (String name : namesInOrder) {
            if (content.length() > 0) {
                content.append("&");
            }
            content.append(name).append("=").append(params.get(name));
        }

        return WebService.md5(content + key).equalsIgnoreCase(sign);
    }

    /**
     * 创建订单
     *
     * @param payId     商户订单号
     * @param param     订单保存的信息
     * @param type      支付方式 1|微信 2|支付宝
     * @param price     订单价格
     * @param notifyUrl 异步通知地址，如果为空则使用系统后台设置的地址
     * @param returnUrl 支付完成后同步跳转地址，将会携带参数跳转
     * @param sign      签名认证 签名方式为 md5(payId + param + type + price + 通讯密钥)
     * @param isHtml    0返回json数据 1跳转到支付页面
     * @return
     */
    @RequestMapping("/createOrder")
    public String createOrder(String payId, String param, Integer type, String price, String notifyUrl, String returnUrl, String sign, Integer isHtml) {
        if (payId == null || payId.equals("")) {
            return new Gson().toJson(ResUtil.error("请传入商户订单号"));
        }
        if (type == null) {
            return new Gson().toJson(ResUtil.error("请传入支付方式=>1|微信 2|支付宝"));
        }
        if (type != 1 && type != 2) {
            return new Gson().toJson(ResUtil.error("支付方式错误=>1|微信 2|支付宝"));
        }


        Double priceD;
        try {
            priceD = Double.valueOf(price);
        } catch (Exception e){
            return new Gson().toJson(ResUtil.error("请传入订单金额"));
        }

        if (priceD == null || !Double.isFinite(priceD) || priceD <= 0) {
            return new Gson().toJson(ResUtil.error("订单金额必须大于0"));
        }


        if (sign == null || sign.equals("")) {
            return new Gson().toJson(ResUtil.error("请传入签名"));
        }
        if (param == null) {
            param = "";
        }
        if (isHtml == null) {
            isHtml = 0;
        }
        // 原版 V 免签接口统一使用后台配置的回调地址。
        notifyUrl = settingDao.findById("notifyUrl")
                .map(setting -> setting.getVvalue()).orElse("");
        returnUrl = settingDao.findById("returnUrl")
                .map(setting -> setting.getVvalue()).orElse("");
        CommonRes commonRes = webService.createOrder(payId, param, type, price, notifyUrl, returnUrl, sign);
        if (isHtml == 0) {
            String res = new Gson().toJson(commonRes);
            return res;
        } else {
            CreateOrderRes c = (CreateOrderRes) commonRes.getData();
            if (c == null) {
                return commonRes.getMsg();
            } else {
                return "<script>window.location.href = '/payPage/pay.html?orderId=" + c.getOrderId() + "'</script>";
            }
        }
    }

    @RequestMapping("/closeOrder")
    public CommonRes closeOrder(String orderId, String sign) {
        if (orderId == null) {
            return ResUtil.error("请传入云端订单号");
        }
        if (sign == null) {
            return ResUtil.error("请传入签名");
        }
        return webService.closeOrder(orderId, sign);
    }

    @RequestMapping("/appHeart")
    public CommonRes appHeart(String t, String sign) {
        return webService.appHeart(t, sign);
    }

    @RequestMapping("/appPush")
    public CommonRes appPush(Integer type, String price, String t, String sign) {
        return webService.appPush(type, price, t, sign);
    }

    @RequestMapping("/getOrder")
    public CommonRes getOrder(String orderId) {
        if (orderId == null) {
            return ResUtil.error("请传入订单编号");
        }
        return webService.getOrder(orderId);
    }

    @RequestMapping("/checkOrder")
    public CommonRes checkOrder(String orderId) {
        if (orderId == null) {
            return ResUtil.error("请传入订单编号");
        }
        return webService.checkOrder(orderId);

    }

    @RequestMapping("/getState")
    public CommonRes getState(String t, String sign) {
        if (t == null) {
            return ResUtil.error("请传入t");
        }
        if (sign == null) {
            return ResUtil.error("请传入sign");
        }
        return webService.getState(t, sign);
    }
}
