package com.atguigu.gmall0808.config;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

// 主要从连接池中获取jedis
public class RedisUtil {

    // 创建连接池
    private JedisPool jedisPool;

    // 做一个初始化方法
    public void initJedisPool(String host,int port,int database){
        // 配置一下连接池的参数
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        // 总数
        jedisPoolConfig.setMaxTotal(200);
        // 获取连接时等待的最大毫秒
        jedisPoolConfig.setMaxWaitMillis(10*1000);
        // 最少剩余数
        jedisPoolConfig.setMinIdle(10);
        // 如果到最大数，设置等待
        jedisPoolConfig.setBlockWhenExhausted(true);
        // 在获取连接时，检查是否有效
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPool = new JedisPool(jedisPoolConfig,host,port,20*1000);
    }
    // 获取jedis连接
    public Jedis getJedis(){
        Jedis jedis = jedisPool.getResource();
        return jedis;
    }


}
