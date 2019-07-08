package com.atguigu.gmall0808.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// 相当于beans.xml
/*
    <beans>
        <bean id="jedisPoolConfig" class="redis.clients.jedis.JedisPoolConfig">
            <property name="maxTotal" value="200"></property>
        </bean>

        <bean id="jedisPool" class="redis.clients.jedis.JedisPool">
            <property name="jedisPoolConfig" ref="jedisPoolConfig"></property>
            <property name="host" value="192.168.67.211"></property>
            <property name="port" value="6379"></property>
        </bean>

        <bean id="redisUtil" class="com.atguigu.gmall0808.config.RedisUtil">

        </bean>


        main()
       JedisPool jedisPool =  getBean("jedisPool",JedisPool.class);
       Jedis jedis = jedisPool.getResource();
       jedis.set(key,value);
    </beans>
 */
@Configuration
public class RedisConfig {
    // 将 host，port 信息放入配置文件中 application.properties
    // :disabled 表示如果配置文件中没有值 的时候，host默认值为disabled
    @Value("${spring.redis.host:disabled}")
    private String host; // host=192.168.67.211

    @Value("${spring.redis.port:0}")
    private int port;

    @Value("${spring.redis.database:0}")
    private int database;

    // 给初始化initJedisPool的方法赋值，并且得到RedisUtil  @Bean 相当于在beans.xml 中一个bean 标签
    @Bean
    public RedisUtil getRedisUtil(){
        // 判断host不能为空
        if ("disabled".equals(host)){
            return null;
        }
        // 判断port必须有值
        RedisUtil redisUtil = new RedisUtil();
        redisUtil.initJedisPool(host,port,database);
        return redisUtil;
    }










}
