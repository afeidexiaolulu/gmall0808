package com.atguigu.gmall0808.service;

import com.atguigu.gmall0808.bean.OrderInfo;
import com.atguigu.gmall0808.bean.enums.ProcessStatus;

import java.util.List;
import java.util.Map;

public interface OrderService {
    /**
     * 保存订单方法
     * @param orderInfo
     * @return 订单Id
     */
    String  saveOrder(OrderInfo orderInfo);

    /**
     *  生成一个字符串
     * @param userId 将字符串保存到redis 时，需要userId作为key
     * @return
     */
    String getTradeNo(String userId);

    /**
     * tradeCode 比较
     * @param userId
     * @param tradeCodeNo
     * @return
     */
    boolean checkTradeCode(String userId,String tradeCodeNo);

    /**
     * 删除redis 中的tradeCode
     * @param userId
     */
    void  delTradeCode(String userId);

    /**
     * 查询库存系统
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(String skuId, Integer skuNum);

    /**
     * 根据订单Id 查询订单信息
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(String orderId);

    /**
     * 根据订单编号更新订单的状态
     * @param orderId
     * @param processStatus
     */
    void updateOrderStatus(String orderId, ProcessStatus processStatus);

    /**
     * 根据订单Id 发送消息给库存！
     * @param orderId
     */
    void sendOrderStatus(String orderId);

    /**
     * 查询所有过期订单
     * @return
     */
    List<OrderInfo> getExpiredOrderList();

    /**
     * 处理过期订单
     * @param orderInfo
     */
    void execExpiredOrder(OrderInfo orderInfo);

    /**
     * 将orderInfo 转换为map
     * @param orderInfo
     * @return
     */
    Map initWareOrder(OrderInfo orderInfo);

    /**
     * 拆单得到子订单集合！
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    List<OrderInfo> splitOrder(String orderId, String wareSkuMap);
}
