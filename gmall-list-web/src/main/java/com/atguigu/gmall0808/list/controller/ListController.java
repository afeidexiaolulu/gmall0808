package com.atguigu.gmall0808.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0808.bean.*;
import com.atguigu.gmall0808.service.ListSerivce;
import com.atguigu.gmall0808.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {

    @Reference
    private ListSerivce listSerivce;

    @Reference
    private ManageService manageService;
    @RequestMapping("list.html")
//    @ResponseBody
    public String index(SkuLsParams skuLsParams, HttpServletRequest request){

        // 设置一下每页显示的条数
        skuLsParams.setPageSize(3);
        SkuLsResult skuLsResult = listSerivce.search(skuLsParams);

        // 将对象转化为字符串
        String skuLsJson = JSON.toJSONString(skuLsResult);
        List<SkuLsInfo> skuLsInfoList = skuLsResult.getSkuLsInfoList();

        // 两张表的关联关系 {baseAttrInfo.id=baseAttrValue.attr_id} 抽出条件：where baseAttrValue.id in (13,82,83)
        // 取得到平台属性值Id
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        // 制作href 要连接的url参数
        String urlParam = makeUrlParam(skuLsParams);
        // 调用服务层方法 查询平台属性集合
        List<BaseAttrInfo> baseAttrInfoList = manageService.getAttrList(attrValueIdList);
        // 声明一个集合来存储面包屑
        ArrayList<BaseAttrValue> baseAttrValueArrayList = new ArrayList<>();
        // 使用迭代器来循环遍历集合 itco ,iter ,itar
        for (Iterator<BaseAttrInfo> iterator = baseAttrInfoList.iterator(); iterator.hasNext(); ) {
            BaseAttrInfo baseAttrInfo = iterator.next();
            // 取得平台属性值集合
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            if (attrValueIdList!=null && attrValueIdList.size()>0){
                // 循环判断
                for (BaseAttrValue baseAttrValue : attrValueList) {
                    // 还需要跟skuLsParams.getValueId()数组中的valueId 进行比较，如果valueId 相同，则将数据移除！
                    if (skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
                        for (String valueId : skuLsParams.getValueId()) {
                            if (valueId.equals(baseAttrValue.getId())){
                                // 将相同的数据在集合中移除！
                                iterator.remove();

                                // 构造平台属性名称：平台属性值名称
                                BaseAttrValue baseAttrValueed = new BaseAttrValue();
                                baseAttrValueed.setValueName(baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName());
                                // url 中的整个参数，点击面包屑，要取除的平台属性值Id
                                String makeUrlParam = makeUrlParam(skuLsParams, valueId);
                                // 将最新的urlParam 放入到BaseAttrValue.urlParam
                                baseAttrValueed.setUrlParam(makeUrlParam);

                                // 将baseAttrValueed对象放入集合中
                                baseAttrValueArrayList.add(baseAttrValueed);
                            }
                        }
                    }
                }
            }

        }


        // 保存urlParam
        request.setAttribute("urlParam",urlParam);
        // 给页面渲染
        request.setAttribute("baseAttrInfoList",baseAttrInfoList);

        // 分页功能
        request.setAttribute("totalPages",skuLsResult.getTotalPages());
        request.setAttribute("pageNo",skuLsParams.getPageNo());
        // 保存一个关键字
        request.setAttribute("keyword",skuLsParams.getKeyword());
        // 将面包屑的集合保存起来，给前台使用
        request.setAttribute("baseAttrValueArrayList",baseAttrValueArrayList);
        // 将skuLsInfo 集合保存，到前台页面进行渲染！
        request.setAttribute("skuLsInfoList",skuLsInfoList);
        return "list";
    }

    /**
     *
     * @param skuLsParams url中的所有参数
     * @param excludeValueIds 点击平台属性值的Id
     * @return
     */
    private String makeUrlParam(SkuLsParams skuLsParams,String... excludeValueIds) {
        String urlParam = "";
//        http://localhost:8086/list.html?keyword=小米
        // 判断keyword不为空，将url后面追加上keyword
        if (skuLsParams.getKeyword()!=null){
            urlParam+="keyword="+skuLsParams.getKeyword();
        }
//        http://localhost:8086/list.html?keyword=小米&catalog3Id=61
        if (skuLsParams.getCatalog3Id()!=null){
            // 添加&符号
            if (urlParam.length()>0){
                urlParam+="&";
            }
            urlParam+="catalog3Id="+skuLsParams.getCatalog3Id();
        }
//        http://localhost:8086/list.html?keyword=小米&catalog3Id=61&valueId=82&valueId=83
        if (skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
            // 循环遍历
            for (int i = 0; i < skuLsParams.getValueId().length; i++) {
                String valueId  = skuLsParams.getValueId()[i];
                // skuLsParams.getValueId()数组中的数据与 excludeValueIds 进行匹配
                if (excludeValueIds!=null &&excludeValueIds.length>0){
                    // 每次用户只能点击一个值，取得下标为0的数据
                    String excludeValueId = excludeValueIds[0];
                    if (excludeValueId.equals(valueId)){
                        continue;
                    }
                }
                if (urlParam.length()>0){
                    urlParam+="&";
                }
                urlParam+="valueId="+valueId;
                // 将valueId 拼接到urlParam！
            }
        }
        System.out.println("urlParam:"+urlParam);
        return urlParam;

    }
}

