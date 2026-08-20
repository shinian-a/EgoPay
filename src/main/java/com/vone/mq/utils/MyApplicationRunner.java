package com.vone.mq.utils;

import com.vone.mq.dao.SettingDao;
import com.vone.mq.entity.Setting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

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
        int row = (int) settingDao.count();
        if (row == 0) {
            Setting setting = new Setting();
            //管理员账号
            setting.setVkey("user");
            setting.setVvalue("admin");
            settingDao.save(setting);

            //管理员密码
            setting.setVkey("pass");
            setting.setVvalue("admin");
            settingDao.save(setting);

            // 易支付商户 ID
            setting.setVkey("pid");
            setting.setVvalue("1000");
            settingDao.save(setting);

            //异步通知地址
            setting.setVkey("notifyUrl");
            setting.setVvalue(baseUrl + "/returnNotify");
            settingDao.save(setting);

            //同步通知地址
            setting.setVkey("returnUrl");
            setting.setVvalue(baseUrl +"/returnNotify");
            settingDao.save(setting);

            //通讯密钥
            setting.setVkey("key");
            setting.setVvalue(md5(String.valueOf(new Date().getTime())));
            settingDao.save(setting);

            //监控端最后心跳
            setting.setVkey("lastheart");
            setting.setVvalue("0");
            settingDao.save(setting);

            //监控端最后收款
            setting.setVkey("lastpay");
            setting.setVvalue("0");
            settingDao.save(setting);

            //监控端状态
            setting.setVkey("jkstate");
            setting.setVvalue("-1");
            settingDao.save(setting);

            //订单最有效时间
            setting.setVkey("close");
            setting.setVvalue("5");
            settingDao.save(setting);

            //区分方式
            setting.setVkey("payQf");
            setting.setVvalue("1");
            settingDao.save(setting);

            //微信通用收款码
            setting.setVkey("wxpay");
            setting.setVvalue("");
            settingDao.save(setting);
            //支付宝通用收款码
            setting.setVkey("zfbpay");
            setting.setVvalue("");
            settingDao.save(setting);
        }

        if (!settingDao.findById("pid").isPresent()) {
            Setting setting = new Setting();
            setting.setVkey("pid");
            setting.setVvalue("1000");
            settingDao.save(setting);
        }
    }

    public static String md5(String text) {
        return DigestUtils.md5DigestAsHex(text.getBytes());
    }
}
