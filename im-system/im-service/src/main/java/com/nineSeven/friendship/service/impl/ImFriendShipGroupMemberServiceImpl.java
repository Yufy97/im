package com.nineSeven.friendship.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nineSeven.ResponseVO;
import com.nineSeven.friendship.dao.ImFriendShipGroupEntity;
import com.nineSeven.friendship.dao.ImFriendShipGroupMemberEntity;
import com.nineSeven.friendship.dao.mapper.ImFriendShipGroupMemberMapper;
import com.nineSeven.friendship.model.req.AddFriendShipGroupMemberReq;
import com.nineSeven.friendship.model.req.DeleteFriendShipGroupMemberReq;
import com.nineSeven.friendship.service.ImFriendShipGroupMemberService;
import com.nineSeven.friendship.service.ImFriendShipGroupService;
import com.nineSeven.user.dao.ImUserDataEntity;
import com.nineSeven.user.service.ImUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ImFriendShipGroupMemberServiceImpl implements ImFriendShipGroupMemberService {

    @Autowired
    ImFriendShipGroupMemberMapper imFriendShipGroupMemberMapper;

    @Autowired
    ImFriendShipGroupService imFriendShipGroupService;

    @Autowired
    ImUserService imUserService;

    @Override
    public ResponseVO addGroupMember(AddFriendShipGroupMemberReq req) {
        ResponseVO<ImFriendShipGroupEntity> group = imFriendShipGroupService.getGroup(req.getFromId(), req.getGroupName(), req.getAppId());
        if(!group.isOk()) {
            return group;
        }

        List<String> success = new ArrayList<>();
         for(String toId: req.getToIds()) {
            ResponseVO<ImUserDataEntity> singleUserInfo = imUserService.getSingleUserInfo(toId, req.getAppId());
            if (singleUserInfo.isOk()) {
                int isSuccess = this.doAddGroupMember(group.getData().getGroupId(), toId);
                if(isSuccess == 1) {
                    success.add(toId);
                }
            }
        }
        return ResponseVO.successResponse(success);
    }

    @Override
    public ResponseVO delGroupMember(DeleteFriendShipGroupMemberReq req) {
        ResponseVO<ImFriendShipGroupEntity> group = imFriendShipGroupService.getGroup(req.getFromId(),req.getGroupName(),req.getAppId());
        if(!group.isOk()){
            return group;
        }
        List<String> successId = new ArrayList<>();
        for (String toId : req.getToIds()) {
            ResponseVO<ImUserDataEntity> singleUserInfo = imUserService.getSingleUserInfo(toId, req.getAppId());
            if(singleUserInfo.isOk()){
                int i = deleteGroupMember(group.getData().getGroupId(), toId);
                if(i == 1){
                    successId.add(toId);
                }
            }
        }
        return ResponseVO.successResponse(successId);
    }

    public int deleteGroupMember(Long groupId, String toId) {
        QueryWrapper<ImFriendShipGroupMemberEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("group_id", groupId);
        queryWrapper.eq("to_id",toId);

        return imFriendShipGroupMemberMapper.delete(queryWrapper);
    }

    @Override
    public int doAddGroupMember(Long groupId, String toId) {
        return imFriendShipGroupMemberMapper.insert(new ImFriendShipGroupMemberEntity(groupId, toId));
    }

    @Override
    public int clearGroupMember(Long groupId) {
        QueryWrapper<ImFriendShipGroupMemberEntity> query = new QueryWrapper<>();
        query.eq("group_id",groupId);
        return imFriendShipGroupMemberMapper.delete(query);
    }
}
