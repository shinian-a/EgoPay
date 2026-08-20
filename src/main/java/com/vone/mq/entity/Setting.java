package com.vone.mq.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * 系统设置实体类
 * <p>
 * 用于存储和管理支付系统的各项配置参数，采用键值对（Key-Value）形式存储。
 * 系统启动时会自动初始化基础配置数据，包括管理员账号、通讯密钥、收款码等。
 * </p>
 * 
 * <p>支持的配置项包括：</p>
 * <ul>
 *     <li>user - 管理员账号</li>
 *     <li>pass - 管理员密码</li>
 *     <li>notifyUrl - 异步通知地址</li>
 *     <li>returnUrl - 同步通知地址</li>
 *     <li>key - 通讯密钥（MD5 加密）</li>
 *     <li>lastheart - 监控端最后心跳时间戳</li>
 *     <li>lastpay - 监控端最后收款时间戳</li>
 *     <li>jkstate - 监控端状态（-1:未绑定，0:掉线，1:正常）</li>
 *     <li>close - 订单最有效时间（分钟）</li>
 *     <li>payQf - 价格区分方式（1:金额递增，2:金额递减）</li>
 *     <li>wxpay - 微信通用收款码 URL</li>
 *     <li>zfbpay - 支付宝通用收款码 URL</li>
 * </ul>
 */
@Entity
public class Setting {
    /**
     * 配置项的键名
     * <p>
     * 作为数据库表的主键，唯一标识一个配置项。
     * 例如："user", "pass", "notifyUrl" 等
     * </p>
     */
    @Id
    private String vkey;
    
    /**
     * 配置项的值
     * <p>
     * 存储对应键名的配置值，可以是字符串、数字、URL 等形式。
     * </p>
     */
    private String vvalue;

    /**
     * 获取配置项的键名
     *
     * @return 配置项的键名
     */
    public String getVkey() {
        return vkey;
    }

    /**
     * 设置配置项的键名
     *
     * @param vkey 配置项的键名
     */
    public void setVkey(String vkey) {
        this.vkey = vkey;
    }

    /**
     * 获取配置项的值
     *
     * @return 配置项的值
     */
    public String getVvalue() {
        return vvalue;
    }

    /**
     * 设置配置项的值
     *
     * @param vvalue 配置项的值
     */
    public void setVvalue(String vvalue) {
        this.vvalue = vvalue;
    }
}
