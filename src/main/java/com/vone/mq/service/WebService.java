package com.vone.mq.service;

import com.vone.mq.dao.PayOrderDao;
import com.vone.mq.dao.PayQrcodeDao;
import com.vone.mq.dao.SettingDao;
import com.vone.mq.dao.TmpPriceDao;
import com.vone.mq.dto.CommonRes;
import com.vone.mq.dto.CreateOrderRes;
import com.vone.mq.entity.PayOrder;
import com.vone.mq.entity.PayQrcode;
import com.vone.mq.entity.Setting;
import com.vone.mq.utils.Arith;
import com.vone.mq.utils.EpaySignUtil;
import com.vone.mq.utils.HttpRequest;
import com.vone.mq.utils.PayUtils;
import com.vone.mq.utils.ResUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 *
 */

@Service
public class WebService {

    private static final Logger log = LoggerFactory.getLogger(WebService.class);

    @Autowired
    private SettingDao settingDao;
    @Autowired
    private PayOrderDao payOrderDao;
    @Autowired
    private TmpPriceDao tmpPriceDao;
    @Autowired
    private PayQrcodeDao payQrcodeDao;

    // 创建订单
    public CommonRes createOrder(String payId, String param, Integer type, String price, String notifyUrl, String returnUrl, String sign){
        return createOrder(payId, param, type, price, notifyUrl, returnUrl, sign, null, false);
    }

    public CommonRes createOrder(String payId, String param, Integer type, String price, String notifyUrl,
                                 String returnUrl, String sign, String epayName, boolean epayOrder){
        if (payId == null || payId.trim().isEmpty() || type == null || (type != 1 && type != 2) || price == null
                || sign == null || sign.trim().isEmpty()) {
            return ResUtil.error("订单参数不完整");
        }
        if (param == null) {
            param = "";
        }
        String key = settingValue("key");
        if (key == null || key.isEmpty()) {
            return ResUtil.error("系统通讯密钥未配置");
        }
        String jsSign =  PayUtils.md5(payId+param+type+price+key);
        if (!sign.equals(jsSign)){
            return ResUtil.error("签名校验不通过");
        }

        final double priceD;
        try {
            priceD = Double.parseDouble(price);
        } catch (NumberFormatException e) {
            return ResUtil.error("订单金额格式错误");
        }
        if (!Double.isFinite(priceD) || priceD <= 0 || priceD > 100000000) {
            return ResUtil.error("订单金额必须大于0");
        }

        PayOrder existingOrder = payOrderDao.findByPayId(payId);
        if (existingOrder != null){
            return ResUtil.error("商户订单号已存在！");
        }


        Date currentTime = new Date();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");

        String orderId = formatter.format(currentTime) + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        int payQf;
        try {
            payQf = Integer.parseInt(settingValue("payQf"));
        } catch (Exception e) {
            return ResUtil.error("价格区分方式未正确配置");
        }
        if (payQf != 1 && payQf != 2) {
            return ResUtil.error("价格区分方式未正确配置");
        }
        //实际支付价格
        double reallyPrice = priceD;

        int row = 0;
        int attempts = 0;
        while (row == 0 && attempts++ < 10000){
            String priceKey = PayUtils.priceKey(type, reallyPrice);

            // 临时占用记录可能因服务中断残留；只有未支付订单才真正占用金额。
            if (payOrderDao.findByReallyPriceAndStateAndType(reallyPrice, 0, type) == null) {
                tmpPriceDao.delprice(priceKey);
            }

            // 先判断占用键是否已存在，避免依赖唯一键冲突异常（异常开销大，还会写 H2 trace）
            if (tmpPriceDao.existsById(priceKey)) {
                // 该金额已被其他未支付订单占用，继续尝试下一金额
                row = 0;
            } else {
                try {
                    row = tmpPriceDao.checkPrice(priceKey);
                    // 某些数据库驱动对 INSERT 的影响行数返回不可靠，确认记录已写入后保留原金额。
                    if (row == 0 && tmpPriceDao.existsById(priceKey)) {
                        row = 1;
                    }
                } catch (Exception e) {
                    // 并发下可能被其他下单抢先占用：已存在则继续换金额，否则按占用失败处理避免无记录裸奔
                    row = 0;
                }
            }

            if (row == 0){
                if (payQf==1){

                    reallyPrice = Arith.add(reallyPrice,0.01);
                }else{
                    reallyPrice = Arith.sub(reallyPrice,0.01);
                }

            }else{
                break;
            }
            if (reallyPrice<=0){
                return ResUtil.error("所有金额均被占用");
            }
        }
        if (row == 0) {
            return ResUtil.error("当前金额区间已无可用金额");
        }

        String payUrl = "";
        if (type == 1){
            payUrl = settingValue("wxpay");
        }else if (type == 2){
            payUrl = settingValue("zfbpay");
        }

        if (payUrl == null || payUrl.isEmpty()){
            tmpPriceDao.delprice(PayUtils.priceKey(type, reallyPrice));
            return ResUtil.error("请您先进入后台配置程序");
        }

        int isAuto = 1;

        PayQrcode payQrcode = payQrcodeDao.findByPriceAndType(reallyPrice,type);
        if (payQrcode!=null){
            payUrl = payQrcode.getPayUrl();
            isAuto = 0;
        }


        PayOrder payOrder = new PayOrder();
        payOrder.setPayId(payId);
        payOrder.setOrderId(orderId);
        payOrder.setCreateDate(new Date().getTime());
        payOrder.setPayDate(0);
        payOrder.setCloseDate(0);
        payOrder.setParam(param);
        payOrder.setType(type);
        payOrder.setPrice(priceD);
        payOrder.setReallyPrice(reallyPrice);
        payOrder.setNotifyUrl(notifyUrl);
        payOrder.setReturnUrl(returnUrl);
        payOrder.setEpayName(epayName);
        payOrder.setEpayOrder(epayOrder);
        payOrder.setState(0);
        payOrder.setIsAuto(isAuto);
        payOrder.setPayUrl(payUrl);

        payOrderDao.save(payOrder);



        int timeOut;
        try {
            timeOut = Integer.parseInt(settingValue("close"));
        } catch (Exception e) {
            tmpPriceDao.delprice(PayUtils.priceKey(type, reallyPrice));
            payOrderDao.delete(payOrder);
            return ResUtil.error("订单有效期未正确配置");
        }
        if (timeOut <= 0) {
            tmpPriceDao.delprice(PayUtils.priceKey(type, reallyPrice));
            payOrderDao.delete(payOrder);
            return ResUtil.error("订单有效期未正确配置");
        }
        CreateOrderRes createOrderRes = new CreateOrderRes(payId,orderId,type,priceD,reallyPrice,payUrl,isAuto,0,timeOut,payOrder.getCreateDate());

        return ResUtil.success(createOrderRes);
    }
    // 关闭订单
    public CommonRes closeOrder(String orderId,String sign){

        String key = settingValue("key");
        if (key == null || key.isEmpty()) {
            return ResUtil.error("系统通讯密钥未配置");
        }
        String jsSign =  PayUtils.md5(orderId+key);
        if (!sign.equals(jsSign)){
            return ResUtil.error("签名校验不通过");
        }

        PayOrder payOrder = payOrderDao.findByOrderId(orderId);
        if (payOrder==null){
            return ResUtil.error("云端订单编号不存在");
        }
        if (payOrder.getState()!=0){
            return ResUtil.error("订单状态不允许关闭");
        }
        tmpPriceDao.delprice(PayUtils.priceKey(payOrder.getType(), payOrder.getReallyPrice()));
        payOrder.setCloseDate(new Date().getTime());
        payOrder.setState(-1);
        payOrderDao.save(payOrder);
        return ResUtil.success();
    }

