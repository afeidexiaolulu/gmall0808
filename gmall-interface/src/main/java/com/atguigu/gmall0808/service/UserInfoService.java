package com.atguigu.gmall0808.service;

import com.atguigu.gmall0808.bean.UserAddress;
import com.atguigu.gmall0808.bean.UserInfo;

import java.util.List;

public interface UserInfoService {

    // 查询所有用户信息
    List<UserInfo> findAll();

    // 根据userId 查询用户地址列表
    List<UserAddress> findUserAddressByUserId(String userId);

    /**
     * 登录方法
     * @param userInfo
     * @return
     */
    UserInfo login(UserInfo userInfo);

    /**
     * 验证用户是否登录
     * @param userId
     * @return
     */
    UserInfo verify(String userId);
}
