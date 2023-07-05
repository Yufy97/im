package com.nineSeven.friendship.service;

import com.nineSeven.ResponseVO;
import com.nineSeven.friendship.model.req.*;
import com.nineSeven.model.RequestBase;
import org.springframework.transaction.annotation.Transactional;

public interface ImFriendShipService {
    ResponseVO importFriendShip(ImportFriendShipReq req);

    ResponseVO addFriend(AddFriendReq req);

    ResponseVO updateFriend(UpdateFriendReq req);

    ResponseVO deleteFriend(DeleteFriendReq req);

    ResponseVO deleteAllFriend(DeleteFriendReq req);

    ResponseVO getAllFriendShip(GetAllFriendShipReq req);

    ResponseVO getRelation(GetRelationReq req);


    ResponseVO checkBlck(CheckFriendShipReq req);

    ResponseVO checkFriendship(CheckFriendShipReq req);

    ResponseVO addBlack(AddFriendShipBlackReq req);

    ResponseVO deleteBlack(DeleteBlackReq req);

    @Transactional
    ResponseVO doAddFriend(RequestBase req, String fromId, FriendDto dto, Integer appId);
}
