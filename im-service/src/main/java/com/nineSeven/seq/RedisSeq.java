package com.nineSeven.seq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisSeq {
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    public long getSeq(String key){
        return stringRedisTemplate.opsForValue().increment(key);
    }
}
