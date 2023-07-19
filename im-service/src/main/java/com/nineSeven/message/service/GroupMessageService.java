package com.nineSeven.message.service;

import com.nineSeven.ResponseVO;
import com.nineSeven.constant.Constants;
import com.nineSeven.enums.command.GroupEventCommand;
import com.nineSeven.enums.command.MessageCommand;
import com.nineSeven.group.service.ImGroupMemberService;
import com.nineSeven.model.message.GroupChatMessageContent;
import com.nineSeven.model.message.MessageContent;
import com.nineSeven.pack.message.ChatMessageAck;
import com.nineSeven.seq.RedisSeq;
import com.nineSeven.utils.MessageProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class GroupMessageService {

    @Autowired
    CheckSendMessageService checkSendMessageService;

    @Autowired
    MessageProducer messageProducer;

    @Autowired
    ImGroupMemberService imGroupMemberService;

    @Autowired
    MessageStoreService messageStoreService;

    @Autowired
    RedisSeq redisSeq;

    private final ThreadPoolExecutor threadPoolExecutor;

    {
        AtomicInteger num = new AtomicInteger(0);
        threadPoolExecutor = new ThreadPoolExecutor(8, 8, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000), r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("message-group-thread-" + num.getAndIncrement());
            return thread;
        });
    }

    public void process(GroupChatMessageContent messageContent) {
        GroupChatMessageContent messageFromMessageIdCache = messageStoreService.getMessageFromMessageIdCache(messageContent.getAppId(), messageContent.getMessageId(), GroupChatMessageContent.class);
        if (messageFromMessageIdCache != null) {
            threadPoolExecutor.execute(() -> {
                ack(messageContent, ResponseVO.successResponse());
                syncToSender(messageContent);
                dispatchMessage(messageContent);
            });
            return;
        }

        messageContent.setMessageSequence(redisSeq.getSeq(messageContent.getAppId() + ":" + Constants.SeqConstants.GroupMessage + ":" + messageContent.getGroupId()));
        threadPoolExecutor.execute(() -> {
            messageStoreService.storeGroupMessage(messageContent);

            messageContent.setMemberId(imGroupMemberService.getGroupMemberId(messageContent.getGroupId(), messageContent.getAppId()));

            ack(messageContent, ResponseVO.successResponse());
            syncToSender(messageContent);
            dispatchMessage(messageContent);
            messageStoreService.setMessageFromMessageIdCache(messageContent.getAppId(), messageContent.getMessageId(), messageContent);
        });
    }

    private void ack(MessageContent messageContent, ResponseVO responseVO) {

        ChatMessageAck chatMessageAck = new ChatMessageAck(messageContent.getMessageId(), messageContent.getMessageSequence());
        responseVO.setData(chatMessageAck);

        messageProducer.sendToUser(messageContent.getFromId(), GroupEventCommand.GROUP_MSG_ACK, responseVO, messageContent);
    }

    private void dispatchMessage(GroupChatMessageContent messageContent) {
        imGroupMemberService.getGroupMemberId(messageContent.getGroupId(), messageContent.getAppId()).forEach(groupMemberId -> {
            if (groupMemberId.equals(messageContent.getFromId())) {
                messageProducer.sendToUser(groupMemberId, GroupEventCommand.MSG_GROUP, messageContent, messageContent.getAppId());
            }
        });
    }

    private void syncToSender(GroupChatMessageContent messageContent) {
        messageProducer.sendToUserExceptClient(messageContent.getFromId(), GroupEventCommand.MSG_GROUP, messageContent, messageContent);
    }
}
