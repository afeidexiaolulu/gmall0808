package com.atguigu.gmall0808.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0808.bean.*;
import com.atguigu.gmall0808.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class ManageController {

    @Reference
    private ManageService manageService;

    @RequestMapping("index")
    public String index(){
        // 返回试图名称 试图应该在哪？默认走的templates文件下的所有页面数据，默认支持thymleaft
        return "index";
    }

    @RequestMapping("attrListPage")
    public String attrListPage(){
        return "attrListPage";
    }

    @RequestMapping("getCatalog1")
    @ResponseBody
    public List<BaseCatalog1> getCatalog1(){
        // 调用服务层返回数据 Json
        return manageService.getCatalog1();
    }

    @RequestMapping("getCatalog2")
    @ResponseBody
    public List<BaseCatalog2> getCatalog2(String catalog1Id){

        return manageService.getCatalog2(catalog1Id);
    }

    @RequestMapping("getCatalog3")
    @ResponseBody
    public List<BaseCatalog3>  getCatalog3(String catalog2Id){
        return manageService.getCatalog3(catalog2Id);
    }

    @RequestMapping("attrInfoList")
    @ResponseBody
    public List<BaseAttrInfo> attrInfoList(String catalog3Id){
        return manageService.getAttrList(catalog3Id);
    }

    @RequestMapping("saveAttrInfo")
    @ResponseBody
    public String saveAttrInfo(BaseAttrInfo baseAttrInfo){

        // 调用服务层的保存方法 {数据：前台传递过来的数据} ctrl+alt+b
        manageService.saveAttrInfo(baseAttrInfo);

        return "OK";
    }

    @RequestMapping("getAttrValueList")
    @ResponseBody
    public List<BaseAttrValue> getAttrValueList(String attrId){
        // attrId = baseAttrInfo.id; 调用服务层获取数据！
        // select * from baseAttrValue where attrId = ?
        // 从实际业务出发{平台属性 -- 平台属性值相关联！如果没有平台属性---会不会有平台属性值}
            // List<BaseAttrValue>  list =  manageService.xxx(attrId); 错误！
        // 先使用attrId 查询平台属性 -- 从平台属性中，去查找平台属性值
        BaseAttrInfo baseAttrInfo = manageService.getAttrInfo(attrId);

        return baseAttrInfo.getAttrValueList();
    }
}
