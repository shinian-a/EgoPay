package com.vone.mq.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * 支付二维码实体类
 * <p>
 * 用于存储用户上传的微信和支付宝收款码信息。系统会将这些收款码与订单关联，
 * 当用户发起支付时，系统根据订单类型（微信支付/支付宝支付）选择对应的收款码，
 * 生成支付链接返回给前端。
 * </p>
 */
@Entity
public class PayQrcode {
    /**
     * 收款码记录的唯一标识 ID
     * <p>
     * 使用数据库自增策略自动生成
     * </p>
     */
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;
    
    /**
     * 收款码的 URL 地址
     * <p>
     * 存储用户上传的微信或支付宝收款码图片的地址，可以是 Base64 编码、文件路径或网络 URL
     * </p>
     */
    private String payUrl;
    
    /**
     * 收款码的金额
     * <p>
     * 表示该收款码对应的固定收款金额，系统会根据订单金额匹配相应价格的收款码
     * </p>
     */
    private double price;
    
    /**
     * 支付方式类型
     * <p>
     * 标识该收款码属于哪种支付方式：
     * </p>
     * <ul>
     *     <li>1 - 微信支付</li>
     *     <li>2 - 支付宝支付</li>
     * </ul>
     */
    private int type;


    /**
     * 获取收款码记录 ID
     *
     * @return 收款码记录的唯一标识 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置收款码记录 ID
     *
     * @param id 收款码记录的唯一标识 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取收款码的 URL 地址
     *
     * @return 收款码的 URL 地址
     */
    public String getPayUrl() {
        return payUrl;
    }

    /**
     * 设置收款码的 URL 地址
     *
     * @param payUrl 收款码的 URL 地址
     */
    public void setPayUrl(String payUrl) {
        this.payUrl = payUrl;
    }

    /**
     * 获取收款码的金额
     *
     * @return 收款码的金额
     */
    public double getPrice() {
        return price;
    }

    /**
     * 设置收款码的金额
     *
     * @param price 收款码的金额
     */
    public void setPrice(double price) {
        this.price = price;
    }

    /**
     * 获取支付方式类型
     *
     * @return 支付方式类型，1 表示微信支付，2 表示支付宝支付
     */
    public int getType() {
        return type;
    }

    /**
     * 设置支付方式类型
     *
     * @param type 支付方式类型，1 表示微信支付，2 表示支付宝支付
     */
    public void setType(int type) {
        this.type = type;
    }

}
