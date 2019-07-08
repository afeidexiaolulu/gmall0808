package com.atguigu.gmall0808.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall0808.bean.OrderInfo;
import com.atguigu.gmall0808.bean.PaymentInfo;
import com.atguigu.gmall0808.bean.enums.PaymentStatus;
import com.atguigu.gmall0808.payment.config.AlipayConfig;
import com.atguigu.gmall0808.service.OrderService;
import com.atguigu.gmall0808.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Values.CHARSET;

@Controller
public class PaymentController {

    @Reference
    private OrderService orderService;

    @Reference
    private PaymentService paymentService;

    @Autowired
    private AlipayClient alipayClient;

    @RequestMapping("index")
    public String index(HttpServletRequest request){
        // 获取orderId
        String orderId = request.getParameter("orderId");
        // 获取总金额
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);

        // 保存orderId
        request.setAttribute("orderId",orderId);
        // 保存总金额
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());
        System.out.println("orderId:"+orderId);
        return  "index";
    }
    // 根据用户选中的支付方式，生成对应的二维码！
    @RequestMapping("alipay/submit")
    @ResponseBody
    public String alipay(HttpServletRequest request,
                         HttpServletResponse response ) throws ServletException, IOException {
        // 保存交易记录 对应的数据库表 paymentInfo
        // 获取orderId
        String orderId = request.getParameter("orderId");
        // 创建一个paymentInfo 对象 paymentInfo 数据应该来源于 orderInfo
        // 通过orderId 查找OrderInfo 信息
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo()); //
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        // 标题
        paymentInfo.setSubject("给小伙伴买火车票回家过年！");
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        paymentInfo.setCreateTime(new Date());
        // 插入数据库
        paymentService.savyPaymentInfo(paymentInfo);

        // 生成二维码
        // 在sdk中 创建AlipayClient 对象
        // AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do", APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE); //获得初始化的AlipayClient
        // AlipayTradePagePayRequest 封装签名参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request
        // 同步回调
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        // 异步回调
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);//在公共参数中设置回跳和通知地址
        // 设置的参数
        /*
            out_trade_no    自动生成一个第三方交易编号
            product_code    销售产品码
            total_amount    总金额
            subject         标题
         */
//        alipayRequest.setBizContent("{" +
//                "    \"out_trade_no\":\"20150320010101001\"," +
//                "    \"product_code\":\"FAST_INSTANT_TRADE_PAY\"," +
//                "    \"total_amount\":88.88," +
//                "    \"subject\":\"Iphone6 16G\"," +
//                "    \"body\":\"Iphone6 16G\"," +
//                "    \"passback_params\":\"merchantBizType%3d3C%26merchantBizNo%3d2016010101111\"," +
//                "    \"extend_params\":{" +
//                "    \"sys_service_provider_id\":\"2088511833207846\"" +
//                "    }"+
//                "  }");//填充业务参数
        // 创建一个map
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",paymentInfo.getOutTradeNo());
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("total_amount",paymentInfo.getTotalAmount());
        map.put("subject",paymentInfo.getSubject());
        // 将map转换为json 字符串
        String mapJson = JSON.toJSONString(map);
        alipayRequest.setBizContent(mapJson);
        String form="";
        try {
            // 执行pageExecute().getBody()
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=utf-8");
//        response.getWriter().write(form);//直接将完整的表单html输出到页面
//        response.getWriter().flush();
//        response.getWriter().close();
        // 正常情况下，支付完成订单的数据应该清空！delete from orderInfo ; 物理删除，逻辑删除！update flag 大数据分析：智能推荐！

        paymentService.sendDelayPaymentResult(paymentInfo.getOutTradeNo(),15,3);
        return form;
    }

    @RequestMapping("alipay/callback/return")
    public String callback(){
        return "redirect:"+AlipayConfig.return_order_url;
    }
    // 告诉商家用户支付结果{success,fail}
    // @RequestParam 将异步通知中收到的所有参数都存放到map中
    @RequestMapping("alipay/callback/notify")
    public String callbackNotify(@RequestParam Map<String,String> paramMap) throws AlipayApiException {
        //  更改交易记录状态    ,更新PaymentStatus.PAID
        PaymentInfo paymentInfoUpd = new PaymentInfo();
        boolean flg = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type); //调用SDK验证签名
        if(flg){
            // 验签：支付宝中有交易记录
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            //  获取 trade_status 交易状态
            String trade_status = paramMap.get("trade_status");
            // 交易成功！
            if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)){
                // 如果交易记录中paymentInfo paymentStatus 状态为 CLOSE ,或者是 PAID, 此时返回的是success，还是fail？
                // 通过out_trade_no 查询交易状态
                String out_trade_no = paramMap.get("out_trade_no");
                 // select * from payment_info where out_trade_no = ?
                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setOutTradeNo(out_trade_no);
                PaymentInfo paymentInfoQuery = paymentService.getPaymentInfo(paymentInfo);

                if (paymentInfoQuery.getPaymentStatus()==PaymentStatus.PAID || paymentInfoQuery.getPaymentStatus()==PaymentStatus.ClOSED){
                    return "fail";
                }
                paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID);
                paymentInfoUpd.setCallbackTime(new Date());
                paymentInfoUpd.setSubject(paramMap.toString());

                paymentService.updatePaymentInfo(out_trade_no,paymentInfoUpd);
                // 发送消息
                paymentService.sendPaymentResult(paymentInfoUpd,"success");
                return "success";
            }

        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            paymentService.sendPaymentResult(paymentInfoUpd,"fail");
            return "fail";
        }
        return "fail";
    }

    @RequestMapping("sendPaymentResult")
    @ResponseBody
    public String sendPaymentResult(PaymentInfo paymentInfo,String result){
        paymentService.sendPaymentResult(paymentInfo,result);
        return "sendPaymentResult";
    }
    // 根据订单编号orderId
    @RequestMapping("queryPaymentResult")
    @ResponseBody
    public String queryPaymentResult(String orderId){
        // 根据orderId 查询到out_trade_no数据
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        PaymentInfo paymentInfoQuery = paymentService.getPaymentInfo(paymentInfo);
        boolean flag = paymentService.checkPayment(paymentInfoQuery);
        return flag+"";
    }




}
