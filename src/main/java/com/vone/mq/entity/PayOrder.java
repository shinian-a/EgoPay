package com.vone.mq.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * 支付订单实体类
 * <p>
 * 用于记录和管理用户通过系统创建的所有支付订单。每个订单包含完整的支付信息，
 * 包括订单号、金额、支付方式、通知地址等。系统会根据订单状态进行监控和处理，
 * 在检测到支付完成后自动触发异步通知和同步跳转。
 * </p>
 */
@Entity
public class PayOrder {
    /**
     * 订单记录的唯一标识 ID
     * <p>
     * 使用数据库自增策略自动生成
     * </p>
     */
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;

    /**
     * 云端生成的唯一订单号
     * <p>
     * 由系统自动生成，格式为：yyyyMMddHHmmss + 4 位随机数
     * 用于在支付系统中唯一标识一笔交易
     * </p>
     */
    private String orderId;

    /**
     * 商户订单号
     * <p>
     * 由调用方（商户）传入的订单号，通常可以是时间戳或其他业务系统的订单编号
     * 该号码会在异步通知和同步跳转时原样返回给商户
     * </p>
     */
    private String payId;


    /**
     * 订单创建时间戳（13 位毫秒级）
     */
    private long createDate;
    
    /**
     * 订单支付成功时间戳（13 位毫秒级）
     * <p>
     * 系统检测到用户完成支付的时间点
     * </p>
     */
    private long payDate;
    
    /**
     * 订单关闭时间戳（13 位毫秒级）
     * <p>
     * 订单过期或超时未支付时的关闭时间
     * </p>
     */
    private long closeDate;


    /**
     * 订单自定义参数
     * <p>
     * 由商户传入的附加信息，系统会原封不动地返回给异步通知接口和同步跳转接口
     * 通常用于传递业务系统的订单备注、用户 ID 等信息
     * </p>
     */
    private String param;

    /**
     * 支付方式类型
     * <p>
     * 标识该订单使用的支付方式：
     * </p>
     * <ul>
     *     <li>1 - 微信支付</li>
     *     <li>2 - 支付宝支付</li>
     * </ul>
     */
    private int type;

    /**
     * 订单金额
     * <p>
     * 商户发起的原始订单金额
     * </p>
     */
    private double price;

    /**
     * 实际支付金额
     * <p>
     * 用户实际扫码支付的金额。由于可能有多人同时支付，系统会通过递增或递减的方式
     * 生成一个唯一的实际支付金额，用于区分不同的订单
     * </p>
     */
    private double reallyPrice;

    /**
     * 异步通知地址
     * <p>
     * 支付成功后，系统会向该地址发送 GET 请求通知商户
     * 如果订单未设置，则使用系统配置中的全局异步通知地址
     * </p>
     */
    private String notifyUrl;
    
    /**
     * 同步跳转地址
     * <p>
     * 支付成功后，用户浏览器会跳转到该地址
     * 如果订单未设置，则使用系统配置中的全局同步通知地址
     * </p>
     */
    private String returnUrl;

    /**
     * 易支付商品名称。
     */
    private String epayName;

    /**
     * 是否通过易支付兼容接口创建。
     */
    private boolean epayOrder;

    /**
     * 订单状态
     * <p>
     * 表示订单当前的状态：
     * </p>
     * <ul>
     *     <li>-1 - 订单过期（超过有效期未支付）</li>
     *     <li>0 - 等待支付</li>
     *     <li>1 - 支付成功</li>
     * </ul>
     */
    private int state;

    /**
     * 是否为通用二维码标识
     * <p>
     * 标识该订单使用的是否为通用收款码：
     * </p>
     * <ul>
     *     <li>1 - 通用二维码（动态匹配金额）</li>
     *     <li>0 - 固定转账二维码（固定金额）</li>
     * </ul>
     */
    private int isAuto;
    
    /**
     * 二维码内容
     * <p>
     * 存储生成的支付二维码的内容，通常是支付链接 URL
     * </p>
     */
    private String payUrl;


    /**
     * 获取二维码内容
     *
     * @return 二维码内容
     */
    public String getPayUrl() {
        return payUrl;
    }

    /**
     * 设置二维码内容
     *
     * @param payUrl 二维码内容
     */
    public void setPayUrl(String payUrl) {
        this.payUrl = payUrl;
    }


    /**
     * 获取商户订单号
     *
     * @return 商户订单号
     */
    public String getPayId() {
        return payId;
    }

    /**
     * 设置商户订单号
     *
     * @param payId 商户订单号
     */
    public void setPayId(String payId) {
        this.payId = payId;
    }

