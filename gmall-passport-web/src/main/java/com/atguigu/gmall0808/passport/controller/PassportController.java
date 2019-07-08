package com.atguigu.gmall0808.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0808.bean.UserInfo;
import com.atguigu.gmall0808.passport.config.JwtUtil;
import com.atguigu.gmall0808.service.UserInfoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Value("${token.key}")
    private String key;
    @Reference
    private UserInfoService userInfoService;

    @RequestMapping("index")
    public String index(HttpServletRequest request){
        // 取出登录后面跟随的originUrl 格式 ： index?originUrl=https%3A%2F%2Fitem.jd.com%2F5089253.html
        String originUrl = request.getParameter("originUrl");
        // 后台这里应该保持一个 originUrl
        request.setAttribute("originUrl",originUrl);
        return "index";
    }

    // 得到用户名，密码 springmvc 对象传值方式
    @RequestMapping("login")
    @ResponseBody
    public String login(UserInfo userInfo,HttpServletRequest request){
//        String salt = "192.168.67.1"; 从服务器上取得Ip地址
        String salt = request.getHeader("X-forwarded-for");
        // 调用服务层方法
        UserInfo info =  userInfoService.login(userInfo);
        if (info!=null){
            // 生成token
            HashMap<String, Object> map = new HashMap<>();
            map.put("userId",info.getId());
            map.put("nickName",info.getNickName());

            String token = JwtUtil.encode(key, map, salt);
            System.out.println("token:"+token);
            return token; // token
        }else {
            return "fail";
        }
    }
    // 认真
    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request){
        // 先获取token ，salt
        String token = request.getParameter("token");
        String currentIp = request.getParameter("currentIp");

        // 解密
        Map<String, Object> map = JwtUtil.decode(token, key, currentIp);
        if (map!=null && map.size()>0){
            // 取出userId
            String userId = (String) map.get("userId");
            // 调用方法
            UserInfo userInfo =  userInfoService.verify(userId);
            if (userInfo!=null){
                return "success";
            }else {
                return "fail";
            }
        }
        return "fail";

    }


}
