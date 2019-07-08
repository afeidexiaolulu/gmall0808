package com.atguigu.gmall0808.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0808.bean.BaseSaleAttr;
import com.atguigu.gmall0808.bean.SpuInfo;
import com.atguigu.gmall0808.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class SpuManageController {

    @Reference
    private ManageService manageService;

    @RequestMapping("spuListPage")
    public String spuListPage(){
        return "spuListPage";
    }

//    @RequestMapping("spuList")
//    @ResponseBody
//    public List<SpuInfo> spuList(String catalog3Id){
//        SpuInfo spuInfo = new SpuInfo();
//        spuInfo.setCatalog3Id(catalog3Id);
//        return    manageService.getSpuInfoList(spuInfo);
//    }

//   pojo spirngmvc 对象传值
    @RequestMapping("spuList")
    @ResponseBody
    public List<SpuInfo> spuList(SpuInfo spuInfo){
        return    manageService.getSpuInfoList(spuInfo);
    }

    @RequestMapping("baseSaleAttrList")
    @ResponseBody
    public List<BaseSaleAttr> baseSaleAttrList(){
        return manageService.getBaseSaleAttrList();
    }

    @RequestMapping("saveSpuInfo")
    @ResponseBody
    public String saveSpuInfo(SpuInfo spuInfo){
        manageService.saveSpuInfo(spuInfo);
        return "OK";
    }
}
