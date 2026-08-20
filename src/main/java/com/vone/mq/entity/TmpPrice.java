package com.vone.mq.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * 临时价格占用实体类
 * <p>
 * 用于记录支付系统中已被占用的价格，防止多个订单使用相同的实际支付金额。
 * 当用户创建支付订单时，系统会为订单分配一个唯一的实际支付金额（reallyPrice），
 * 并将该金额记录到此表中，避免其他订单重复使用。
 * </p>
 */
@Entity
public class TmpPrice {
    /**
     * 被占用的价格标识
     * <p>
     * 格式为："支付方式类型 - 实际支付金额"
     * 例如："1-0.01" 表示微信支付 0.01 元，"2-5.20" 表示支付宝支付 5.20 元
     * </p>
     */
    @Id
    private String price;

    /**
     * 获取被占用的价格
     *
     * @return 价格字符串，格式为 "type-price"
     */
    public String getPrice() {
        return price;
    }

    /**
     * 设置被占用的价格
     *
     * @param price 价格字符串，格式为 "type-price"
     */
    public void setPrice(String price) {
        this.price = price;
    }
}
