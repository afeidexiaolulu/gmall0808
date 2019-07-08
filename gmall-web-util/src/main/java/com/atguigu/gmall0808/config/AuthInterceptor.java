package com.atguigu.gmall0808.config;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0808.util.HttpClientUtil;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {
    // 进入控制器之前执行
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取newToken {只有在登录成功之后！其他控制器都取不到！}
        String token = request.getParameter("newToken");
        if (token!=null){
            // 将token 放入cookie 中 使用工具类
            CookieUtil.setCookie(request,response,"token",token,WebConst.COOKIE_MAXAGE,false);
        }
        //{当用户登录之后} 用户走不需要登录的控制器时，
        if (token==null){
            token=CookieUtil.getCookieValue(request,"token",false);
        }
        // 取得token 然后做解密得到用户的昵称
        if (token!=null){
            // 解密token！
            // eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IkFkbWluaXN0cmF0b3IiLCJ1c2VySWQiOiIyIn0.WUvbFvXQnTMBGNyHWT-DE41MR9cn7c_W1oAtDAzb7VU
            // token 由三部分组成，私有部分：第二部分eyJuaWNrTmFtZSI6IkFkbWluaXN0cmF0b3IiLCJ1c2VySWQiOiIyIn0
            // 解密中间的私有部分得到一个map集合，然后从map集合中取得nickName
           Map map= getUserMapByToken(token);
           String nickName = (String) map.get("nickName");
           // 将用户昵称保持到作用域
            request.setAttribute("nickName",nickName);
        }
        //  需要知道方法上是否由该注解？handler 转换为方法类
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        LoginRequire methodAnnotation = handlerMethod.getMethodAnnotation(LoginRequire.class);
        if (methodAnnotation!=null){
            // 可能需要登录！调用认证方法
            // 先获取currentIp ，服务器的Ip地址
            String currentIp = request.getHeader("X-forwarded-for");
            // 调用认证方法verify() token,currentIp 参数都有了！但是，不在同一个项目中，httpClient
            // http://passport.atguigu.com/verify?token=eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IkFkbWluaXN0cmF0b3IiLCJ1c2VySWQiOiIyIn0.WUvbFvXQnTMBGNyHWT-DE41MR9cn7c_W1oAtDAzb7VU&currentIp=192.168.67.1
            String result = HttpClientUtil.doGet(WebConst.VERIFY_ADDRESS + "?token=" + token + "&currentIp=" + currentIp);
            if ("success".equals(result)){
                // 认证成功！保存一个用户Id {后续购物车会使用！}
                Map map= getUserMapByToken(token);
                String userId = (String) map.get("userId");
                // 将用户昵称保持到作用域
                request.setAttribute("userId",userId);
                // 放行！
                return true;
            } else {
                // 认证失败！
                // 需要不需要登录主要看autoRedirect() 是否是 true
                if (methodAnnotation.autoRedirect()){
                    // 必须登录！则需要跳转到登录页面！
                    // https://passport.jd.com/new/login.aspx?ReturnUrl=https%3A%2F%2Fitem.jd.com%2F5089253.html
                    // http://passport.atguigu.com/index?originUrl=http%3A%2F%2Fitem.gmall.com%2F50.html
                    // 获取当前的url
                    String requestURL  = request.getRequestURL().toString(); // http://item.gmall.com/50.html
                    // 将原始url 进行编码
                    String encodeURL  = URLEncoder.encode(requestURL, "UTF-8"); // http%3A%2F%2Fitem.gmall.com%2F50.html

                    // 跳转到登录页面
                    response.sendRedirect(WebConst.LOGIN_ADDRESS+"?originUrl="+encodeURL);
                    // 认证失败！
                    return  false;
                }
            }
        }
        return true;
    }

    private Map getUserMapByToken(String token) {
        // token :eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IkFkbWluaXN0cmF0b3IiLCJ1c2VySWQiOiIyIn0.WUvbFvXQnTMBGNyHWT-DE41MR9cn7c_W1oAtDAzb7VU
        // 得到token第二部分：
        // token 由三部分组成，私有部分：第二部分eyJuaWNrTmFtZSI6IkFkbWluaXN0cmF0b3IiLCJ1c2VySWQiOiIyIn0
        String tokenUserInfo  = StringUtils.substringBetween(token, ".");
        // Base64UrlCodec ？ JwtUtil.decode(token, key, "192.168.67.121");
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        byte[] bytes = base64UrlCodec.decode(tokenUserInfo);
        // 将数组转换为字符串
        String userStr = null;
        try {
            userStr = new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        // 字符串转换成map
        Map map = JSON.parseObject(userStr, Map.class);
        return  map;
    }

}
