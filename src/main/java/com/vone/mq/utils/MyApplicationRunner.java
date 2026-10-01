package com.vone.mq.utils;

import com.vone.mq.dao.SettingDao;
import com.vone.mq.entity.Setting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 首次启动数据库初始化
 */
@Component
public class MyApplicationRunner implements ApplicationRunner {
    @Autowired
    private SettingDao settingDao;
    @Override
    public void run(ApplicationArguments var1) throws UnknownHostException {
        // 一行获取当前服务器地址
        String baseUrl = "http://" + InetAddress.getLocalHost().getHostAddress();
        //查询是不是首次启动，如果是就创建基础的设置数据
        long row = settingDao.count();
        if (row == 0) {
            List<Setting> settings = new ArrayList<>();
            //管理员账号 / 密码
            settings.add(setting("user", "admin"));
            settings.add(setting("pass", "admin"));
            // 易支付商户 ID
            settings.add(setting("pid", "1000"));
            //异步通知地址
            settings.add(setting("notifyUrl", baseUrl + "/returnNotify"));
            //同步通知地址
            settings.add(setting("returnUrl", baseUrl + "/returnNotify"));
            //通讯密钥
            settings.add(setting("key", PayUtils.md5(String.valueOf(new Date().getTime()))));
            //监控端最后心跳 / 最后收款 / 状态
            settings.add(setting("lastheart", "0"));
            settings.add(setting("lastpay", "0"));
            settings.add(setting("jkstate", "-1"));
            //订单最有效时间（分钟）
            settings.add(setting("close", "5"));
            //金额区分方式
            settings.add(setting("payQf", "1"));
            //微信 / 支付宝通用收款码
            settings.add(setting("wxpay", ""));
            settings.add(setting("zfbpay", ""));
            // 批量落库，减少首次启动的数据库交互次数
            settingDao.saveAll(settings);
        }

        if (!settingDao.findById("pid").isPresent()) {
            settingDao.save(setting("pid", "1000"));
        }
    }

    private static Setting setting(String key, String value) {
        Setting setting = new Setting();
        setting.setVkey(key);
        setting.setVvalue(value);
        return setting;
    }
}
