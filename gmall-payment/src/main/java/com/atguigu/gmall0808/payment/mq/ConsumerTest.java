package com.atguigu.gmall0808.payment.mq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

public class ConsumerTest {
    public static void main(String[] args) throws JMSException {
        // 创建一个消息队列工厂
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(ActiveMQConnectionFactory.DEFAULT_USER,ActiveMQConnectionFactory.DEFAULT_PASSWORD,"tcp://192.168.67.211:61616");
        // 创建连接
        Connection connection = activeMQConnectionFactory.createConnection();
        // 打开连接
        connection.start();
        // 创建session
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        // 创建队列
        Queue queue = session.createQueue("atguigu");
        // 创建消费者
        MessageConsumer consumer = session.createConsumer(queue);
        // 消费消息
        consumer.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                if (message instanceof ActiveMQTextMessage){
                    try {
                        String text = ((ActiveMQTextMessage) message).getText();
                        System.out.println("text:"+text);
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            }
        });


    }
}
