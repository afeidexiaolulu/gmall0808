package com.atguigu.gmall0808.service;

import com.atguigu.gmall0808.bean.PaymentInfo;

public interface PaymentService {
    /**
     *  保存方法
     * @param paymentInfo
     */
    void  savyPaymentInfo(PaymentInfo paymentInfo);

    /**
     * 通过paymentInfo 对象中的out_trade_no 查询PaymentInfo
     * @param paymentInfo
     * @return
     */
    PaymentInfo getPaymentInfo(PaymentInfo paymentInfo);

    /**
     *  根据out_trade_no 更新数据
     * @param out_trade_no
     * @param paymentInfoUpd
     */
    void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUpd);

    /**
     * 发送orderId ，对应的支付结果！
     * @param paymentInfo
     * @param result
     */
    void sendPaymentResult(PaymentInfo paymentInfo,String result);

    /**
     * 主要根据out_trade_no 查询支付结果
     * @param paymentInfoQuery
     * @return
     */
    boolean checkPayment(PaymentInfo paymentInfoQuery);

    /**
     *
     * @param outTradeNo 交易编号
     * @param delaySec  延迟时间
     * @param checkCount 检查次数
     */
    void sendDelayPaymentResult(String outTradeNo,int delaySec ,int checkCount);

    /**
     *  根据orderId 关闭过期的交易记录数据
     * @param orderId
     */
    void closePayment(String orderId);
}
