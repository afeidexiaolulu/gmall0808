package com.atguigu.gmall0808.order.task;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0808.bean.OrderInfo;
import com.atguigu.gmall0808.service.OrderService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@EnableScheduling
@Component
public class OrderTask {

    @Reference
    private OrderService orderService;

    // 分时日月周
    // 表示每分钟的第五秒执行！
    @Scheduled(cron = "5 * * * * ?")
    public void test01(){
        System.out.println(new Thread().getName()+"-------------001");
    }
    // 表示每隔五秒执行一次！
    @Scheduled(cron = "0/5 * * * * ?")
    public void test02(){
        System.out.println(new Thread().getName()+"-------------002");
    }

    @Scheduled(cron = "0/20 * * * * ?")
    public void checkOrder(){
       /*
            1.  先查询所有的过期订单
            2.  对每个过期的订单进行处理
            3.  还会需要处理交易记录数据
        */
       // 开始时间
        System.out.println("开始时间");
        long starttime  = System.currentTimeMillis();
        List<OrderInfo> orderInfoList = orderService.getExpiredOrderList();
        for (OrderInfo orderInfo : orderInfoList) {
            // 处理orderInfo
            orderService.execExpiredOrder(orderInfo);
        }
        long costtime =  System.currentTimeMillis()-starttime;
        System.out.println("一共处理"+orderInfoList.size()+"个订单 共消耗"+costtime+"毫秒");
    }
}