    // 应用心跳
    public CommonRes appHeart(String t,String sign){
        String key = settingValue("key");
        if (key == null || key.isEmpty() || t == null || sign == null) {
            return ResUtil.error("参数不完整");
        }
        String jssign = PayUtils.md5(t+key);
        if (!jssign.equals(sign)){
            return ResUtil.error("签名校验错误");
        }
        long clientTime;
        try {
            clientTime = Long.parseLong(t);
        } catch (NumberFormatException e) {
            return ResUtil.error("客户端时间格式错误");
        }
        long cz = clientTime-new Date().getTime();

        if (cz<0){
            cz = cz*-1;
        }
        if (cz>50*1000){
            return ResUtil.error("客户端时间错误");
        }

        Setting setting = new Setting();
        setting.setVkey("lastheart");
        setting.setVvalue(t);
        settingDao.save(setting);

        setting.setVkey("jkstate");
        setting.setVvalue("1");
        settingDao.save(setting);

        return ResUtil.success();
    }

    // 应用推送
    public CommonRes appPush(Integer type,String price,String t,String sign){
        if (type == null || price == null || t == null || sign == null) {
            return ResUtil.error("推送参数不完整");
        }
        String key = settingValue("key");
        if (key == null || key.isEmpty()) {
            return ResUtil.error("系统通讯密钥未配置");
        }
        long paymentTime;
        double paymentPrice;
        try {
            paymentTime = Long.parseLong(t);
            paymentPrice = Double.parseDouble(price);
        } catch (NumberFormatException e) {
            return ResUtil.error("推送参数模式错误");
        }
        if (!Double.isFinite(paymentPrice) || paymentPrice <= 0) {
            return ResUtil.error("支付金额错误");
        }
        long cz = paymentTime-new Date().getTime();

        if (cz<0){
            cz = cz*-1;
        }
        if (cz>50*1000){
            return ResUtil.error("客户端时间错误");
        }
        String jssign = PayUtils.md5(type+""+price+t+key);
        if (!jssign.equals(sign)){
            return ResUtil.error("签名校验错误");
        }

        Setting setting = new Setting();
        setting.setVkey("lastpay");
        setting.setVvalue(t);
        settingDao.save(setting);

        PayOrder processedOrder = payOrderDao.findByPayDate(paymentTime);
        if (processedOrder != null) {
            if (String.valueOf(processedOrder.getPayId()).startsWith("无订单转账")
                    || processedOrder.getState() == 1) {
                return ResUtil.success();
            }
            return notifyPaidOrder(processedOrder, key);
        }

        PayOrder payOrder = payOrderDao.findByReallyPriceAndStateAndType(paymentPrice,0,type);

        if (payOrder==null){

            payOrder = new PayOrder();
            // 无订单转账记录追加付款时间戳，保证商户号/云端单号唯一（兼容唯一约束）
            String noOrderKey = "无订单转账" + paymentTime;
            payOrder.setPayId(noOrderKey);
            payOrder.setOrderId(noOrderKey);
            payOrder.setCreateDate(new Date().getTime());
            payOrder.setPayDate(new Date().getTime());
            payOrder.setCloseDate(new Date().getTime());
            payOrder.setParam("无订单转账");
            payOrder.setType(type);
            payOrder.setPrice(paymentPrice);
            payOrder.setReallyPrice(paymentPrice);
            payOrder.setPayDate(paymentTime);
            payOrder.setState(1);
            payOrder.setPayUrl("无订单转账");
            payOrderDao.save(payOrder);
            return ResUtil.success();

        }else{
            tmpPriceDao.delprice(PayUtils.priceKey(type, paymentPrice));

            payOrder.setState(1);
            payOrder.setPayDate(paymentTime);
            payOrder.setCloseDate(new Date().getTime());
            payOrderDao.save(payOrder);

            return notifyPaidOrder(payOrder, key);
        }
    }

