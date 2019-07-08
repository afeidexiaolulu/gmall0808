package com.atguigu.gmall0808.usermanage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0808.bean.UserAddress;
import com.atguigu.gmall0808.bean.UserInfo;
import com.atguigu.gmall0808.config.RedisUtil;
import com.atguigu.gmall0808.service.UserInfoService;
import com.atguigu.gmall0808.usermanage.mapper.UserAddressMapper;
import com.atguigu.gmall0808.usermanage.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class UserInfoServiceImpl implements UserInfoService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private RedisUtil redisUtil;

    public String USERKEY_PREFIX="user:";
    public String USERINFOKEY_SUFFIX=":info";
    public int USERKEY_TIMEOUT=60*60*24;


    @Override
    public List<UserInfo> findAll() {
        return userInfoMapper.selectAll();
    }

    @Override
    public List<UserAddress> findUserAddressByUserId(String userId) {
        // select * from userAddress where userId = ?
        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        return userAddressMapper.select(userAddress);
    }

    /**
     * 登录方法
     *
     * @param userInfo
     * @return
     */
    @Override
    public UserInfo login(UserInfo userInfo) {
        // select * fromn user_info where loginName= ? and passwd=?
        // 密码是加密的 {202cb962ac59075b964b07152d234b70}
        String passwd = userInfo.getPasswd();
        // 使用工具类进行加密
        String newPassword = DigestUtils.md5DigestAsHex(passwd.getBytes());
        userInfo.setPasswd(newPassword);
        UserInfo info = userInfoMapper.selectOne(userInfo);
        // 需要将登录的用户信息放入redis
        if (info!=null){
            // 放入redis
            Jedis jedis = redisUtil.getJedis();
            // 定义key user:userId:info
            String userKey = USERKEY_PREFIX+info.getId()+USERINFOKEY_SUFFIX;
            jedis.setex(userKey,USERKEY_TIMEOUT, JSON.toJSONString(info));
            jedis.close();
        }


        return info;
    }

    /**
     * 验证用户是否登录
     *
     * @param userId
     * @return
     */
    @Override
    public UserInfo verify(String userId) {
        // 获取jedis
        Jedis jedis = redisUtil.getJedis();
        // 定义key
        String userKey = USERKEY_PREFIX+userId+USERINFOKEY_SUFFIX;
        // 取出数据
        String userJson = jedis.get(userKey);
        // 延长用户的过期时间
        jedis.expire(userKey,USERKEY_TIMEOUT);
        // 判断当前是否有数据
        if (userJson!=null &&!"".equals(userJson)){
            UserInfo userInfo = JSON.parseObject(userJson, UserInfo.class);
            return userInfo;
        }
        return null;
    }
}
