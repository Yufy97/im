package com.nineSeven.group.service;


import com.nineSeven.ResponseVO;
import com.nineSeven.group.dao.ImGroupEntity;
import com.nineSeven.group.model.req.*;
import com.nineSeven.model.SyncReq;

public interface ImGroupService {

    ResponseVO importGroup(ImportGroupReq req);

    ResponseVO createGroup(CreateGroupReq req);

    ResponseVO updateBaseGroupInfo(UpdateGroupReq req);

    ResponseVO getJoinedGroup(GetJoinedGroupReq req);

    ResponseVO destroyGroup(DestroyGroupReq req);

    ResponseVO transferGroup(TransferGroupReq req);

    ResponseVO<ImGroupEntity> getGroup(String groupId, Integer appId);

    ResponseVO getGroup(GetGroupReq req);

    ResponseVO muteGroup(MuteGroupReq req);

//    ResponseVO syncJoinedGroupList(SyncReq req);

    Long getUserGroupMaxSeq(String userId, Integer appId);

    ResponseVO syncJoinedGroupList(SyncReq req);
}
