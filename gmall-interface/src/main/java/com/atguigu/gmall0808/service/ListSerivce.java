package com.atguigu.gmall0808.service;

import com.atguigu.gmall0808.bean.SkuLsInfo;
import com.atguigu.gmall0808.bean.SkuLsParams;
import com.atguigu.gmall0808.bean.SkuLsResult;

public interface ListSerivce {
    /**
     * 将数据库中的重要字段{封装到skuLsInfo}保存到es中
     * @param skuLsInfo
     */
    void saveSkuInfo(SkuLsInfo skuLsInfo);

    /**
     *
     * 根据用户传递过来的参数进行查询数据！
     * @param skuLsParam
     * @return
     */
    SkuLsResult search(SkuLsParams skuLsParam);

    /**
     * 根据skuId 更新商品被访问的次数
     * @param skuId
     */
    void incrHotScore(String skuId);
}