    /**
     * 获取订单记录 ID
     *
     * @return 订单记录的唯一标识 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置订单记录 ID
     *
     * @param id 订单记录的唯一标识 ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取云端订单号
     *
     * @return 云端生成的唯一订单号
     */
    public String getOrderId() {
        return orderId;
    }

    /**
     * 设置云端订单号
     *
     * @param orderId 云端生成的唯一订单号
     */
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    /**
     * 获取订单创建时间戳
     *
     * @return 订单创建时间戳（13 位毫秒级）
     */
    public long getCreateDate() {
        return createDate;
    }

    /**
     * 设置订单创建时间戳
     *
     * @param createDate 订单创建时间戳（13 位毫秒级）
     */
    public void setCreateDate(long createDate) {
        this.createDate = createDate;
    }

    /**
     * 获取订单支付时间戳
     *
     * @return 订单支付成功时间戳（13 位毫秒级）
     */
    public long getPayDate() {
        return payDate;
    }

    /**
     * 设置订单支付时间戳
     *
     * @param payDate 订单支付成功时间戳（13 位毫秒级）
     */
    public void setPayDate(long payDate) {
        this.payDate = payDate;
    }

    /**
     * 获取订单关闭时间戳
     *
     * @return 订单关闭时间戳（13 位毫秒级）
     */
    public long getCloseDate() {
        return closeDate;
    }

    /**
     * 设置订单关闭时间戳
     *
     * @param closeDate 订单关闭时间戳（13 位毫秒级）
     */
    public void setCloseDate(long closeDate) {
        this.closeDate = closeDate;
    }

    /**
     * 获取订单自定义参数
     *
     * @return 订单自定义参数
     */
    public String getParam() {
        return param;
    }

    /**
     * 设置订单自定义参数
     *
     * @param param 订单自定义参数
     */
    public void setParam(String param) {
        this.param = param;
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

    /**
     * 获取订单金额
     *
     * @return 订单金额
     */
    public double getPrice() {
        return price;
    }

    /**
     * 设置订单金额
     *
     * @param price 订单金额
     */
    public void setPrice(double price) {
        this.price = price;
    }

    /**
     * 获取实际支付金额
     *
     * @return 实际支付金额
     */
    public double getReallyPrice() {
        return reallyPrice;
    }

    /**
     * 设置实际支付金额
     *
     * @param reallyPrice 实际支付金额
     */
    public void setReallyPrice(double reallyPrice) {
        this.reallyPrice = reallyPrice;
    }

    /**
     * 获取异步通知地址
     *
     * @return 异步通知地址
     */
    public String getNotifyUrl() {
        return notifyUrl;
    }

    /**
     * 设置异步通知地址
     *
     * @param notifyUrl 异步通知地址
     */
    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    /**
     * 获取同步跳转地址
     *
     * @return 同步跳转地址
     */
    public String getReturnUrl() {
        return returnUrl;
    }

    /**
     * 设置同步跳转地址
     *
     * @param returnUrl 同步跳转地址
     */
    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getEpayName() {
        return epayName;
    }

    public void setEpayName(String epayName) {
        this.epayName = epayName;
    }

    public boolean isEpayOrder() {
        return epayOrder;
    }

    public void setEpayOrder(boolean epayOrder) {
        this.epayOrder = epayOrder;
    }

    /**
     * 获取订单状态
     *
     * @return 订单状态，-1 表示过期，0 表示等待支付，1 表示支付成功
     */
    public int getState() {
        return state;
    }

    /**
     * 设置订单状态
     *
     * @param state 订单状态，-1 表示过期，0 表示等待支付，1 表示支付成功
     */
    public void setState(int state) {
        this.state = state;
    }

    /**
     * 获取通用二维码标识
     *
     * @return 通用二维码标识，1 表示通用二维码，0 表示固定转账二维码
     */
    public int getIsAuto() {
        return isAuto;
    }

    /**
     * 设置通用二维码标识
     *
     * @param isAuto 通用二维码标识，1 表示通用二维码，0 表示固定转账二维码
     */
    public void setIsAuto(int isAuto) {
        this.isAuto = isAuto;
    }

    /**
     * 返回订单信息的字符串表示
     * <p>
     * 用于调试和日志记录，包含订单的主要字段信息
     * </p>
     *
     * @return 订单信息的字符串表示
     */
    @Override
    public String toString() {
        return "PayOrder{" +
                "id=" + id +
                ", orderId='" + orderId + '\'' +
                ", createDate=" + createDate +
                ", payDate=" + payDate +
                ", closeDate=" + closeDate +
                ", param='" + param + '\'' +
                ", type=" + type +
                ", price=" + price +
                ", reallyPrice=" + reallyPrice +
                ", notifyUrl='" + notifyUrl + '\'' +
                ", returnUrl='" + returnUrl + '\'' +
                ", state=" + state +
                '}';
    }
}
