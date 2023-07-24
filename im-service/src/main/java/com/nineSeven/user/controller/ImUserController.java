package com.nineSeven.user.controller;


import com.nineSeven.ClientType;
import com.nineSeven.ResponseVO;
import com.nineSeven.route.RouteHandle;
import com.nineSeven.route.RouteInfo;
import com.nineSeven.user.model.req.DeleteUserReq;
import com.nineSeven.user.model.req.GetUserSequenceReq;
import com.nineSeven.user.model.req.ImportUserReq;
import com.nineSeven.user.model.req.LoginReq;
import com.nineSeven.user.service.ImUserService;
import com.nineSeven.utils.ZKit;
import com.nineSeven.utils.RouteInfoParseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/v1/user")
public class ImUserController {

    @Autowired
    ImUserService imUserService;

    @Autowired
    RouteHandle routeHandle;

    @Autowired
    ZKit zKit;

    @PostMapping("/login")
    public ResponseVO login(@RequestBody @Validated LoginReq req, Integer appId) {
        req.setAppId(appId);
        ResponseVO login = imUserService.login(req);
        if(login.isOk()) {
            List<String> allNodes;
            if(req.getClientType() == ClientType.WEB.getCode()) {
                allNodes = zKit.getAllWebNode();
            } else {
                allNodes = zKit.getAllTcpNode();
            }
            String addr = routeHandle.routeServer(allNodes, req.getUserId());

            RouteInfo routeInfo = RouteInfoParseUtil.parse(addr);
            return ResponseVO.successResponse(routeInfo);
        }
        return ResponseVO.errorResponse();
    }

    @PostMapping("/importUser")
    public ResponseVO importUser(@RequestBody ImportUserReq req, Integer appId) {
        req.setAppId(appId);
        return imUserService.importUser(req);
    }

    @RequestMapping("/deleteUser")
    public ResponseVO deleteUser(@RequestBody @Validated DeleteUserReq req, Integer appId) {
        req.setAppId(appId);
        return imUserService.deleteUser(req);
    }

    @RequestMapping("/getUserSequence")
    public ResponseVO getUserSequence(@RequestBody @Validated GetUserSequenceReq req, Integer appId) {
        req.setAppId(appId);
        return imUserService.getUserSequence(req);
    }
}
