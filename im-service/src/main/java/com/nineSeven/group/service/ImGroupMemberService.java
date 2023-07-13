package com.nineSeven.group.service;

import com.nineSeven.ResponseVO;
import com.nineSeven.group.model.req.*;
import com.nineSeven.group.model.resp.GetRoleInGroupResp;

import java.util.Collection;
import java.util.List;

public interface ImGroupMemberService {

    ResponseVO importGroupMember(ImportGroupMemberReq req);
    ResponseVO addGroupMember(String groupId, GroupMemberDto dto, Integer appId);

    ResponseVO addMember(AddGroupMemberReq req);

    ResponseVO<List<GroupMemberDto>> getGroupMember(String groupId, Integer appId);

    List<String> getGroupMemberId(String groupId, Integer appId);

    ResponseVO<GetRoleInGroupResp> getRoleInGroupOne(String groupId, String memberId, Integer appId);

    ResponseVO<Collection<String>> getMemberJoinedGroup(GetJoinedGroupReq req);

    ResponseVO transferGroupMember(String owner, String groupId, Integer appId);

    ResponseVO removeMember(RemoveGroupMemberReq req);

    ResponseVO removeGroupMember(String groupId, Integer appId, String memberId);

    ResponseVO updateGroupMember(UpdateGroupMemberReq req);

    ResponseVO speak(SpeakMemberReq req);

    List<GroupMemberDto> getGroupManager(String groupId, Integer appId);
}
