package com.nineSeven.conversation.dao.controller;

import com.nineSeven.ResponseVO;
import com.nineSeven.conversation.model.DeleteConversationReq;
import com.nineSeven.conversation.model.UpdateConversationReq;
import com.nineSeven.conversation.service.ConversationService;
import com.nineSeven.model.SyncReq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("v1/conversation")
public class ConversationController {

    @Autowired
    ConversationService conversationService;

    @PostMapping("/deleteConversation")
    public ResponseVO deleteConversation(@RequestBody @Validated DeleteConversationReq req, Integer appId, String identifier) {
        req.setAppId(appId);
//        req.setOperater(identifier);
        return conversationService.deleteConversation(req);
    }

    @PostMapping("/updateConversation")
    public ResponseVO updateConversation(@RequestBody @Validated UpdateConversationReq req, Integer appId, String identifier) {
        req.setAppId(appId);
//        req.setOperater(identifier);
        return conversationService.updateConversation(req);
    }

    @PostMapping("/syncConversationList")
    public ResponseVO syncFriendShipList(@RequestBody @Validated SyncReq req, Integer appId) {
        req.setAppId(appId);
        return conversationService.syncConversationSet(req);
    }
}
