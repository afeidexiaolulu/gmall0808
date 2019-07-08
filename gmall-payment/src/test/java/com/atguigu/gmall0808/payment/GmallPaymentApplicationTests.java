package com.atguigu.gmall0808.payment;

import com.atguigu.gmall0808.config.ActiveMQUtil;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.jms.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallPaymentApplicationTests {

	@Autowired
	private ActiveMQUtil activeMQUtil;
	@Test
	public void contextLoads() {
	}

	@Test
	public void testActiveMq() throws JMSException {
		// 创建一个消息队列工厂
		// ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("tcp://192.168.67.211:61616");
		// 创建连接
		Connection connection = activeMQUtil.getConnection();
		// 打开连接
		connection.start();
		// 创建session 对象 第一个参数boolean类型 ，表示事务是否开启 ,第二个参数需要根据第一个参数的设定而选择
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
//        Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
		// 创建队列
		Queue queue = session.createQueue("atguiguTest");
		// 创建一个消息提供者
		MessageProducer producer = session.createProducer(queue);
		// 创建一个消息对象
		ActiveMQTextMessage textMessage = new ActiveMQTextMessage();
		// 设置发送的内容
		textMessage.setText("你好，新年好！？");
		// 提交
//        session.commit();
		// 发送消息
		producer.send(textMessage);
		// 关闭
		producer.close();
		session.close();
		connection.close();
	}
}

