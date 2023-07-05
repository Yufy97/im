package com.nineSeven.friendship.service;


import com.nineSeven.ResponseVO;
import com.nineSeven.friendship.dao.ImFriendShipGroupEntity;
import com.nineSeven.friendship.model.req.AddFriendShipGroupReq;
import com.nineSeven.friendship.model.req.DeleteFriendShipGroupReq;

/**
 * @author: Chackylee
 * @description:
 **/
public interface ImFriendShipGroupService {

    ResponseVO addGroup(AddFriendShipGroupReq req);

    ResponseVO deleteGroup(DeleteFriendShipGroupReq req);

    ResponseVO<ImFriendShipGroupEntity> getGroup(String fromId, String groupName, Integer appId);

    Long updateSeq(String fromId, String groupName, Integer appId);
}
