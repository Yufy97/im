package com.nineSeven.message.service;

import com.nineSeven.enums.command.MessageCommand;
import com.nineSeven.model.message.MessageReceiveAckContent;
import com.nineSeven.utils.MessageProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageSyncService {

    @Autowired
    MessageProducer messageProducer;

    public void receiveMark(MessageReceiveAckContent content) {
        messageProducer.sendToUser(content.getToId(), MessageCommand.MSG_RECIVE_ACK, content, content.getAppId());
    }
}
