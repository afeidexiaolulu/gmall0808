package com.atguigu.gmall0808.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0808.bean.SkuInfo;
import com.atguigu.gmall0808.bean.SkuLsInfo;
import com.atguigu.gmall0808.bean.SpuImage;
import com.atguigu.gmall0808.bean.SpuSaleAttr;
import com.atguigu.gmall0808.service.ListSerivce;
import com.atguigu.gmall0808.service.ManageService;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@Controller
public class SkuManageController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListSerivce listSerivce;

    @RequestMapping("spuImageList")
    @ResponseBody
    public List<SpuImage> spuImageList(String spuId){
       return manageService.getSpuImageList(spuId);
    }

    @RequestMapping("spuSaleAttrList")
    @ResponseBody
    public List<SpuSaleAttr> spuSaleAttrList(String spuId){
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrList(spuId);
        return spuSaleAttrList;
    }

    @RequestMapping("saveSku")
    @ResponseBody
    public String saveSku(SkuInfo skuInfo){
        manageService.saveSku(skuInfo);
        return "OK";
    }

    @RequestMapping("onSale")
    @ResponseBody
    public String onSale(String skuId){
        // 通过skuId 查询skuInfo 对象
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        // 声明对象
        SkuLsInfo skuLsInfo = new SkuLsInfo();
        // 必须给skuLsInfo 赋值！使用工具类
//        try {
//            BeanUtils.copyProperties(skuLsInfo,skuInfo);
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        }
        org.springframework.beans.BeanUtils.copyProperties(skuInfo,skuLsInfo);
        listSerivce.saveSkuInfo(skuLsInfo);
        
        return "OK";
    }


}
