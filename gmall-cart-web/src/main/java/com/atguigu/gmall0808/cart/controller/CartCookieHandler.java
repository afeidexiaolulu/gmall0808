package com.atguigu.gmall0808.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0808.bean.CartInfo;
import com.atguigu.gmall0808.bean.SkuInfo;
import com.atguigu.gmall0808.config.CookieUtil;
import com.atguigu.gmall0808.service.ManageService;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Component
public class CartCookieHandler {
    // 定义购物车名称
    private String COOKIECARTNAME = "CART";
    // 设置cookie 过期时间
    private int COOKIE_CART_MAXAGE=7*24*3600;

    @Reference
    private ManageService manageService;

    // 未登录添加购物车到cookie 中的方法
    public void addToCart(HttpServletRequest request, HttpServletResponse response, String skuId, String userId, int skuNum) {

        // 添加商品到购物车！查询cookie 中的所有购物车数据
        String cartJson  = CookieUtil.getCookieValue(request, COOKIECARTNAME, true);
        boolean ifExist=false;
        List<CartInfo> cartInfoList = new ArrayList<>();
        // 将字符串变成{cartInfo集合对象}
        if (cartJson!=null && cartJson.length()>0){
            cartInfoList = JSON.parseArray(cartJson, CartInfo.class);

            if (cartInfoList!=null && cartInfoList.size()>0){
                // 判断添加的商品在购物车中是否存在！如果存在，则数量相加，否则直接添加到购物车cookie！
                for (CartInfo cartInfo : cartInfoList) {
                    // 说明购物车中有该商品
                    if (cartInfo.getSkuId().equals(skuId)){
                        // 数量相加
                        cartInfo.setSkuNum(cartInfo.getSkuNum()+skuNum);
                        ifExist = true;
                        break;
                    }
                }
            }
        }

        // 表示购物车集合中没有该商品
        if (!ifExist){
            // 直接添加到cookie中
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo = new CartInfo();
            cartInfo.setSkuId(skuId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            // 因为没有登录，userId 为null ,所以cookie要与mysql 进行合并的时候，从新将userId赋值
            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(skuNum);
            cartInfoList.add(cartInfo);

        }

        // 将集合的数据放入 cookie 中 将集合转换为字符串
        String cartJsonList = JSON.toJSONString(cartInfoList);
        // 将最新的集合数据放入cookie
        CookieUtil.setCookie(request,response,COOKIECARTNAME,cartJsonList,COOKIE_CART_MAXAGE,true);
    }
    // 获取cookie 中的所有购物车数据
    public List<CartInfo> getCartList(HttpServletRequest request) {
        // 得到购物车字符串
        String cookieValue = CookieUtil.getCookieValue(request, COOKIECARTNAME, true);
        // 将字符串转换为集合对象
        List<CartInfo> cartInfoList = JSON.parseArray(cookieValue, CartInfo.class);
        return cartInfoList;
    }
    // 删除cookie 中的数据
    public void deleteCartCookie(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.deleteCookie(request,response,COOKIECARTNAME);
    }
    // 更改状态
    public void checkCart(HttpServletRequest request, HttpServletResponse response, String skuId, String isChecked) {
        List<CartInfo> cartListCK = getCartList(request);
        // 循环遍历集合数据更改状态
        for (CartInfo cartInfo : cartListCK) {
            if (cartInfo.getSkuId().equals(skuId)){
                cartInfo.setIsChecked(isChecked);
            }
        }
        // 将变更之后的集合数据放入cookie 中
        CookieUtil.setCookie(request,response,COOKIECARTNAME,JSON.toJSONString(cartListCK),COOKIE_CART_MAXAGE,true);

    }
}
