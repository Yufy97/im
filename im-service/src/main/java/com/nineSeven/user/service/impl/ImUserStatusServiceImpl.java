package com.nineSeven.user.service.impl;

import com.nineSeven.enums.command.UserEventCommand;
import com.nineSeven.friendship.service.ImFriendShipService;
import com.nineSeven.model.ClientInfo;
import com.nineSeven.pack.user.UserStatusChangeNotifyPack;
import com.nineSeven.user.model.UserStatusChangeNotifyContent;
import com.nineSeven.user.model.req.SubscribeUserOnlineStatusReq;
import com.nineSeven.user.service.ImUserStatusService;
import com.nineSeven.utils.MessageProducer;
import com.nineSeven.utils.UserSessionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImUserStatusServiceImpl implements ImUserStatusService {

    @Autowired
    UserSessionUtils userSessionUtils;

    @Autowired
    MessageProducer messageProducer;

    @Autowired
    ImFriendShipService imFriendShipService;

    @Override
    public void processUserOnlineStatusNotify(UserStatusChangeNotifyContent content) {
        UserStatusChangeNotifyPack pack = new UserStatusChangeNotifyPack();
        BeanUtils.copyProperties(content, pack);
        pack.setClient(userSessionUtils.getUserSession(content.getAppId(), content.getUserId()));

        //发送给其他端
        messageProducer.sendToUserExceptClient(content.getUserId(), UserEventCommand.USER_ONLINE_STATUS_CHANGE_NOTIFY_SYNC, pack, content);

        //发送给好友
        dispatcher(pack.getAppId(), pack.getUserId(), content);
    }

    @Override
    public void subscribeUserOnlineStatus(SubscribeUserOnlineStatusReq req) {

    }

    private void dispatcher(Integer appId, String userId, ClientInfo content) {
        List<String> ids = imFriendShipService.getAllFriendId(appId, userId);

        for (String id : ids) {
            messageProducer.sendToUser(id, UserEventCommand.USER_ONLINE_STATUS_CHANGE_NOTIFY, content, appId);
        }
    }
}
