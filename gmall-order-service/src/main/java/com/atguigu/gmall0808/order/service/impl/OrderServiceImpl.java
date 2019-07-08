package com.atguigu.gmall0808.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0808.bean.OrderDetail;
import com.atguigu.gmall0808.bean.OrderInfo;
import com.atguigu.gmall0808.bean.enums.OrderStatus;
import com.atguigu.gmall0808.bean.enums.ProcessStatus;
import com.atguigu.gmall0808.config.ActiveMQUtil;
import com.atguigu.gmall0808.config.RedisUtil;
import com.atguigu.gmall0808.order.mapper.OrderDetailMapper;
import com.atguigu.gmall0808.order.mapper.OrderInfoMapper;
import com.atguigu.gmall0808.service.OrderService;
import com.atguigu.gmall0808.service.PaymentService;
import com.atguigu.gmall0808.util.HttpClientUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import javax.jms.Queue;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService{


    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisUtil redisUtil;
    
    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Reference
    private PaymentService paymentService;
    /**
     * 保存订单方法
     *
     * @param orderInfo
     * @return 订单Id
     */
    @Override
    public String saveOrder(OrderInfo orderInfo) {
        // 创建时间没有，过期时间也没有
        orderInfo.setCreateTime(new Date());
        // 当前日期+1day
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        // 第三方交易编号
        String outTradeNo="ATGUIGU"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        // 保存订单！
        orderInfoMapper.insertSelective(orderInfo);
        // 订单明细保存到数据库
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (orderDetailList!=null && orderDetailList.size()>0){
            // 循环插入数据
            for (OrderDetail orderDetail : orderDetailList) {
                orderDetail.setOrderId(orderInfo.getId());
                orderDetailMapper.insertSelective(orderDetail);
            }
        }
        return orderInfo.getId();
    }

    /**
     * 生成一个字符串
     *
     * @param userId 将字符串保存到redis 时，需要userId作为key
     * @return
     */
    @Override
    public String getTradeNo(String userId) {
        Jedis jedis = redisUtil.getJedis();
        // 定义key
        String tradeNoKey="user:"+userId+":tradeCode";
        // 随机生成一个tradeCode
        String tradeCode = UUID.randomUUID().toString();
        // 放入tradeCode
        jedis.setex(tradeNoKey,10*60,tradeCode);
        jedis.close();
        return tradeCode;
    }

    /**
     * tradeCode 比较
     *
     * @param userId
     * @param tradeCodeNo
     * @return
     */
    @Override
    public boolean checkTradeCode(String userId, String tradeCodeNo) {
        // 获取redis 中tradeCode
        Jedis jedis = redisUtil.getJedis();
        // 定义key
        String tradeNoKey="user:"+userId+":tradeCode";
        String tradeCode  = jedis.get(tradeNoKey);
        if (tradeCode!=null && tradeCode.length()>0){
            if (tradeCode.equals(tradeCodeNo)){
                return true;
            } else{
                return false;
            }
        }
        return false;
    }

    /**
     * 删除redis 中的tradeCode
     * @param userId
     */
    @Override
    public void delTradeCode(String userId) {
        Jedis jedis = redisUtil.getJedis();
        // 定义key
        String tradeNoKey="user:"+userId+":tradeCode";
        // 删除数据
        jedis.del(tradeNoKey);
        jedis.close();
    }

    /**
     * 查询库存系统
     *
     * @param skuId
     * @param skuNum
     * @return
     */
    @Override
    public boolean checkStock(String skuId, Integer skuNum) {

        // 调用库存系统接口 "http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);
        if ("1".equals(result)){
            return true;
        }else {
            return false;
        }
    }

    /**
     * 根据订单Id 查询订单信息
     *
     * @param orderId
     * @return
     */
    @Override
    public OrderInfo getOrderInfo(String orderId) {
        // 返回orderInfo
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);
        // 查询orderDetail 信息
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.select(orderDetail);
        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;
    }

    /**
     * 根据订单编号更新订单的状态
     *
     * @param orderId
     * @param processStatus
     */
    @Override
    public void updateOrderStatus(String orderId, ProcessStatus processStatus) {
        // update orderInfo set orderStatus = PAID and processStatus = PAID where id = orderId
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus);
        orderInfo.setOrderStatus(processStatus.getOrderStatus());
