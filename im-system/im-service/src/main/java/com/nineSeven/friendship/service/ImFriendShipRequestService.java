package com.nineSeven.friendship.service;

import com.nineSeven.ResponseVO;
import com.nineSeven.friendship.model.req.ApproveFriendRequestReq;
import com.nineSeven.friendship.model.req.FriendDto;
import com.nineSeven.friendship.model.req.ReadFriendShipRequestReq;

public interface ImFriendShipRequestService {
    ResponseVO addFriendShipRequest(String fromId, FriendDto dto, Integer appId);

    ResponseVO approveFriendRequest(ApproveFriendRequestReq req);

    ResponseVO getFriendRequest(String fromId, Integer appId);

    ResponseVO readFriendShipRequestReq(ReadFriendShipRequestReq req);
}
