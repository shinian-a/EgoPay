package com.vone.mq.utils;

import com.vone.mq.MqApplication;
import com.vone.mq.dao.SettingDao;
import com.vone.mq.entity.Setting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 启动初始化
 * <p>
 * 首次启动（数据库为空）时写入默认配置并打印“初始化数据库”日志；
 * 之后每次启动只打印“系统启动完毕”日志与启动耗时。
 * </p>
 */
@Component
public class MyApplicationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MyApplicationRunner.class);

    /** 获取本机地址失败时的回退 IP */
    private static final String FALLBACK_IP = "127.0.0.1";

    /** 本机 IP 缓存，一次启动内只解析一次 */
    private static String CACHED_IP;

    @Autowired
    private SettingDao settingDao;

    @Value("${server.port:8081}")
    private String serverPort;

    @Override
    public void run(ApplicationArguments var1) {
        // 查询是不是首次启动，如果是就创建基础的设置数据
        long row = settingDao.count();
        boolean firstBoot = row == 0;

        if (firstBoot) {
            log.info("首次启动：数据库为空，开始初始化数据库");
            long initStart = System.currentTimeMillis();

            // 用本机地址生成默认回调地址；解析结果会缓存，启动日志复用同一次结果
            String baseUrl = "http://" + resolveLocalIp();

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

            log.info("数据库初始化完成：已写入 {} 项默认配置，耗时 {} ms（默认管理员 admin/admin，请尽快登录后台修改）",
                    settings.size(), System.currentTimeMillis() - initStart);
        }

        if (settingDao.findById("pid").isEmpty()) {
            settingDao.save(setting("pid", "1000"));
            log.info("补齐缺失的配置项：pid=1000");
        }

        String localIp = resolveLocalIp();
        log.info("系统启动完毕，耗时 {} 秒，测试收银台 http://{}:{}/payTest.html，后台 http://{}:{}/admin.html",
                elapsedSeconds(), localIp, serverPort, localIp, serverPort);
    }

    /**
     * 解析本机 IP，失败时回退到 127.0.0.1，避免主机名解析异常导致启动中断。
     * 结果缓存，同一次启动内只解析一次。
     */
    private static synchronized String resolveLocalIp() {
        if (CACHED_IP != null) {
            return CACHED_IP;
        }
        try {
            CACHED_IP = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            log.warn("获取本机 IP 失败，回退为 {}，请稍后在后台手动修改通知地址", FALLBACK_IP, e);
            CACHED_IP = FALLBACK_IP;
        }
        return CACHED_IP;
    }

    /** 从主类加载时刻算起的启动耗时（秒） */
    private static String elapsedSeconds() {
        return String.format("%.3f", (System.currentTimeMillis() - MqApplication.START_TIME) / 1000.0);
    }

    private static Setting setting(String key, String value) {
        Setting setting = new Setting();
        setting.setVkey(key);
        setting.setVvalue(value);
        return setting;
    }
}
