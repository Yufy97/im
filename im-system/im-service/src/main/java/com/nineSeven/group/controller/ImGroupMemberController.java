package com.nineSeven.group.controller;

import com.nineSeven.ResponseVO;
import com.nineSeven.group.model.req.*;
import com.nineSeven.group.service.ImGroupMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/group/member")
public class ImGroupMemberController {
    @Autowired
    ImGroupMemberService imGroupMemberService;

    @PostMapping("/importGroupMember")
    public ResponseVO importGroupMember(@RequestBody @Validated ImportGroupMemberReq req, Integer appId, String identifier)  {
        req.setAppId(appId);
        req.setOperater(identifier);
        return imGroupMemberService.importGroupMember(req);
    }

    @PostMapping("/add")
    public ResponseVO addMember(@RequestBody @Validated AddGroupMemberReq req, Integer appId, String identifier)  {
        req.setAppId(appId);
        req.setOperater(identifier);
        return imGroupMemberService.addMember(req);
    }

    @PostMapping("/remove")
    public ResponseVO removeMember(@RequestBody @Validated RemoveGroupMemberReq req, Integer appId, String identifier)  {
        req.setAppId(appId);
        req.setOperater(identifier);
        return imGroupMemberService.removeMember(req);
    }

    @PostMapping("/exit")
    public ResponseVO memberExit(@RequestBody @Validated RemoveGroupMemberReq req, Integer appId)  {
        req.setAppId(appId);
        return imGroupMemberService.removeGroupMember(req.getGroupId(), appId, req.getMemberId());
    }

    @PostMapping("/update")
    public ResponseVO updateGroupMember(@RequestBody @Validated UpdateGroupMemberReq req, Integer appId, String identifier)  {
        req.setAppId(appId);
        req.setOperater(identifier);
        return imGroupMemberService.updateGroupMember(req);
    }

    @PostMapping("/speak")
    public ResponseVO speak(@RequestBody @Validated SpeakMemberReq req, Integer appId, String identifier)  {
        req.setAppId(appId);
        req.setOperater(identifier);
        return imGroupMemberService.speak(req);
    }
}
