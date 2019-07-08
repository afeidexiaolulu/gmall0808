package com.atguigu.gmall0808.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0808.bean.SkuInfo;
import com.atguigu.gmall0808.bean.SkuSaleAttrValue;
import com.atguigu.gmall0808.bean.SpuSaleAttr;
import com.atguigu.gmall0808.service.ListSerivce;
import com.atguigu.gmall0808.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;

@Controller
public class ItemController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListSerivce listSerivce;

    @RequestMapping("{skuId}.html")
    public String getSkuItem(@PathVariable String skuId, HttpServletRequest request){
        System.out.println("skuId:"+skuId);
        // 根据skuId 查询skuInfo 对象
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        // 加载销售属性值
        List<SpuSaleAttr> saleAttrList =  manageService.selectSpuSaleAttrListCheckBySku(skuInfo);

        // 获取所有的销售属性值集合
        List<SkuSaleAttrValue> skuSaleAttrValueListBySpu = manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());
        // 声明一个key
        String key = "";
        // 声明一个map集合
        HashMap<String, Object> map = new HashMap<>();
        for (int i = 0; i < skuSaleAttrValueListBySpu.size(); i++) {
            SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueListBySpu.get(i);
            // 第一次拼接 key = 185
            // 第二次拼接 key = 185|
            // 第三次拼接 key = 185|188 ,并将 key ，skuId 放入map集合
            // 放入完成之后，再继续拼接key， 此处key 应该从新拼接 key=185

            if (key.length()>0){
                key+="|";
            }
            key+=skuSaleAttrValue.getSaleAttrValueId();
            // 拼接完成之后，将key ，放入map集合中
            if ((i+1)==skuSaleAttrValueListBySpu.size() || !skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueListBySpu.get(i+1).getSkuId())){
                map.put(key,skuSaleAttrValue.getSkuId());
                key="";
            }
        }

        // 将map 转换为json 字符串，并保持到页面
        String valuesSkuJson = JSON.toJSONString(map);


        System.out.println(valuesSkuJson);

        request.setAttribute("valuesSkuJson",valuesSkuJson);

        // 将数据-销售属性对象集合进行保存
        request.setAttribute("saleAttrList",saleAttrList);
        //  保存skuInfo 对象信息
        request.setAttribute("skuInfo",skuInfo);
        // 记录商品被访问的次数！
        listSerivce.incrHotScore(skuId);
        return "item";
    }
}