    private CommonRes notifyPaidOrder(PayOrder payOrder, String key) {
        String url = payOrder.getNotifyUrl();
        if (url == null || url.trim().isEmpty()) {
            url = settingValue("notifyUrl");
            if (url != null && !url.trim().isEmpty()) {
                payOrder.setNotifyUrl(url);
                payOrderDao.save(payOrder);
            }
        }
        if (url == null || url.trim().isEmpty()) {
            payOrderDao.setState(2, payOrder.getId());
            return ResUtil.error("订单未配置异步通知地址");
        }

        String query;
        if (payOrder.isEpayOrder()) {
            query = EpaySignUtil.buildSignedQuery(buildEpayNotifyParams(payOrder), key);
        } else {
            query = "payId=" + PayUtils.encode(payOrder.getPayId()) + "&param=" + PayUtils.encode(payOrder.getParam())
                    + "&type=" + payOrder.getType() + "&price=" + PayUtils.encode(String.valueOf(payOrder.getPrice()))
                    + "&reallyPrice=" + PayUtils.encode(String.valueOf(payOrder.getReallyPrice()));
            String sign = PayUtils.md5(payOrder.getPayId() + payOrder.getParam() + payOrder.getType()
                    + payOrder.getPrice() + payOrder.getReallyPrice() + key);
            query += "&sign=" + sign;
        }

        String response = HttpRequest.sendGet(url, query);
        if (response != null && "success".equalsIgnoreCase(response.trim())) {
            payOrder.setState(1);
            payOrderDao.save(payOrder);
            return ResUtil.success();
        }

        payOrderDao.setState(2, payOrder.getId());
        return ResUtil.error("通知异步地址失败或无响应");
    }

    // 查询订单数据
    public CommonRes getOrder(String orderId){
        PayOrder payOrder = payOrderDao.findByOrderId(orderId);
        return buildOrderResponse(payOrder);
    }

    public CommonRes getOrderByPayId(String payId){
        PayOrder payOrder = payOrderDao.findByPayId(payId);
        return buildOrderResponse(payOrder);
    }

