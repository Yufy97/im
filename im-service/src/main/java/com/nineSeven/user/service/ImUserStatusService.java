package com.nineSeven.user.service;

import com.nineSeven.user.model.UserStatusChangeNotifyContent;
import com.nineSeven.user.model.req.SubscribeUserOnlineStatusReq;

public interface ImUserStatusService {
    void processUserOnlineStatusNotify(UserStatusChangeNotifyContent content);

    void subscribeUserOnlineStatus(SubscribeUserOnlineStatusReq req);
}
