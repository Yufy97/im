package com.nineSeven.redis;

import com.nineSeven.config.BootStrapConfig;
import org.redisson.api.RedissonClient;

public class RedisManager {
    private static RedissonClient redissonClient;

    public static void init(BootStrapConfig config) {
        SingleClientStrategy singleClientStrategy = new SingleClientStrategy();
        redissonClient = singleClientStrategy.getRedissonClient(config.getIm().getRedis());
    }

    public static RedissonClient getRedissonClient() {
        return redissonClient;
    }
}