    private CommonRes buildOrderResponse(PayOrder payOrder){
        if (payOrder==null){
            return ResUtil.error("云端订单编号不存在");
        }

        String timeOut = settingValue("close");
        if (timeOut == null || timeOut.isEmpty()) {
            return ResUtil.error("订单有效期未配置");
        }
        CreateOrderRes createOrderRes = new CreateOrderRes(
                payOrder.getPayId(),payOrder.getOrderId(),payOrder.getType(),payOrder.getPrice(),payOrder.getReallyPrice()
                ,payOrder.getPayUrl(),payOrder.getIsAuto(),payOrder.getState(),Integer.valueOf(timeOut),payOrder.getCreateDate());

        return ResUtil.success(createOrderRes);
    }

    // 订单状态查询
    public CommonRes checkOrder(String orderId){
        PayOrder payOrder = payOrderDao.findByOrderId(orderId);
        if (payOrder==null){
            return ResUtil.error("云端订单编号不存在");
        }
        if (payOrder.getState()==0){
            return ResUtil.error("订单未支付");
        }
        if (payOrder.getState()==-1){
            return ResUtil.error("订单已过期");
        }
        String key = settingValue("key");
        if (key == null || key.isEmpty()) {
            return ResUtil.error("系统通讯密钥未配置");
        }
        //执行通知
        String url = payOrder.getReturnUrl();
        if (url==null || url.equals("")){
            url = settingValue("returnUrl");
        }
        if (url == null || url.trim().isEmpty()) {
            return ResUtil.error("订单未配置同步跳转地址");
        }

        if (payOrder.isEpayOrder()) {
            return ResUtil.success(appendQuery(url, EpaySignUtil.buildSignedQuery(buildEpayNotifyParams(payOrder), key)));
        }

        String p = "payId="+PayUtils.encode(payOrder.getPayId())+"&param="+PayUtils.encode(payOrder.getParam())
                +"&type="+payOrder.getType()+"&price="+PayUtils.encode(String.valueOf(payOrder.getPrice()))
                +"&reallyPrice="+PayUtils.encode(String.valueOf(payOrder.getReallyPrice()));
        String sign = PayUtils.md5(payOrder.getPayId()+payOrder.getParam()+payOrder.getType()+payOrder.getPrice()+payOrder.getReallyPrice()+key);
        p = p+"&sign="+sign;
        return ResUtil.success(appendQuery(url, p));
    }

    /**
     * 构建易支付异步通知的签名查询串
     * <p>
     * 按易支付规范组装异步通知参数并生成 MD5 签名，返回形如
     * name1=value1&amp;name2=value2&amp;sign=xxx 的查询串，供支付成功自动通知、
     * 后台手动补单等场景复用，避免签名逻辑分散。
     * </p>
     *
     * @param payOrder 支付订单
     * @param key      系统通讯密钥
     * @return 带签名的请求查询串
     */
    public String buildEpayNotifyQuery(PayOrder payOrder, String key) {
        return EpaySignUtil.buildSignedQuery(buildEpayNotifyParams(payOrder), key);
    }

    private Map<String, String> buildEpayNotifyParams(PayOrder payOrder) {
        Map<String, String> params = new HashMap<>();
        params.put("pid", settingValue("pid"));
        params.put("trade_no", payOrder.getOrderId());
        params.put("out_trade_no", payOrder.getPayId());
        params.put("type", payOrder.getType() == 1 ? "wxpay" : "alipay");
        params.put("name", payOrder.getEpayName() == null || payOrder.getEpayName().isEmpty()
                ? "商品" : payOrder.getEpayName());
        params.put("money", new BigDecimal(String.valueOf(payOrder.getPrice())).setScale(2).toPlainString());
        params.put("trade_status", "TRADE_SUCCESS");
        if (payOrder.getParam() != null && !payOrder.getParam().isEmpty()) {
            params.put("param", payOrder.getParam());
        }
        params.put("sign_type", "MD5");
        return params;
    }

    public CommonRes getState(String t,String sign){

        String key = settingValue("key");
        if (key == null || key.isEmpty() || t == null || sign == null) {
            return ResUtil.error("参数不完整");
        }
        String jsSign =  PayUtils.md5(t+key);
        if (!sign.equals(jsSign)){
            return ResUtil.error("签名校验不通过");
        }

        Map<String,String> map = new HashMap<>();
        String state = settingValue("jkstate");
        String lastheart = settingValue("lastheart");
        String lastpay = settingValue("lastpay");
        if (state == null || lastheart == null || lastpay == null) {
            return ResUtil.error("监控端状态未初始化");
        }
        map.put("state",state);
        map.put("lastheart",lastheart);
        map.put("lastpay",lastpay);

        return ResUtil.success(map);
    }

    private static String appendQuery(String url, String query) {
        if (url == null || url.isEmpty()) {
            return query;
        }
        return url + (url.contains("?") ? "&" : "?") + query;
    }


    private String settingValue(String key) {
        return settingDao.findById(key).map(Setting::getVvalue).orElse(null);
    }
}
