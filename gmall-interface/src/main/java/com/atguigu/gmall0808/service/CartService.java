package com.atguigu.gmall0808.service;

import com.atguigu.gmall0808.bean.CartInfo;
import java.util.List;

public interface CartService  {
    // 添加购物车 skuId,userId,skuNum
    void  addToCart(String skuId,String userId,Integer skuNum);

    /**
     * 根据用户Id查询购物车数据
     * @param userId
     * @return
     */
    List<CartInfo> getCartList(String userId);

    /**
     * 合并购物车
     * @param cartListCK
     * @param userId
     * @return
     */
    List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId);

    /**
     * 根据skuId 变更商品的选中状态！
     * @param skuId
     * @param isChecked
     * @param userId
     */
    void checkCart(String skuId, String isChecked, String userId);

    /**
     * 根据userId查询被选中的商品
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);

    /**
     * 根据userId将最新的购物车集合查询出来，并放入缓存
     * @param userId
     * @return
     */
    List<CartInfo> loadCartCache(String userId);
}
