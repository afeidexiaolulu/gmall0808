package com.atguigu.gmall0808.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0808.bean.CartInfo;
import com.atguigu.gmall0808.bean.SkuInfo;
import com.atguigu.gmall0808.cart.constant.CartConst;
import com.atguigu.gmall0808.cart.mapper.CartInfoMapper;
import com.atguigu.gmall0808.config.RedisUtil;
import com.atguigu.gmall0808.service.CartService;
import com.atguigu.gmall0808.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.sql.Ref;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Reference
    private ManageService manageService;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        /*
            添加购物车
            1.  先判断购物车中是否已经存在该商品 ，则数量+skuNum ，更新数据库
            2.  如果购物车中不存在该商品，则直接添加到mysql
            3.  无论是更新，还是添加，都需要保存到redis
          */
        CartInfo cartInfo = new CartInfo();
        cartInfo.setSkuId(skuId);
        cartInfo.setUserId(userId);
        // select * from cartInfo where skuId = ? and userId = ? ,
        // 查询出来的结果没有skuPrice
        CartInfo cartInfoExist  = cartInfoMapper.selectOne(cartInfo);
        if (cartInfoExist!=null){
            // 说明购物车中有该商品
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            // 将skuPrice 添加到redis
            cartInfoExist.setSkuPrice(cartInfoExist.getCartPrice());
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);
            // redis
        }else {
            // 说明购物车没有该商品，则直接添加到数据库
            // 查询要添加商品的信息
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo1 = new CartInfo();
            cartInfo1.setSkuId(skuId);
            cartInfo1.setSkuNum(skuNum);
            cartInfo1.setUserId(userId);
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setSkuName(skuInfo.getSkuName());
            cartInfo1.setSkuPrice(skuInfo.getPrice());
            // 写入数据库
            cartInfoMapper.insertSelective(cartInfo1);
            // 如果购物车中没有要添加的商品，则cartInfoExist为null，
            cartInfoExist = cartInfo1;
            // redis
        }

        //  获取jedis
        Jedis jedis = redisUtil.getJedis();
        // 放入redis ，必须定义key ！  存储用户的key user:userId:info
        // 使用hash jedis.hset(key,field,value)
        // key = user:userId:cart
        // field = skuId
        // value = cartInfo 对象
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        // 将数据放入redis
        jedis.hset(cartKey,skuId, JSON.toJSONString(cartInfoExist));
        //  做一个购物车的过期时间！ 大数据{} 根用户的过期时间保持一致！
        //  先得到用户的过期时间
        //  先获取用户key
        String userKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USERINFOKEY_SUFFIX;
        Long ttl = jedis.ttl(userKey);
        // 设置过期时间
        jedis.expire(cartKey,ttl.intValue());
        //  关闭redis！
        jedis.close();



    }

    /**
     * 根据用户Id查询购物车数据
     *
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> getCartList(String userId) {
        /*
            1.  先看redis 中是否能取得数据
            2.  true: 直接返回
            3.  fasle: 从数据库查找
         */
        //  获取jedis
        Jedis jedis = redisUtil.getJedis();
        //  定义key user:userId:cart
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        // 准备根据key 取得数据
        List<String> stringList = jedis.hvals(userCartKey);
        if (stringList!=null && stringList.size()>0){
            List<CartInfo> cartInfoList = new ArrayList<>();
            // 该字符串集合中每个对象都是一个cartInfo
            for (String cartInfoStr : stringList) {
                CartInfo cartInfo = JSON.parseObject(cartInfoStr, CartInfo.class);
                cartInfoList.add(cartInfo);
            }
            // 查询之后的数据应该进行排序：按照id倒序 排序 {按照添加时间进行排序！}
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    // string 类型排序 compareTo();
                    return o1.getId().compareTo(o2.getId());
                }
            });


            return  cartInfoList;
        }else {
            // redis 中没有数据 从mysql 查询，并放入redis 中！
            List<CartInfo> cartInfoList = loadCartCache(userId);
            return  cartInfoList;
        }
    }

    /**
     * 合并购物车
     *
     * @param cartListCK
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId) {
        // cookie 与 redis{mysql} 进行合并
        // 获取redis，mysql 数据 表示查询实时价格
        List<CartInfo> cartInfoListDB  = cartInfoMapper.selectCartListWithCurPrice(userId);

        if (cartInfoListDB!=null && cartInfoListDB.size()>0){
            // 如果cookie 与 redis，mysql 购物车中的skuId 相同，则数量相加，否则，直接插入数据库
            for (CartInfo cartInfoCK : cartListCK) {
                // 定义一个表示
                boolean isMatch = false;
                for (CartInfo cartInfoDB : cartInfoListDB) {
                    if (cartInfoDB.getSkuId().equals(cartInfoCK.getSkuId())){
                        // 数量相加
                        cartInfoDB.setSkuNum(cartInfoDB.getSkuNum()+cartInfoCK.getSkuNum());
                        // 更新数据库
                        cartInfoMapper.updateByPrimaryKeySelective(cartInfoDB);
                        isMatch=true;
                    }
                }
                // 表示没有匹配上
                if (!isMatch){
                    // 直接将cookie的数据插入数据库
                    cartInfoCK.setUserId(userId);
                    cartInfoMapper.insertSelective(cartInfoCK);
                }
            }
            // 因为，我们需要将同一个用户，合并之后的所有数据都查询出来，并放入redis，才能返回！
            List<CartInfo> cartInfoList = loadCartCache(userId);
            // 做商品被选中时，合并 状态为“1”
            for (CartInfo cartInfoDB : cartInfoList) {
                for (CartInfo infoCK : cartListCK) {
                    if (cartInfoDB.getSkuId().equals(infoCK.getSkuId())){
                        if ("1".equals(infoCK.getIsChecked())){
                            // 更改cartInfoDB的状态为1
                            cartInfoDB.setIsChecked(infoCK.getIsChecked());
                            // 从新加载数据
                            checkCart(cartInfoDB.getSkuId(),infoCK.getIsChecked(),userId);
                        }
                    }

                }
            }



            return cartInfoList;
        }


        return null;
    }

    /**
     * 根据skuId 变更商品的选中状态！
     *
     * @param skuId
     * @param isChecked
     * @param userId
     */
    @Override
    public void checkCart(String skuId, String isChecked, String userId) {
        // 将原来的数据进行更新，先获取user:userId:cart key所对应的数据
        Jedis jedis = redisUtil.getJedis();
        // 定义一个key
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        // 获取数据
        String cartJson = jedis.hget(userCartKey, skuId);
        // 将字符串变成对象
        CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
        // 修改原始数据的状态！
        cartInfo.setIsChecked(isChecked);
        //  将最新数据保存到集合中
        jedis.hset(userCartKey,skuId,JSON.toJSONString(cartInfo));

        // 将被选中的商品添加到新的key中保存起来 user:userId:checked
        String userCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
        if ("1".equals(isChecked)){
            // 添加到redis
            jedis.hset(userCheckedKey,skuId,JSON.toJSONString(cartInfo));
        }else {
            jedis.hdel(userCheckedKey,skuId);
        }

        jedis.close();

    }

    /**
     * 根据userId查询被选中的商品
     *
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        //  获取jedis
        Jedis jedis = redisUtil.getJedis();
        // 定义key user:userId:checked
        String userCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
        // 取得所有数据
        List<String> cartCheckedList  = jedis.hvals(userCheckedKey);
        // 循环集合中的字符串，每个字符串都代表一个cartInfo 对象
        for (String cartJson : cartCheckedList) {
            // cartJson 转换为对象
            CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
            cartInfoList.add(cartInfo);
        }
        jedis.close();
        return cartInfoList;
    }

    /**
     *  查询数据库，放入redis！
     * @param userId
     * @return
     */
    public List<CartInfo> loadCartCache(String userId) {
        // 显示购物车中价格，应该是实时价格{skuInfo.price} skuInfo, cartInfo
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        // 判断cartInfoList
        if (cartInfoList==null || cartInfoList.size()==0){
            return null;
        }
        // 将数据放入redis
        Jedis jedis = redisUtil.getJedis();
        //  定义key
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        // 循环遍历
//        for (CartInfo cartInfo : cartInfoList) {
//            jedis.hset(userCartKey,cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
//        }
        HashMap<String, String> map = new HashMap<>();
        for (CartInfo cartInfo : cartInfoList) {
            map.put(cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
        }
        // 放入数据
        jedis.hmset(userCartKey,map);
        // 关闭redis
        jedis.close();

        return  cartInfoList;
    }
}
