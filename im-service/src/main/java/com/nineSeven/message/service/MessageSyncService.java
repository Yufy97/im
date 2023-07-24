package com.nineSeven.message.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.nineSeven.ResponseVO;
import com.nineSeven.constant.Constants;
import com.nineSeven.conversation.service.ConversationService;
import com.nineSeven.enums.command.Command;
import com.nineSeven.enums.command.GroupEventCommand;
import com.nineSeven.enums.command.MessageCommand;
import com.nineSeven.model.SyncReq;
import com.nineSeven.model.SyncResp;
import com.nineSeven.model.message.MessageReadedContent;
import com.nineSeven.model.message.MessageReceiveAckContent;
import com.nineSeven.model.message.OfflineMessageContent;
import com.nineSeven.pack.message.MessageReadedPack;
import com.nineSeven.utils.MessageProducer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MessageSyncService {

    @Autowired
    MessageProducer messageProducer;

    @Autowired
    ConversationService conversationService;

    @Autowired
    RedisTemplate redisTemplate;
    public void receiveMark(MessageReceiveAckContent content) {
        messageProducer.sendToUser(content.getToId(), MessageCommand.MSG_RECIVE_ACK, content, content.getAppId());
    }

    public void readMark(MessageReadedContent messageReadedContent) {
        conversationService.messageMarkRead(messageReadedContent);

        MessageReadedPack messageReadedPack = new MessageReadedPack();
        BeanUtils.copyProperties(messageReadedContent, messageReadedPack);
        syncToSender(messageReadedPack, messageReadedContent, MessageCommand.MSG_READED_NOTIFY);
        messageProducer.sendToUser(messageReadedContent.getToId(), MessageCommand.MSG_READED_RECEIPT, messageReadedPack, messageReadedContent.getAppId());
    }

    private void syncToSender(MessageReadedPack pack, MessageReadedContent messagedContent, Command command) {
        messageProducer.sendToUserExceptClient(pack.getFromId(), command, pack, messagedContent);
    }

    public void groupReadMark(MessageReadedContent messageReaded) {
        conversationService.messageMarkRead(messageReaded);
        MessageReadedPack messageReadedPack = new MessageReadedPack();
        BeanUtils.copyProperties(messageReaded, messageReadedPack);

        syncToSender(messageReadedPack, messageReaded, GroupEventCommand.MSG_GROUP_READED_NOTIFY);
        if (!messageReaded.getFromId().equals(messageReaded.getToId())) {
            messageProducer.sendToUser(messageReadedPack.getToId(), GroupEventCommand.MSG_GROUP_READED_RECEIPT, messageReaded, messageReaded.getAppId());
        }
    }

    public ResponseVO syncOfflineMessage(SyncReq req) {
        SyncResp<OfflineMessageContent> resp = new SyncResp<>();
        ZSetOperations zSet = redisTemplate.opsForZSet();
        String key = req.getAppId() + ":" + Constants.RedisConstants.OfflineMessage + ":" + req.getOperater();
        Set set = zSet.reverseRangeWithScores(key, 0, 0);
        Long maxSeq = 0L;
        if(!CollectionUtils.isEmpty(set)){
            List list = new ArrayList(set);
            DefaultTypedTuple o = (DefaultTypedTuple) list.get(0);
            maxSeq = o.getScore().longValue();
        }
        resp.setMaxSequence(maxSeq);

        List<OfflineMessageContent> list = (List<OfflineMessageContent>) zSet.rangeByScore(key, req.getLastSequence(), maxSeq, 0, req.getMaxLimit()).stream()
                .map(o -> JSONObject.parseObject((String) o, OfflineMessageContent.class))
                .collect(Collectors.toList());

        resp.setDataList(list);
        if(!CollectionUtils.isEmpty(list)) {
            resp.setCompleted(maxSeq >=list.get(list.size() - 1).getMessageSequence());
        }
        return ResponseVO.successResponse(resp);
    }
}
