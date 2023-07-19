package com.nineSeven.message.service;

import com.nineSeven.ResponseVO;
import com.nineSeven.constant.Constants;
import com.nineSeven.enums.command.MessageCommand;
import com.nineSeven.model.ClientInfo;
import com.nineSeven.model.message.MessageContent;
import com.nineSeven.pack.message.ChatMessageAck;
import com.nineSeven.pack.message.MessageReceiveServerAckPack;
import com.nineSeven.seq.RedisSeq;
import com.nineSeven.utils.ConversationIdGenerate;
import com.nineSeven.utils.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class P2PMessageService {

    private static Logger logger = LoggerFactory.getLogger(P2PMessageService.class);

    @Autowired
    CheckSendMessageService checkSendMessageService;

    @Autowired
    MessageProducer messageProducer;

    @Autowired
    MessageStoreService messageStoreService;

    @Autowired
    RedisSeq redisSeq;

    private AtomicInteger atomicInteger = new AtomicInteger();
    private final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(8, 8, 60, TimeUnit.SECONDS, new LinkedBlockingDeque<>(1000), new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("message-process-thread-" + atomicInteger.getAndIncrement());
            return thread;
        }
    });

    public void process(MessageContent messageContent) {
        MessageContent messageFromMessageIdCache = messageStoreService.getMessageFromMessageIdCache(messageContent.getAppId(), messageContent.getMessageId(), MessageContent.class);
        if(messageFromMessageIdCache != null) {
            threadPoolExecutor.execute(() -> {
                //回复确认包
                ack(messageFromMessageIdCache, ResponseVO.successResponse());
                //同步发送者其他端
                syncToSender(messageFromMessageIdCache);

                if (dispatchMessage(messageFromMessageIdCache).isEmpty()) {
                    receiveAck(messageFromMessageIdCache);
                }
            });
            return;
        }

        threadPoolExecutor.execute(() -> {
            messageContent.setMessageSequence(redisSeq.getSeq(messageContent.getAppId() + ":" +
                    Constants.SeqConstants.Message + ":" + ConversationIdGenerate.generateP2PId(messageContent.getFromId(), messageContent.getToId())));
            //消息持久化
            messageStoreService.storeP2PMessage(messageContent);
            //回复确认包
            ack(messageContent, ResponseVO.successResponse());
            //同步发送者其他端
            syncToSender(messageContent);
            //发送消息
            if (dispatchMessage(messageContent).isEmpty()) {
                receiveAck(messageContent);
            }
            messageStoreService.setMessageFromMessageIdCache(messageContent.getAppId(), messageContent.getMessageId(), messageContent);
        });
    }

    private void ack(MessageContent messageContent, ResponseVO responseVO) {
        logger.info("msg ack,msgId={},checkResut{}", messageContent.getMessageId(), responseVO.getCode());

        ChatMessageAck chatMessageAck = new ChatMessageAck(messageContent.getMessageId(), messageContent.getMessageSequence());
        responseVO.setData(chatMessageAck);

        messageProducer.sendToUser(messageContent.getFromId(), MessageCommand.MSG_ACK, responseVO, messageContent);
    }

    private List<ClientInfo> dispatchMessage(MessageContent messageContent) {
        return messageProducer.sendToUser(messageContent.getToId(), MessageCommand.MSG_P2P, messageContent, messageContent.getAppId());
    }

    public void receiveAck(MessageContent messageContent) {
        MessageReceiveServerAckPack pack = new MessageReceiveServerAckPack();
        pack.setFromId(messageContent.getToId());
        pack.setToId(messageContent.getFromId());
        pack.setMessageKey(messageContent.getMessageKey());
        pack.setMessageSequence(messageContent.getMessageSequence());
        pack.setServerSend(true);
        messageProducer.sendToUser(messageContent.getFromId(), MessageCommand.MSG_RECIVE_ACK, pack,
                new ClientInfo(messageContent.getAppId(), messageContent.getClientType(), messageContent.getImei()));
    }

    private void syncToSender(MessageContent messageContent) {
        messageProducer.sendToUserExceptClient(messageContent.getFromId(), MessageCommand.MSG_P2P, messageContent, messageContent);
    }

    public ResponseVO imServerPermissionCheck(String fromId, String toId, Integer appId) {
        ResponseVO responseVO = checkSendMessageService.checkSenderForbidAndMute(fromId, appId);
        if (!responseVO.isOk()) {
            return responseVO;
        }
        responseVO = checkSendMessageService.checkFriendShip(fromId, toId, appId);
        return responseVO;
    }
}
