package com.atguigu.gmall0808.order.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0808.bean.enums.ProcessStatus;
import com.atguigu.gmall0808.service.OrderService;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;

@Component
public class OrderConsumer {

    @Reference
    private OrderService orderService;

    // 监听消息工厂 得到消息中的内容 {result=success, orderId=143}
    @JmsListener(destination = "PAYMENT_RESULT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerPaymentResult(ActiveMQMapMessage mapMessage) throws JMSException {
        // 取得消息中的数据
        String orderId = mapMessage.getString("orderId");
        String result = mapMessage.getString("result");

        // 根据result 的结果来修改订单的状态
        if ("success".equals(result)){
            // 更新订单为PAID
            // 更加orderId
            orderService.updateOrderStatus(orderId, ProcessStatus.PAID);
            // 发送消息给库存系统
            orderService.sendOrderStatus(orderId);
            // 更改订单的状态
            orderService.updateOrderStatus(orderId,ProcessStatus.NOTIFIED_WARE);
        }else {
            // 更新订单为UNPAID
        }
    }


    // 监听消息工厂 得到消息中的内容 {result=success, orderId=143}
    @JmsListener(destination = "SKU_DEDUCT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(ActiveMQMapMessage mapMessage) throws JMSException {
        // 取得消息中的数据
        String orderId = mapMessage.getString("orderId");
        String status = mapMessage.getString("status");

        // 根据result 的结果来修改订单的状态
        if ("DEDUCTED".equals(status)){
            // 更新订单为PAID
            // 更加orderId
            orderService.updateOrderStatus(orderId, ProcessStatus.DELEVERED);
        }
    }


}
