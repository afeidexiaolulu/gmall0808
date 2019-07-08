package com.atguigu.gmall0808.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0808.bean.CartInfo;
import com.atguigu.gmall0808.bean.SkuInfo;
import com.atguigu.gmall0808.config.CookieUtil;
import com.atguigu.gmall0808.config.LoginRequire;
import com.atguigu.gmall0808.service.CartService;
import com.atguigu.gmall0808.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Controller
public class CartController {

    @Reference
    private CartService cartService;

    @Autowired
    private CartCookieHandler cartCookieHandler;

    @Reference
    private ManageService manageService;


    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response){
        // 取得skuNum，skuId
        String skuNum = request.getParameter("skuNum");
        String skuId = request.getParameter("skuId");
        // 根据userId 判断当前用户是否登录
//        request.getParameter("userId"); // 取得控制器？后面的值
        String userId = (String) request.getAttribute("userId");
        // 调用服务层
        if (userId!=null){
            // 此时处于登录状态
            cartService.addToCart(skuId,userId,Integer.parseInt(skuNum));
        }else {
            // 未登录状态，放入cookie 中 skuId,skuNum.
            cartCookieHandler.addToCart(request,response,skuId,userId,Integer.parseInt(skuNum));
        }

        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum",skuNum);
        return "success";
    }

    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request,HttpServletResponse response){
        List<CartInfo> cartInfoList = new ArrayList<>();

        // 得到用户Id
        String userId = (String) request.getAttribute("userId");
        if (userId!=null){
            // 登录以后进行合并购物车{cookie 和 redis{mysql} 进行合并}
            // 取得cookie 的购物车数据
            List<CartInfo> cartListCK = cartCookieHandler.getCartList(request);
            if (cartListCK!=null && cartListCK.size()>0){
                cartInfoList = cartService.mergeToCartList(cartListCK,userId);
                // 将cookie 中的数据进行删除！
                cartCookieHandler.deleteCartCookie(request,response);
            }else {
                // 登录了，从redis，或者mysql
                cartInfoList = cartService.getCartList(userId);
                // 将集合数据保存到页面进行渲染
            }
        }else {
            // 从cookie 中查询
            cartInfoList =  cartCookieHandler.getCartList(request);
        }
        request.setAttribute("cartInfoList",cartInfoList);
        return "cartList";
    }

    @RequestMapping("checkCart")
    @ResponseBody
    @LoginRequire(autoRedirect = false)
    public void checkCart(HttpServletRequest request,HttpServletResponse response){
        // 获取userId
        String userId = (String) request.getAttribute("userId");
        String isChecked = request.getParameter("isChecked");
        String skuId = request.getParameter("skuId");
        if (userId!=null){
            // 调用服务层的方法 更改商品选中状态
            cartService.checkCart(skuId,isChecked,userId);
        }else {
            // 更改状态？
            cartCookieHandler.checkCart(request,response,skuId,isChecked);
        }
    }

    @RequestMapping("toTrade")
    @LoginRequire(autoRedirect = true)
    public String toTrade(HttpServletRequest request,HttpServletResponse response){
        // 获取userId
        String userId = (String) request.getAttribute("userId");
        // 合并被选中的商品合并！
        // 取得cookie中的数据
        List<CartInfo> cartListCK = cartCookieHandler.getCartList(request);
        if (cartListCK!=null && cartListCK.size()>0){
            // 进行合并
            cartService.mergeToCartList(cartListCK,userId);
            // 删除cookie 中的数据
            cartCookieHandler.deleteCartCookie(request,response);
        }
        // 订单页面
        return "redirect://order.gmall.com/trade";
    }
}
