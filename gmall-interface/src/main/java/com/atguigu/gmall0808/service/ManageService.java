package com.atguigu.gmall0808.service;

import com.atguigu.gmall0808.bean.*;

import java.util.List;

public interface ManageService {
    /**
     * 查询所有一级分类
     * @return
     */
    List<BaseCatalog1>  getCatalog1();

    /**
     * 根据一级分类Id 查询二级分类所有数据
     * @param catalog1Id
     * @return
     */
    List<BaseCatalog2>  getCatalog2(String catalog1Id);


    /**
     * 根据二级分类Id 查询三级分类所有数据
     * @param catalog2Id
     * @return
     */
    List<BaseCatalog3>  getCatalog3(String catalog2Id);

    /**
     * 根据三级分类Id 查询平台属性所有数据
     * @param catalog3Id
     * @return
     */
    List<BaseAttrInfo> getAttrList(String catalog3Id);

    /**
     * 保存平台属性-平台属性值
     * @param baseAttrInfo
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    /**
     * 根据属性值Id 查询属性对象
     * @param attrId
     * @return
     */
    BaseAttrInfo getAttrInfo(String attrId);

    /**
     * 主要根据三级分类Id 查询spuInfo的集合
     * @param spuInfo
     * @return
     */
    List<SpuInfo> getSpuInfoList(SpuInfo spuInfo);
    //List<SpuInfo> getSpuInfoList(String catalog3Id);

    /**
     * 查询所有的销售属性列表
     * @return
     */
    List<BaseSaleAttr> getBaseSaleAttrList();

    /**
     * 保存spuInfo 数据
     * @param spuInfo
     */
    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * 根据当前选中的spuId 查询所有图片
     * @param spuId
     * @return
     */
    List<SpuImage> getSpuImageList(String spuId);

    /**
     * 根据spuId 查询销售属性集合
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrList(String spuId);

    /**
     * 保存skuInfo 数据
     * @param skuInfo
     */
    void saveSku(SkuInfo skuInfo);

    /**
     * 根据skuId 查询skuInfo 对象
     * @param skuId
     * @return
     */
    SkuInfo getSkuInfo(String skuId);

    /**
     * 根据spuId,skuId 查询销售属性，销售属性值，并且使对应的skuId 的销售属性值默认选中
     * @param skuInfo
     * @return
     */
    List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(SkuInfo skuInfo);

    /**
     * 根据spuId查询skuId对应的销售属性值集合
     * @param spuId
     * @return
     */
    List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId);

    /**
     *
     * 通过平台属性值Id 查询平台属性集合
     * @param attrValueIdList
     * @return
     */
    List<BaseAttrInfo> getAttrList(List<String> attrValueIdList);
}
