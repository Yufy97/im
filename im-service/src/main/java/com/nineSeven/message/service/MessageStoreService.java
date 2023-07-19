package com.nineSeven.message.service;

import com.alibaba.fastjson.JSONObject;
import com.nineSeven.constant.Constants;
import com.nineSeven.enums.DelFlagEnum;
import com.nineSeven.message.dao.ImGroupMessageHistoryEntity;
import com.nineSeven.message.dao.ImMessageBodyEntity;
import com.nineSeven.message.dao.ImMessageHistoryEntity;
import com.nineSeven.message.dao.mapper.ImGroupMessageHistoryMapper;
import com.nineSeven.message.dao.mapper.ImMessageBodyMapper;
import com.nineSeven.message.dao.mapper.ImMessageHistoryMapper;
import com.nineSeven.model.message.*;
import com.nineSeven.utils.SnowflakeIdWorker;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MessageStoreService {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Transactional
    public void storeP2PMessage(MessageContent messageContent) {
        ImMessageBody imMessageBody = extractMessageBody(messageContent);
        DoStoreP2PMessageDto dto = new DoStoreP2PMessageDto();
        messageContent.setMessageKey(imMessageBody.getMessageKey());
        dto.setMessageContent(messageContent);
        dto.setMessageBody(imMessageBody);

        rabbitTemplate.convertAndSend(Constants.RabbitConstants.StoreP2PMessage, "", JSONObject.toJSONString(dto));
    }

    @Transactional
    public void storeGroupMessage(GroupChatMessageContent messageContent) {

        ImMessageBody imMessageBody = extractMessageBody(messageContent);
        DoStoreGroupMessageDto dto = new DoStoreGroupMessageDto();
        messageContent.setMessageKey(imMessageBody.getMessageKey());
        dto.setMessageBody(imMessageBody);
        dto.setGroupChatMessageContent(messageContent);

        rabbitTemplate.convertAndSend(Constants.RabbitConstants.StoreGroupMessage, "", JSONObject.toJSONString(dto));
    }


    private ImMessageBody extractMessageBody(MessageContent messageContent) {
        ImMessageBody messageBody = new ImMessageBody();
        messageBody.setAppId(messageContent.getAppId());
        messageBody.setMessageKey(SnowflakeIdWorker.nextId());
        messageBody.setCreateTime(System.currentTimeMillis());
        messageBody.setSecurityKey("");
        messageBody.setExtra(messageContent.getExtra());
        messageBody.setDelFlag(DelFlagEnum.NORMAL.getCode());
        messageBody.setMessageTime(messageContent.getMessageTime());
        messageBody.setMessageBody(messageContent.getMessageBody());
        return messageBody;
    }


    public void setMessageFromMessageIdCache(Integer appId,String messageId,Object messageContent){
        //appid : cache : messageId
        stringRedisTemplate.opsForValue().set(appId + ":" + Constants.RedisConstants.cacheMessage + ":" + messageId,
                JSONObject.toJSONString(messageContent),300, TimeUnit.SECONDS);
    }

    public <T> T getMessageFromMessageIdCache(Integer appId, String messageId, Class<T> clazz){
        //appid : cache : messageId
        String msg = stringRedisTemplate.opsForValue().get(appId + ":" + Constants.RedisConstants.cacheMessage + ":" + messageId);
        if(StringUtils.isBlank(msg)){
            return null;
        }
        return JSONObject.parseObject(msg, clazz);
    }
}
