package com.nineSeven.message.service;

import com.nineSeven.ResponseVO;
import com.nineSeven.enums.command.GroupEventCommand;
import com.nineSeven.enums.command.MessageCommand;
import com.nineSeven.group.service.ImGroupMemberService;
import com.nineSeven.model.message.GroupChatMessageContent;
import com.nineSeven.model.message.MessageContent;
import com.nineSeven.pack.message.ChatMessageAck;
import com.nineSeven.utils.MessageProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public void process(GroupChatMessageContent messageContent) {
        String fromId = messageContent.getFromId();
        String groupId = messageContent.getGroupId();
        Integer appId = messageContent.getAppId();
        ResponseVO responseVO = imServerPermissionCheck(fromId, groupId, appId);
        if(responseVO.isOk()) {
            messageStoreService.storeGroupMessage(messageContent);
            ack(messageContent, responseVO);
            syncToSender(messageContent);
            dispatchMessage(messageContent);
        } else {
            ack(messageContent, responseVO);
        }
    }

    private void ack(MessageContent messageContent, ResponseVO responseVO){

        ChatMessageAck chatMessageAck = new ChatMessageAck(messageContent.getMessageId(),messageContent.getMessageSequence());
        responseVO.setData(chatMessageAck);

        messageProducer.sendToUser(messageContent.getFromId(), GroupEventCommand.GROUP_MSG_ACK, responseVO, messageContent);
    }

    private void dispatchMessage(GroupChatMessageContent messageContent){
        imGroupMemberService.getGroupMemberId(messageContent.getGroupId(), messageContent.getAppId()).forEach(groupMemberId -> {
                    if(groupMemberId.equals(messageContent.getFromId())) {
                        messageProducer.sendToUser(groupMemberId, GroupEventCommand.MSG_GROUP, messageContent, messageContent.getAppId());
                    }
                });
    }

    private void syncToSender(GroupChatMessageContent messageContent){
        messageProducer.sendToUserExceptClient(messageContent.getFromId(), GroupEventCommand.MSG_GROUP, messageContent, messageContent);
    }

    public ResponseVO imServerPermissionCheck(String fromId,String groupId, Integer appId){

        return checkSendMessageService.checkGroupMessage(fromId,groupId, appId);
    }
}
