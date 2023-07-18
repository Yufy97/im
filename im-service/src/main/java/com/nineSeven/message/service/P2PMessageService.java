package com.nineSeven.message.service;

import com.nineSeven.ResponseVO;
import com.nineSeven.enums.command.MessageCommand;
import com.nineSeven.model.message.MessageContent;
import com.nineSeven.pack.message.ChatMessageAck;
import com.nineSeven.utils.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
            threadPoolExecutor.execute(() -> {
                messageStoreService.storeP2PMessage(messageContent);
                ack(messageContent, ResponseVO.successResponse());
                syncToSender(messageContent);
                dispatchMessage(messageContent);
            });
    }

    private void ack(MessageContent messageContent, ResponseVO responseVO){
        logger.info("msg ack,msgId={},checkResut{}",messageContent.getMessageId(),responseVO.getCode());

        ChatMessageAck chatMessageAck = new ChatMessageAck(messageContent.getMessageId(),messageContent.getMessageSequence());
        responseVO.setData(chatMessageAck);

        messageProducer.sendToUser(messageContent.getFromId(), MessageCommand.MSG_ACK, responseVO, messageContent);
    }

    private void dispatchMessage(MessageContent messageContent){

        messageProducer.sendToUser(messageContent.getToId(), MessageCommand.MSG_P2P, messageContent, messageContent.getAppId());
    }

    private void syncToSender(MessageContent messageContent){
        messageProducer.sendToUserExceptClient(messageContent.getFromId(), MessageCommand.MSG_P2P, messageContent, messageContent);
    }

    public ResponseVO imServerPermissionCheck(String fromId,String toId, Integer appId){
        ResponseVO responseVO = checkSendMessageService.checkSenderForbidAndMute(fromId, appId);
        if(!responseVO.isOk()){
            return responseVO;
        }
        responseVO = checkSendMessageService.checkFriendShip(fromId, toId, appId);
        return responseVO;
    }
}
