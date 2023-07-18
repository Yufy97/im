package com.nineSeven.message.controller;

import com.nineSeven.ResponseVO;
import com.nineSeven.message.service.P2PMessageService;
import com.nineSeven.model.message.CheckSendMessageReq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("v1/message")
public class MessageController {

    @Autowired
    P2PMessageService p2PMessageService;

    @RequestMapping("/checkSend")
    public ResponseVO checkSend(@RequestBody @Validated CheckSendMessageReq req) {
        return p2PMessageService.imServerPermissionCheck(req.getFromId(), req.getToId(), req.getAppId());
    }
}
