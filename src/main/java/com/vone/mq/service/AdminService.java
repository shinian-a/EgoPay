package com.vone.mq.service;

import com.vone.mq.dao.PayOrderDao;
import com.vone.mq.dao.PayQrcodeDao;
import com.vone.mq.dao.SettingDao;
import com.vone.mq.dao.TmpPriceDao;
import com.vone.mq.dto.CommonRes;
import com.vone.mq.dto.PageRes;
import com.vone.mq.entity.PayOrder;
import com.vone.mq.entity.PayQrcode;
import com.vone.mq.entity.Setting;
import com.vone.mq.utils.Arith;
import com.vone.mq.utils.HttpRequest;
import com.vone.mq.utils.ResUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;


import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.*;

@Service
public class AdminService {

    @Autowired
    private SettingDao settingDao;
    @Autowired
    private PayOrderDao payOrderDao;
    @Autowired
    private TmpPriceDao tmpPriceDao;
    @Autowired
    private PayQrcodeDao payQrcodeDao;
    public CommonRes login(String user,String pass){
        String configuredUser = settingDao.findById("user").map(Setting::getVvalue).orElse(null);
        String configuredPass = settingDao.findById("pass").map(Setting::getVvalue).orElse(null);
        if (configuredUser == null || configuredPass == null
                || !configuredUser.equals(user) || !configuredPass.equals(pass)){
            return ResUtil.error("账号或密码不正确");
        }

        return ResUtil.success();
    }
    public CommonRes saveSetting(String user,String pass,String pid,String notifyUrl,String returnUrl,String key,String wxpay,String zfbpay,String close,String payQf){
        Setting s = new Setting();
        s.setVkey("user");
        s.setVvalue(user);
        settingDao.save(s);

        s.setVkey("pass");
        s.setVvalue(pass);
        settingDao.save(s);

        if (pid != null && !pid.trim().isEmpty()) {
            s.setVkey("pid");
            s.setVvalue(pid);
            settingDao.save(s);
        }

        s.setVkey("notifyUrl");
        s.setVvalue(notifyUrl);
        settingDao.save(s);

        s.setVkey("returnUrl");
        s.setVvalue(returnUrl);
        settingDao.save(s);

        s.setVkey("key");
        s.setVvalue(key);
        settingDao.save(s);

        s.setVkey("wxpay");
        s.setVvalue(wxpay);
        settingDao.save(s);
        s.setVkey("zfbpay");
        s.setVvalue(zfbpay);
        settingDao.save(s);


        s.setVkey("payQf");
        s.setVvalue(payQf);
        settingDao.save(s);

        s.setVkey("close");
        s.setVvalue(close);
        settingDao.save(s);
        return ResUtil.success();
    }
    public CommonRes getSettings(){
        List<Setting> settings = settingDao.findAll();
        Map<String,String> map = new HashMap<>();
        for (Setting s:settings ) {
            map.put(s.getVkey(),s.getVvalue());
        }
        return ResUtil.success(map);
    }

    public PageRes getOrders(Integer page, Integer limit, Integer type, Integer state){

        if (page == null || page < 1) {
            page = 1;
        }
        if (limit == null || limit < 1 || limit > 100) {
            limit = 10;
        }
        Pageable pageable = PageRequest.of(page-1, limit, Sort.Direction.DESC, "id");

        Specification<PayOrder> specification = new Specification<PayOrder>() {
            @Override
            public Predicate toPredicate(Root<PayOrder> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb) {
                List<Predicate> list = new ArrayList<Predicate>();

                if (type!=null) {
                    list.add(cb.equal(root.get("type").as(int.class), type));
                }

                if (state!=null) {
                    list.add(cb.equal(root.get("state").as(int.class), state));
                }
                return cb.and(list.toArray(new Predicate[list.size()]));
            }
        };
        Page<PayOrder> payOrders = payOrderDao.findAll(specification, pageable);

        PageRes list = PageRes.success(payOrders.getTotalElements(),payOrders.getContent());
        return list;
    }

    public CommonRes setBd(Integer id){
        PayOrder payOrder = payOrderDao.findById(Long.valueOf(id)).orElse(null);
        if (payOrder==null){
            return ResUtil.error("订单不存在");
        }
        String key = settingDao.findById("key").map(Setting::getVvalue).orElse(null);
        if (key == null || key.isEmpty()) {
            return ResUtil.error("系统通讯密钥未配置");
        }
        String p = "payId="+payOrder.getPayId()+"&param="+payOrder.getParam()+"&type="+payOrder.getType()+"&price="+payOrder.getPrice()+"&reallyPrice="+payOrder.getReallyPrice();
        String sign = payOrder.getPayId()+payOrder.getParam()+payOrder.getType()+payOrder.getPrice()+payOrder.getReallyPrice()+key;
        p = p+"&sign="+md5(sign);

        String url = payOrder.getNotifyUrl();
        if (url==null || url.equals("")){
            url = settingDao.findById("notifyUrl").map(Setting::getVvalue).orElse(null);
            if (url==null || url.equals("")){
                return ResUtil.error("您还未配置异步通知地址，请现在系统配置中配置");
            }
        }

        String res = HttpRequest.sendGet(url,p);

        if (res!=null && res.equals("success")){
            if (payOrder.getState()==0){
                tmpPriceDao.delprice(priceKey(payOrder.getType(), payOrder.getReallyPrice()));
            }
            payOrderDao.setState(1,payOrder.getId());
            return ResUtil.success();
        }else{
            return ResUtil.error(-2,res);
        }

    }

