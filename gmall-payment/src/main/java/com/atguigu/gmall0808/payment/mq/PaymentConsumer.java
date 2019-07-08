package com.atguigu.gmall0808.payment.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0808.bean.PaymentInfo;
import com.atguigu.gmall0808.service.PaymentService;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;

@Component
public class PaymentConsumer {

    @Reference
    private PaymentService paymentService;

    @JmsListener(destination = "PAYMENT_RESULT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerPaymentResultCheck(ActiveMQMapMessage activeMQMapMessage) throws JMSException {
            // 取得消息队列中的数据
        String outTradeNo = activeMQMapMessage.getString("outTradeNo");
        int delaySec = activeMQMapMessage.getInt("delaySec");
        int checkCount = activeMQMapMessage.getInt("checkCount");

        // 创建一个paymentInfo对象
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(outTradeNo);
        // 需要调用checkPayment
        boolean result = paymentService.checkPayment(paymentInfo);
        System.out.println("检查结果："+result);
        // 如果result = true ，说明已经付款，false 说明没有付款！根据checkCount 次数来判断是否继续发送消息！
        if (!result&&checkCount>0){
            // 继续发送消息队列
            System.out.println("checkCount:"+checkCount);
            paymentService.sendDelayPaymentResult(outTradeNo,delaySec,checkCount-1);
        }

    }

}
