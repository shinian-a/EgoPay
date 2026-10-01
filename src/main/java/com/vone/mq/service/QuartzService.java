package com.vone.mq.service;

import com.vone.mq.dao.PayOrderDao;
import com.vone.mq.dao.SettingDao;
import com.vone.mq.dao.TmpPriceDao;
import com.vone.mq.entity.PayOrder;
import com.vone.mq.entity.Setting;
import com.vone.mq.utils.PayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Optional;


/**
 * 服务器定时任务
 */
@Component
public class QuartzService {

    private static final Logger log = LoggerFactory.getLogger(QuartzService.class);

    @Autowired
    private SettingDao settingDao;
    @Autowired
    private PayOrderDao payOrderDao;
    @Autowired
    private TmpPriceDao tmpPriceDao;

    // 定时器间隔30秒
    @Scheduled(fixedRate = 30000)
    public void timerToZZP(){
        try {
            // 清理过期超时订单
            Optional<Setting> closeSetting = settingDao.findById("close");
            if (!closeSetting.isPresent()) {
                log.debug("未找到 close 设置，跳过过期订单清理");
                return;
            }

            String timeout = closeSetting.get().getVvalue();
            String closeTime = String.valueOf(new Date().getTime());
            timeout = String.valueOf(new Date().getTime() - (long) Integer.parseInt(timeout) *60*1000);

            int row = payOrderDao.setTimeout(timeout,closeTime);

            List<PayOrder> payOrders = payOrderDao.findAllByCloseDate(Long.valueOf(closeTime));
            for (PayOrder payOrder: payOrders) {
                tmpPriceDao.delprice(PayUtils.priceKey(payOrder.getType(), payOrder.getReallyPrice()));
            }
            log.debug("成功清理 {} 个过期订单", row);
        }catch (Exception e){
            log.error("清理过期订单异常", e);
        }
        // 监控端状态和最后心跳
        try {
            Optional<Setting> heartSetting = settingDao.findById("lastheart");
            Optional<Setting> stateSetting = settingDao.findById("jkstate");

            if (!heartSetting.isPresent() || !stateSetting.isPresent()) {
                log.debug("未找到心跳或状态设置");
                return;
            }

            String lastheart = heartSetting.get().getVvalue();
            String state = stateSetting.get().getVvalue();

            if (state.equals("1") && new Date().getTime() - Long.parseLong(lastheart) > 60*1000){
                Setting setting = new Setting();
                setting.setVkey("jkstate");
                setting.setVvalue("0");
                settingDao.save(setting);
            }
        } catch (Exception e) {
            log.error("监控端心跳状态处理异常", e);
        }
    }
}
