package com.nineSeven.redis;

import com.nineSeven.config.BootStrapConfig;
import com.nineSeven.receiever.UserLoginMessageListener;
import org.redisson.api.RedissonClient;

public class RedisManager {
    private static RedissonClient redissonClient;

    public static void init(BootStrapConfig config) {
        SingleClientStrategy singleClientStrategy = new SingleClientStrategy();
        redissonClient = singleClientStrategy.getRedissonClient(config.getIm().getRedis());
        new UserLoginMessageListener(config.getIm().getLoginModel()).listenerUserLogin();
    }

    public static RedissonClient getRedissonClient() {
        return redissonClient;
    }
}
