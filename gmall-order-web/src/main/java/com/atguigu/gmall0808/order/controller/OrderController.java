package com.atguigu.gmall0808.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0808.bean.*;
import com.atguigu.gmall0808.bean.enums.OrderStatus;
import com.atguigu.gmall0808.bean.enums.ProcessStatus;
import com.atguigu.gmall0808.config.LoginRequire;
import com.atguigu.gmall0808.service.CartService;
import com.atguigu.gmall0808.service.ManageService;
import com.atguigu.gmall0808.service.OrderService;
import com.atguigu.gmall0808.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class OrderController {

//    @Autowired
    @Reference
    private UserInfoService userInfoService;

    @Reference
    private CartService cartService;

    @Reference
    private OrderService orderService;

    @Reference
    private ManageService manageService;
    // 调用方法 根据userId 查询用户列表
//    @RequestMapping("trade")
//    @ResponseBody
//    public List<UserAddress> trade(String userId){
//        List<UserAddress> userAddressByUserId = userInfoService.findUserAddressByUserId(userId);
//        return  userAddressByUserId;
//    }
    @RequestMapping("trade")
    @LoginRequire
    public String trade(HttpServletRequest request){
        // 获取用户Id
        String userId = (String) request.getAttribute("userId");
        // 用户收货地址信息
        List<UserAddress> userAddressList = userInfoService.findUserAddressByUserId(userId);
        // 声明一个集合来存放OrderDetail
        ArrayList<OrderDetail> orderDetailList = new ArrayList<>();
        // 显示送货清单 { 订单明细orderDetail == 购物车中[被选中的商品]}
        List<CartInfo> cartInfoList = cartService.getCartCheckedList(userId);
        // 循环cartInfoList
        for (CartInfo cartInfo : cartInfoList) {
            // 将cartInfo 中的数据赋值给orderDetail
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setOrderPrice(cartInfo.getCartPrice());
            // 将orderDetail 添加到集合中即可！
            orderDetailList.add(orderDetail);
        }
        // 计算总价钱
        OrderInfo orderInfo = new OrderInfo();
        // 将订单明细放入订单中
        orderInfo.setOrderDetailList(orderDetailList);
        // 调用方法来计算总价
        orderInfo.sumTotalAmount();

        // 保存总价格：
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());

        // 保存订单详细集合，给页面展示
        request.setAttribute("orderDetailList",orderDetailList);

        // 保存集合数据，给前台渲染
        request.setAttribute("userAddressList",userAddressList);
        // 生成tradeCode
        String tradeNo = orderService.getTradeNo(userId);
        request.setAttribute("tradeNo",tradeNo);


        return "trade";
    }
    @RequestMapping("submitOrder")
    @LoginRequire
    public String submitOrder(OrderInfo orderInfo,HttpServletRequest request){
        //  将数据进行保存！
        String userId = (String) request.getAttribute("userId");
        orderInfo.setUserId(userId);
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        // 先算总价，并将总价赋给totalAmount
        orderInfo.sumTotalAmount();
        orderInfo.setTotalAmount(orderInfo.getTotalAmount());

        // 判断tradeCode
        String tradeNo = request.getParameter("tradeNo");
        // 调用比较方法
        boolean result = orderService.checkTradeCode(userId, tradeNo);
        if (!result){
            request.setAttribute("errMsg","表单不能重复提交，请刷新！");
            return "tradeFail";
        }

        // 检查库存
        for (OrderDetail orderDetail : orderInfo.getOrderDetailList()) {
            boolean flag = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
            if (!flag){
                request.setAttribute("errMsg","库存不足，请重新下订单！");
                return "tradeFail";
            }
            SkuInfo skuInfo = manageService.getSkuInfo(orderDetail.getSkuId());
            // 验价：订单的价格，与skuInfo.price 的价格比较，如果相同，则可以下订单，如果不同，则提示信息！
//            if (!orderDetail.getOrderPrice().equals(skuInfo.price)){
//                request.setAttribute("errMsg","库存不足，请重新下订单！");
//                return "tradeFail";
//            }
//            不使用equals！
//            if (!orderDetail.getOrderPrice().equals(skuInfo.getPrice())){
//                request.setAttribute("errMsg","库存不足，请重新下订单！");
//                cartService.loadCartCache(userId);
//                return "tradeFail";
//            }
            int res = orderDetail.getOrderPrice().compareTo(skuInfo.getPrice());
            if(res!=0){
                request.setAttribute("errMsg","库存不足，请重新下订单！");
                cartService.loadCartCache(userId);
                return "tradeFail";
            }
        }
        
        String orderId = orderService.saveOrder(orderInfo);

        // 调用删除方法
        orderService.delTradeCode(userId);
        return "redirect://payment.gmall.com/index?orderId="+orderId;
    }

    @RequestMapping("orderSplit")
    @ResponseBody
    public String orderSplit(HttpServletRequest request){
        // 订单id
        String orderId = request.getParameter("orderId");
        // [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
        String wareSkuMap = request.getParameter("wareSkuMap");
        // 声明一个list集合来存储map
        ArrayList<Map> wareMapList = new ArrayList<>();
        // 得到子订单的集合
        List<OrderInfo> orderInfoList = orderService.splitOrder(orderId,wareSkuMap);
        for (OrderInfo orderInfo : orderInfoList) {
            // 将orderInfo 转换为map
            Map map = orderService.initWareOrder(orderInfo);
            wareMapList.add(map);
        }
        return JSON.toJSONString(wareMapList);
    }

}
