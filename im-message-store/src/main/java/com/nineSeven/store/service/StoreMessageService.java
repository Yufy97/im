package com.nineSeven.store.service;

import com.nineSeven.model.message.GroupChatMessageContent;
import com.nineSeven.store.dao.ImGroupMessageHistoryEntity;
import com.nineSeven.store.dao.mapper.ImGroupMessageHistoryMapper;
import com.nineSeven.store.model.DoStoreGroupMessageDto;
import com.nineSeven.store.model.DoStoreP2PMessageDto;
import com.nineSeven.store.dao.ImMessageBodyEntity;
import com.nineSeven.store.dao.ImMessageHistoryEntity;
import com.nineSeven.store.dao.mapper.ImMessageBodyMapper;
import com.nineSeven.store.dao.mapper.ImMessageHistoryMapper;
import com.nineSeven.model.message.MessageContent;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StoreMessageService {
    @Autowired
    ImMessageBodyMapper imMessageBodyMapper;

    @Autowired
    ImMessageHistoryMapper imMessageHistoryMapper;

    @Autowired
    ImGroupMessageHistoryMapper imGroupMessageHistoryMapper;



    public void storeP2PMessage(DoStoreP2PMessageDto dto) {
        imMessageBodyMapper.insert(dto.getImMessageBodyEntity());
        List<ImMessageHistoryEntity> imMessageHistoryEntities = extractToP2PMessageHistory(dto.getMessageContent(), dto.getImMessageBodyEntity());
        imMessageHistoryMapper.insertBatchSomeColumn(imMessageHistoryEntities);
    }

    private List<ImMessageHistoryEntity> extractToP2PMessageHistory(MessageContent messageContent, ImMessageBodyEntity imMessageBodyEntity) {
        List<ImMessageHistoryEntity> list = new ArrayList<>();
        ImMessageHistoryEntity fromHistory = new ImMessageHistoryEntity();
        BeanUtils.copyProperties(messageContent, fromHistory);
        fromHistory.setOwnerId(messageContent.getFromId());
        fromHistory.setMessageKey(imMessageBodyEntity.getMessageKey());
        fromHistory.setCreateTime(System.currentTimeMillis());
        fromHistory.setSequence(messageContent.getMessageSequence());

        ImMessageHistoryEntity toHistory = new ImMessageHistoryEntity();
        BeanUtils.copyProperties(messageContent, toHistory);
        toHistory.setOwnerId(messageContent.getToId());
        toHistory.setMessageKey(imMessageBodyEntity.getMessageKey());
        toHistory.setCreateTime(System.currentTimeMillis());
        toHistory.setSequence(messageContent.getMessageSequence());

        list.add(fromHistory);
        list.add(toHistory);
        return list;
    }

    public void doStoreGroupMessage(DoStoreGroupMessageDto dto) {
        imMessageBodyMapper.insert(dto.getImMessageBodyEntity());
        imGroupMessageHistoryMapper.insert(extractToGroupMessageHistory(dto.getGroupChatMessageContent(), dto.getImMessageBodyEntity()));
    }

    private ImGroupMessageHistoryEntity extractToGroupMessageHistory(GroupChatMessageContent messageContent , ImMessageBodyEntity messageBodyEntity) {
        ImGroupMessageHistoryEntity result = new ImGroupMessageHistoryEntity();
        BeanUtils.copyProperties(messageContent, result);
        result.setMessageKey(messageBodyEntity.getMessageKey());
        result.setCreateTime(System.currentTimeMillis());
        return result;
    }
}
