package com.nineSeven.group.service;


import com.nineSeven.ResponseVO;
import com.nineSeven.group.dao.ImGroupEntity;
import com.nineSeven.group.model.req.*;

public interface ImGroupService {

    ResponseVO importGroup(ImportGroupReq req);

    ResponseVO createGroup(CreateGroupReq req);

    ResponseVO updateBaseGroupInfo(UpdateGroupReq req);

    ResponseVO getJoinedGroup(GetJoinedGroupReq req);

    ResponseVO destroyGroup(DestroyGroupReq req);

    ResponseVO transferGroup(TransferGroupReq req);

    ResponseVO<ImGroupEntity> getGroup(String groupId, Integer appId);

    public ResponseVO getGroup(GetGroupReq req);

    public ResponseVO muteGroup(MuteGroupReq req);

//    ResponseVO syncJoinedGroupList(SyncReq req);

    Long getUserGroupMaxSeq(String userId, Integer appId);
}
