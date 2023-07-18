package com.nineSeven.message.service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPObject;
import com.nineSeven.constant.Constants;
import com.nineSeven.enums.DelFlagEnum;
import com.nineSeven.message.dao.ImGroupMessageHistoryEntity;
import com.nineSeven.message.dao.ImMessageBodyEntity;
import com.nineSeven.message.dao.ImMessageHistoryEntity;
import com.nineSeven.message.dao.mapper.ImGroupMessageHistoryMapper;
import com.nineSeven.message.dao.mapper.ImMessageBodyMapper;
import com.nineSeven.message.dao.mapper.ImMessageHistoryMapper;
import com.nineSeven.model.message.DoStoreP2PMessageDto;
import com.nineSeven.model.message.GroupChatMessageContent;
import com.nineSeven.model.message.ImMessageBody;
import com.nineSeven.model.message.MessageContent;
import com.nineSeven.utils.SnowflakeIdWorker;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class MessageStoreService {
    @Autowired
    private SnowflakeIdWorker snowflakeIdWorker;

    @Autowired
    private ImMessageBodyMapper imMessageBodyMapper;

    @Autowired
    private ImMessageHistoryMapper imMessageHistoryMapper;

    @Autowired
    private ImGroupMessageHistoryMapper imGroupMessageHistoryMapper;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Transactional
    public void storeP2PMessage(MessageContent messageContent) {
//        imMessageBodyMapper.insert(imMessageBodyEntity);
//
//        List<ImMessageHistoryEntity> imMessageHistoryEntities = extractToP2PMessageHistory(messageContent, imMessageBodyEntity);
//        imMessageHistoryMapper.insertBatchSomeColumn(imMessageHistoryEntities);
//        messageContent.setMessageKey(imMessageBodyEntity.getMessageKey());
        ImMessageBody imMessageBody = extractMessageBody(messageContent);
        DoStoreP2PMessageDto dto = new DoStoreP2PMessageDto();
        messageContent.setMessageKey(imMessageBody.getMessageKey());
        dto.setGroupChatMessageContent(messageContent);
        dto.setMessageBody(imMessageBody);

        rabbitTemplate.convertAndSend(Constants.RabbitConstants.StoreP2PMessage, "", JSONObject.toJSONString(dto));
    }

    @Transactional
    public void storeGroupMessage(GroupChatMessageContent messageContent) {
//        ImMessageBodyEntity imMessageBodyEntity = extractMessageBody(messageContent);
//        imMessageBodyMapper.insert(imMessageBodyEntity);
//
//        imGroupMessageHistoryMapper.insert(extractToGroupMessageHistory(messageContent, imMessageBodyEntity));
    }

    private ImGroupMessageHistoryEntity extractToGroupMessageHistory(GroupChatMessageContent messageContent , ImMessageBodyEntity messageBodyEntity) {
        ImGroupMessageHistoryEntity result = new ImGroupMessageHistoryEntity();
        BeanUtils.copyProperties(messageContent, result);
        result.setMessageKey(messageBodyEntity.getMessageKey());
        result.setCreateTime(System.currentTimeMillis());
        return result;
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

    private List<ImMessageHistoryEntity> extractToP2PMessageHistory(MessageContent messageContent, ImMessageBodyEntity imMessageBodyEntity) {
        List<ImMessageHistoryEntity> list = new ArrayList<>();
        ImMessageHistoryEntity fromHistory = new ImMessageHistoryEntity();
        BeanUtils.copyProperties(messageContent, fromHistory);
        fromHistory.setOwnerId(messageContent.getFromId());
        fromHistory.setMessageKey(imMessageBodyEntity.getMessageKey());
        fromHistory.setCreateTime(System.currentTimeMillis());

        ImMessageHistoryEntity toHistory = new ImMessageHistoryEntity();
        BeanUtils.copyProperties(messageContent, toHistory);
        toHistory.setOwnerId(messageContent.getToId());
        toHistory.setMessageKey(imMessageBodyEntity.getMessageKey());
        toHistory.setCreateTime(System.currentTimeMillis());

        list.add(fromHistory);
        list.add(toHistory);
        return list;
    }
}
