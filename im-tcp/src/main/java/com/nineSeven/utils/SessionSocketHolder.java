package com.nineSeven.utils;

import com.alibaba.fastjson.JSONObject;
import com.nineSeven.constant.Constants;
import com.nineSeven.enums.ImConnectStatusEnum;
import com.nineSeven.model.UserClientDto;
import com.nineSeven.model.UserSession;
import com.nineSeven.redis.RedisManager;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SessionSocketHolder {
    public static final Map<UserClientDto, NioSocketChannel> CHANNELS = new ConcurrentHashMap<UserClientDto, NioSocketChannel>();

    public static void put(Integer appId, String userId,Integer clientType,String imei, NioSocketChannel channel) {
        UserClientDto userClientDto = new UserClientDto();
        userClientDto.setAppId(appId);
        userClientDto.setUserId(userId);
        userClientDto.setClientType(clientType);
        userClientDto.setImei(imei);
        CHANNELS.put(userClientDto, channel);
    }

    public static NioSocketChannel get(Integer appId, String userId,Integer clientType, String imei) {
        UserClientDto userClientDto = new UserClientDto();
        userClientDto.setAppId(appId);
        userClientDto.setUserId(userId);
        userClientDto.setClientType(clientType);
        userClientDto.setImei(imei);
        return CHANNELS.get(userClientDto);
    }

    public static List<NioSocketChannel> get(Integer appId , String id) {

        Set<UserClientDto> channelInfos = CHANNELS.keySet();
        List<NioSocketChannel> channels = new ArrayList<>();

        channelInfos.forEach(channel ->{
            if(channel.getAppId().equals(appId) && id.equals(channel.getUserId())){
                channels.add(CHANNELS.get(channel));
            }
        });

        return channels;
    }

    public static void remove(Integer appId, String userId,Integer clientType, String imei) {
        UserClientDto userClientDto = new UserClientDto();
        userClientDto.setAppId(appId);
        userClientDto.setUserId(userId);
        userClientDto.setClientType(clientType);
        userClientDto.setImei(imei);
        CHANNELS.remove(userClientDto);
    }

    public static void remove(NioSocketChannel channel) {
        CHANNELS.entrySet().stream().filter(o -> o.getValue() == channel).forEach(o -> CHANNELS.remove(o.getKey()));
    }

    public static void removeUserSession(NioSocketChannel nioSocketChannel){
        //获取管道绑定的userId,appId,clientType
        Integer appId = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.AppId)).get();
        String userId = (String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get();
        Integer clientType = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ClientType)).get();
        String imei = (String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.Imei)).get();

        //删除管道绑定的userId,appId,clientType
        SessionSocketHolder.remove(appId, userId, clientType, imei);

        //删除redis的缓存
        RedissonClient redissonClient = RedisManager.getRedissonClient();
        RMap<String, String> map = redissonClient.getMap(appId + Constants.RedisConstants.UserSessionConstants +userId);
        map.remove(clientType + ":" + imei);

        //关闭管道
        nioSocketChannel.close();
    }

    public static void offlineUserSession(NioSocketChannel nioSocketChannel) {
        //获取管道绑定的userId,appId,clientType
        Integer appId = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.AppId)).get();
        String userId = (String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get();
        Integer clientType = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ClientType)).get();
        String imei = (String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.Imei)).get();

        SessionSocketHolder.remove(appId, userId, clientType, imei);

        //修改redis的缓存
        RedissonClient redissonClient = RedisManager.getRedissonClient();
        RMap<String, String> map = redissonClient.getMap(appId + Constants.RedisConstants.UserSessionConstants +userId);
        String session = map.get(clientType + ":" + imei);

        if(!StringUtils.isBlank(session)) {
            UserSession userSession = JSONObject.parseObject(session, UserSession.class);
            userSession.setConnectState(ImConnectStatusEnum.OFFLINE_STATUS.getCode());
            map.put(clientType + ":" + imei, JSONObject.toJSONString(userSession));
        }

        //关闭管道
        nioSocketChannel.close();
    }
}
