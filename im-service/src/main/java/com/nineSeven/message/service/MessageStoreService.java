package com.nineSeven.message.service;

import com.alibaba.fastjson.JSONObject;
import com.nineSeven.config.AppConfig;
import com.nineSeven.constant.Constants;
import com.nineSeven.conversation.service.ConversationService;
import com.nineSeven.enums.ConversationTypeEnum;
import com.nineSeven.enums.DelFlagEnum;
import com.nineSeven.model.message.*;
import com.nineSeven.utils.SnowflakeIdWorker;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MessageStoreService {

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    AppConfig appConfig;

    @Autowired
    ConversationService conversationService;

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

    public void storeOfflineMessage(OfflineMessageContent content) {
        String fromKey = content.getAppId() + ":" + Constants.RedisConstants.OfflineMessage + ":" + content.getFromId();
        String toKey = content.getAppId() + ":" + Constants.RedisConstants.OfflineMessage + ":" + content.getToId();

        ZSetOperations<String, String> zSet = stringRedisTemplate.opsForZSet();
        if(zSet.zCard(fromKey) > appConfig.getOfflineMessageCount()) {
            zSet.removeRange(fromKey, 0, 0);
        }
        content.setConversationId(conversationService.convertConversationId(ConversationTypeEnum.P2P.getCode(), content.getFromId(), content.getToId()));
        zSet.add(fromKey, JSONObject.toJSONString(content), content.getMessageKey());

        if(zSet.zCard(toKey) > appConfig.getOfflineMessageCount()) {
            zSet.removeRange(fromKey, 0, 0);
        }
        content.setConversationId(conversationService.convertConversationId(ConversationTypeEnum.P2P.getCode(), content.getToId(), content.getFromId()));
        zSet.add(toKey, JSONObject.toJSONString(content), content.getMessageKey());
    }

    public void storeGroupOfflineMessage(OfflineMessageContent offlineMessage, List<String> memberIds){

        ZSetOperations<String, String> operations = stringRedisTemplate.opsForZSet();
        //判断 队列中的数据是否超过设定值
        offlineMessage.setConversationType(ConversationTypeEnum.GROUP.getCode());

        for (String memberId : memberIds) {
            // 找到toId的队列
            String toKey = offlineMessage.getAppId() + ":" + Constants.RedisConstants.OfflineMessage + ":" + memberId;
            offlineMessage.setConversationId(conversationService.convertConversationId(ConversationTypeEnum.GROUP.getCode(), memberId, offlineMessage.getToId()));
            if (operations.zCard(toKey) > appConfig.getOfflineMessageCount()) {
                operations.removeRange(toKey, 0, 0);
            }
            // 插入 数据 根据messageKey 作为分值
            operations.add(toKey, JSONObject.toJSONString(offlineMessage),
                    offlineMessage.getMessageKey());
        }
    }
}