//        orderInfo.setOrderStatus(OrderStatus.PAID);
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }

    /**
     * 根据订单Id 发送消息给库存！
     *
     * @param orderId
     */
    @Override
    public void sendOrderStatus(String orderId) {
        // 得到连接
        Connection connection = activeMQUtil.getConnection();
        // 根据orderId 将orderInfo的数据转换为json字符串
        String orderJson = initWareOrder(orderId);
        // 打开连接
        try {
            connection.start();
            // 创建session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            // 创建队列
            Queue order_result_queue = session.createQueue("ORDER_RESULT_QUEUE");
            // 创建消息提供者
            MessageProducer producer = session.createProducer(order_result_queue);
            ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
            activeMQTextMessage.setText(orderJson);
            // 准备发送消息
            producer.send(activeMQTextMessage);
            // 提交
            session.commit();
            // 关闭
            producer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询所有过期订单
     *
     * @return
     */
    @Override
    public List<OrderInfo> getExpiredOrderList() {
        // 过期时间<当前时间 and 支付方式为UNPAID
        Example example = new Example(OrderInfo.class);
        example.createCriteria().andLessThan("expireTime",new Date()).andEqualTo("processStatus",ProcessStatus.UNPAID);
        List<OrderInfo> orderInfoList = orderInfoMapper.selectByExample(example);
        return orderInfoList;
    }

    /**
     * 处理过期订单
     *
     * @param orderInfo
     */

    @Override
    @Async
    public void execExpiredOrder(OrderInfo orderInfo) {
        // 更新订单状态
        updateOrderStatus(orderInfo.getId(),ProcessStatus.CLOSED);
        // 如果出现未付款的情况，paymentInfo记录表中也有数据。应该将paymentInfo 中的数据进行更改
        paymentService.closePayment(orderInfo.getId());
    }

    private String initWareOrder(String orderId) {
        // 通过orderId 查询orderInfo
        OrderInfo orderInfo = getOrderInfo(orderId);
        // 将orderInfo 转化为map
        Map map = initWareOrder(orderInfo);
        return JSON.toJSONString(map);
    }

    public Map initWareOrder(OrderInfo orderInfo) {
        // 创建map集合
        HashMap<String, Object> map = new HashMap<>();
        map.put("orderId",orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        map.put("orderBody","测试数据");
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        map.put("paymentWay","2");
        map.put("wareId",orderInfo.getWareId()); // 拆单使用
        // 在声明一个集合存储orderDetailList对象
        ArrayList<Map> newOrderDetailList = new ArrayList<>();
        // 操作订单明细
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            HashMap<String, Object> detailMap = new HashMap<>();
            detailMap.put("skuId",orderDetail.getSkuId());
            detailMap.put("skuName",orderDetail.getSkuName());
            detailMap.put("skuNum",orderDetail.getSkuNum());
            newOrderDetailList.add(detailMap);
        }
        map.put("details",newOrderDetailList);
        return map;
    }

    /**
     * 拆单得到子订单集合！
     *
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    @Override
    public List<OrderInfo> splitOrder(String orderId, String wareSkuMap) {
        List<OrderInfo> subOrderInfoList = new ArrayList<>();
        // 先获取原始订单
        OrderInfo orderInfoOrigin = getOrderInfo(orderId);
        // 如何判断是否需要拆单  [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
        List<Map> mapList = JSON.parseArray(wareSkuMap, Map.class);
        // 循环集合
        for (Map map : mapList) {
            // 得到仓库的Id
            String wareId = (String) map.get("wareId");
            //  得到skuIds
            List<String> skuIds = (List<String>) map.get("skuIds");
            // 创建一个新的子订单对象
            OrderInfo subOrderInfo  = new OrderInfo();
            // 属性拷贝 {从原始订单想新的子订单拷贝}
            BeanUtils.copyProperties(orderInfoOrigin,subOrderInfo);
            // 需要考虑的问题是，id不能直接拷贝，需要置空
            subOrderInfo.setId(null);
            // 父订单的Id
            subOrderInfo.setParentOrderId(orderInfoOrigin.getId());
            // 声明一个新的子订单明细集合
            ArrayList<OrderDetail> subOrderDetailList   = new ArrayList<>();
            // 处理子订单的明细 1. 得到原始订单明细
            List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
            for (OrderDetail orderDetail : orderDetailList) {
                // 需要跟skuIds 中的数据进行匹配
                for (String skuId : skuIds) {
                    if (orderDetail.getSkuId().equals(skuId)){
                        // 设置订单明细的订单Id
                        // orderDetail.setOrderId(subOrderInfo.getId());
                        // 使订单明细主键自增
                        orderDetail.setId(null);
                        // 将新的子订单明细，添加到集合中
                        subOrderDetailList.add(orderDetail);
                    }
                }
            }
            // 将订单明细集合放入新的子订单中！
            subOrderInfo.setOrderDetailList(subOrderDetailList);

            // 金额不能拷贝
            subOrderInfo.sumTotalAmount();

            // 设置一下仓库的Id
            subOrderInfo.setWareId(wareId);

            // 需要将新的子订单保存到数据库
            saveOrder(subOrderInfo);

            // 添加新的子订单
            subOrderInfoList.add(subOrderInfo);

        }
        // 更新状态
        updateOrderStatus(orderId,ProcessStatus.SPLIT);
        return subOrderInfoList;
    }


}
