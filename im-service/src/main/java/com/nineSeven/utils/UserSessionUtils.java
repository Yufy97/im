package com.nineSeven.utils;

import com.alibaba.fastjson.JSONObject;
import com.nineSeven.constant.Constants;
import com.nineSeven.enums.ImConnectStatusEnum;
import com.nineSeven.model.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


@Component
public class UserSessionUtils {

//    public Object get;
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    public List<UserSession> getUserSession(Integer appId, String userId){

        String userSessionKey = appId + Constants.RedisConstants.UserSessionConstants + userId;
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(userSessionKey);
        List<UserSession> list = new ArrayList<>();
        Collection<Object> values = entries.values();
        for (Object o : values){
            UserSession session = JSONObject.parseObject((String) o, UserSession.class);
            if(session.getConnectState().equals(ImConnectStatusEnum.ONLINE_STATUS.getCode())){
                list.add(session);
            }
        }
        return list;
    }


    public UserSession getUserSession(Integer appId,String userId, Integer clientType,String imei){

        String userSessionKey = appId + Constants.RedisConstants.UserSessionConstants + userId;
        String hashKey = clientType + ":" + imei;
        Object o = stringRedisTemplate.opsForHash().get(userSessionKey, hashKey);
        return JSONObject.parseObject(o.toString(), UserSession.class);
    }


}
