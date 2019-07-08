package com.atguigu.gmall0808.manage.mapper;

import com.atguigu.gmall0808.bean.SkuSaleAttrValue;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SkuSaleAttrValueMapper extends Mapper<SkuSaleAttrValue> {
    // 根据spuId查询所有的skuId 对应的销售属性值
    public List<SkuSaleAttrValue> selectSkuSaleAttrValueListBySpu (String spuId);
}