    public CommonRes addPayQrcode(PayQrcode payQrcode){
        if (payQrcode.getPayUrl()==null){
            return ResUtil.error();
        }
        if (payQrcode.getPrice()==0){
            return ResUtil.error();
        }
        if (payQrcode.getType()==0){
            return ResUtil.error();
        }
        payQrcodeDao.save(payQrcode);
        return ResUtil.success();
    }

    public CommonRes getMain(){
        Calendar currentDate = new GregorianCalendar();

        currentDate.set(Calendar.HOUR_OF_DAY, 0);
        currentDate.set(Calendar.MINUTE, 0);
        currentDate.set(Calendar.SECOND, 0);
        Date tmp = (Date) currentDate.getTime();
        String startDate = String.valueOf(tmp.getTime());
        currentDate = new GregorianCalendar();
        currentDate.set(Calendar.HOUR_OF_DAY, 23);
        currentDate.set(Calendar.MINUTE, 59);
        currentDate.set(Calendar.SECOND, 59);
        tmp = (Date) currentDate.getTime();
        String endDate = String.valueOf(tmp.getTime());

        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(2);

        int todayOrder = payOrderDao.getTodayCount(startDate,endDate);//当日总订单

        int todaySuccessOrder =  payOrderDao.getTodayCount(startDate,endDate,1);//当日成功订单
        int todaySuccessOrder2 =  payOrderDao.getTodayCount(startDate,endDate,2);//当日成功订单
        todaySuccessOrder += todaySuccessOrder2;

        int todayCloseOrder =  payOrderDao.getTodayCount(startDate,endDate,-1);//当日失败订单

        double todayMoney;
        double todayMoney2;
        try {
            todayMoney = payOrderDao.getTodayCountMoney(startDate,endDate,1);
        }catch (Exception e){
            todayMoney = 0;
        }
        try {
            todayMoney2 = payOrderDao.getTodayCountMoney(startDate,endDate,2);
        }catch (Exception e){
            todayMoney2 = 0;
        }

        todayMoney = Arith.add(todayMoney,todayMoney2);

        int countOrder = payOrderDao.getCount(1);
        double countMoney;
        double countMoney2;
        try {
            countMoney = payOrderDao.getCountMoney(1);
        }catch (Exception e){
            countMoney = 0;
        }
        try {
            countMoney2 = payOrderDao.getCountMoney(2);
        }catch (Exception e){
            countMoney2 = 0;
        }
        countMoney = Arith.add(countMoney,countMoney2);


        Map<String,String> map = new HashMap<>();
        map.put("todayOrder", String.valueOf(todayOrder));
        map.put("todaySuccessOrder", String.valueOf(todaySuccessOrder));
        map.put("todayCloseOrder", String.valueOf(todayCloseOrder));
        map.put("todayMoney", nf.format(todayMoney));
        map.put("countOrder", String.valueOf(countOrder));
        map.put("countMoney",nf.format(countMoney));

        return ResUtil.success(map);
    }

    public PageRes getPayQrcodes(Integer page, Integer limit, Integer type){

        if (page == null || page < 1) {
            page = 1;
        }
        if (limit == null || limit < 1 || limit > 100) {
            limit = 10;
        }
        Pageable pageable = PageRequest.of(page-1, limit, Sort.Direction.DESC, "id");

        Specification<PayQrcode> specification = new Specification<PayQrcode>() {
            @Override
            public Predicate toPredicate(Root<PayQrcode> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb) {
                List<Predicate> list = new ArrayList<Predicate>();

                if (type!=null) {
                    list.add(cb.equal(root.get("type").as(int.class), type));
                }

                return cb.and(list.toArray(new Predicate[list.size()]));
            }
        };
        Page<PayQrcode> payQrcodes = payQrcodeDao.findAll(specification, pageable);

        PageRes list = PageRes.success(payQrcodes.getTotalElements(),payQrcodes.getContent());
        return list;
    }
    public CommonRes delPayQrcode(Long id){
        if (id == null || !payQrcodeDao.existsById(id)) {
            return ResUtil.error("二维码不存在");
        }
        payQrcodeDao.deleteById(id);
        return ResUtil.success();
    }
    public CommonRes delOrder(Long id){
        if (id == null) {
            return ResUtil.error("订单不存在");
        }
        PayOrder payOrder = payOrderDao.findById(id).orElse(null);
        if (payOrder == null) {
            return ResUtil.error("订单不存在");
        }
        if (payOrder.getState()==0){
            tmpPriceDao.delprice(priceKey(payOrder.getType(), payOrder.getReallyPrice()));
        }
        payOrderDao.deleteById(id);
        return ResUtil.success();
    }
    public CommonRes delGqOrder(){
        for (PayOrder payOrder : payOrderDao.findAllByState(-1)) {
            tmpPriceDao.delprice(priceKey(payOrder.getType(), payOrder.getReallyPrice()));
        }
        payOrderDao.deleteByState(-1);
        return ResUtil.success();
    }

    public CommonRes delLastOrder(){
        long threshold = new Date().getTime()-7*86400*1000;
        for (PayOrder payOrder : payOrderDao.findAllByCreateDateBefore(threshold)) {
            if (payOrder.getState() == 0) {
                tmpPriceDao.delprice(priceKey(payOrder.getType(), payOrder.getReallyPrice()));
            }
        }
        payOrderDao.deleteByAfterCreateDate(String.valueOf(threshold));
        return ResUtil.success();
    }


    public static String md5(String text) {
        //加密后的字符串
        String encodeStr= DigestUtils.md5DigestAsHex(text.getBytes());
        return encodeStr;
    }

    private static String priceKey(int type, double price) {
        return type + "-" + java.math.BigDecimal.valueOf(price)
                .stripTrailingZeros().toPlainString();
    }
}
