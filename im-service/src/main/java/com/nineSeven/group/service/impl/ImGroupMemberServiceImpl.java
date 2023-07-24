package com.nineSeven.group.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nineSeven.ResponseVO;
import com.nineSeven.config.AppConfig;
import com.nineSeven.constant.Constants;
import com.nineSeven.enums.GroupErrorCode;
import com.nineSeven.enums.GroupMemberRoleEnum;
import com.nineSeven.enums.GroupStatusEnum;
import com.nineSeven.enums.GroupTypeEnum;
import com.nineSeven.exception.ApplicationException;
import com.nineSeven.group.dao.ImGroupEntity;
import com.nineSeven.group.dao.ImGroupMemberEntity;
import com.nineSeven.group.dao.mapper.ImGroupMemberMapper;
import com.nineSeven.group.model.callback.AddMemberAfterCallback;
import com.nineSeven.group.model.req.*;
import com.nineSeven.group.model.resp.AddMemberResp;
import com.nineSeven.group.model.resp.GetRoleInGroupResp;
import com.nineSeven.group.service.ImGroupMemberService;
import com.nineSeven.group.service.ImGroupService;
import com.nineSeven.user.dao.ImUserDataEntity;
import com.nineSeven.user.service.ImUserService;
import com.nineSeven.utils.CallbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ImGroupMemberServiceImpl implements ImGroupMemberService {
    @Autowired
    ImUserService imUserService;

    @Autowired
    ImGroupMemberMapper imGroupMemberMapper;

    @Autowired
    ImGroupService imGroupService;

    @Autowired
    AppConfig appConfig;

    @Autowired
    CallbackService callbackService;


    @Override
    public ResponseVO importGroupMember(ImportGroupMemberReq req) {
        List<AddMemberResp> resp = new ArrayList<>();

        ResponseVO<ImGroupEntity> groupResp = imGroupService.getGroup(req.getGroupId(), req.getAppId());
        if (!groupResp.isOk()) {
            return groupResp;
        }

        for (GroupMemberDto memberId :
                req.getMembers()) {
            ResponseVO responseVO = null;
            try {
                responseVO = this.addGroupMember(req.getGroupId(), memberId, req.getAppId());
            } catch (Exception e) {
                e.printStackTrace();
                responseVO = ResponseVO.errorResponse();
            }
            AddMemberResp addMemberResp = new AddMemberResp();
            addMemberResp.setMemberId(memberId.getMemberId());
            if (responseVO.isOk()) {
                addMemberResp.setResult(0);
            } else if (responseVO.getCode() == GroupErrorCode.USER_IS_JOINED_GROUP.getCode()) {
                addMemberResp.setResult(2);
            } else {
                addMemberResp.setResult(1);
            }
            resp.add(addMemberResp);
        }

        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO addGroupMember(String groupId, GroupMemberDto dto, Integer appId) {
        ResponseVO<ImUserDataEntity> singleUserInfo = imUserService.getSingleUserInfo(dto.getMemberId(), appId);
        if (!singleUserInfo.isOk()) {
            return singleUserInfo;
        }

        if (dto.getRole() != null && GroupMemberRoleEnum.OWNER.getCode() == dto.getRole()) {
            QueryWrapper<ImGroupMemberEntity> queryOwner = new QueryWrapper<>();
            queryOwner.eq("group_id", groupId);
            queryOwner.eq("app_id", appId);
            queryOwner.eq("role", GroupMemberRoleEnum.OWNER.getCode());
            Integer ownerNum = imGroupMemberMapper.selectCount(queryOwner);
            if (ownerNum > 0) {
                return ResponseVO.errorResponse(GroupErrorCode.GROUP_IS_HAVE_OWNER);
            }
        }


        QueryWrapper<ImGroupMemberEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("group_id", groupId)
                .eq("app_id", appId)
                .eq("member_id", dto.getMemberId());

        ImGroupMemberEntity imGroupMemberEntity = imGroupMemberMapper.selectOne(queryWrapper);
        if (imGroupMemberEntity == null) {
            imGroupMemberEntity = new ImGroupMemberEntity();
            BeanUtils.copyProperties(dto, imGroupMemberEntity);
            imGroupMemberEntity.setGroupId(groupId);
            imGroupMemberEntity.setAppId(appId);
            imGroupMemberEntity.setJoinTime(System.currentTimeMillis());
            int insert = imGroupMemberMapper.insert(imGroupMemberEntity);
            if (insert == 1) {
                return ResponseVO.successResponse();
            }
            return ResponseVO.errorResponse(GroupErrorCode.USER_JOIN_GROUP_ERROR);
        } else if (GroupMemberRoleEnum.LEAVE.getCode() == imGroupMemberEntity.getRole()) {
            imGroupMemberEntity = new ImGroupMemberEntity();
            BeanUtils.copyProperties(dto, imGroupMemberEntity);
            imGroupMemberEntity.setJoinTime(System.currentTimeMillis());
            int update = imGroupMemberMapper.update(imGroupMemberEntity, queryWrapper);
            if (update == 1) {
                return ResponseVO.successResponse();
            }
            return ResponseVO.errorResponse(GroupErrorCode.USER_JOIN_GROUP_ERROR);
        }
        return ResponseVO.errorResponse(GroupErrorCode.USER_IS_JOINED_GROUP);
    }

    @Override
    public ResponseVO addMember(AddGroupMemberReq req) {
        List<AddMemberResp> resp = new ArrayList<>();

        boolean isAdmin = false;
        ResponseVO<ImGroupEntity> groupResp = imGroupService.getGroup(req.getGroupId(), req.getAppId());
        if (!groupResp.isOk()) {
            return groupResp;
        }

        List<GroupMemberDto> memberDtos = req.getMembers();

        if(appConfig.isAddGroupMemberBeforeCallback()){

            ResponseVO responseVO = callbackService.beforeCallback(req.getAppId(), Constants.CallbackCommand.GroupMemberAddBefore, JSONObject.toJSONString(req));
            if(!responseVO.isOk()){
                return responseVO;
            }

            try {
                memberDtos = JSONArray.parseArray(JSONObject.toJSONString(responseVO.getData()), GroupMemberDto.class);
            }catch (Exception e){
                e.printStackTrace();
                log.error("GroupMemberAddBefore 回调失败：{}",req.getAppId());
            }
        }
        ImGroupEntity group = groupResp.getData();

        if (!isAdmin && GroupTypeEnum.PUBLIC.getCode() == group.getGroupType()) {
            throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_APPMANAGER_ROLE);
        }

        List<String> successId = new ArrayList<>();
        for (GroupMemberDto memberId : memberDtos) {
            ResponseVO responseVO = null;
            try {
                responseVO = this.addGroupMember(req.getGroupId(), memberId, req.getAppId());
            } catch (Exception e) {
                e.printStackTrace();
                responseVO = ResponseVO.errorResponse();
            }
            AddMemberResp addMemberResp = new AddMemberResp();
            addMemberResp.setMemberId(memberId.getMemberId());
            if (responseVO.isOk()) {
                successId.add(memberId.getMemberId());
                addMemberResp.setResult(0);
            } else if (responseVO.getCode() == GroupErrorCode.USER_IS_JOINED_GROUP.getCode()) {
                addMemberResp.setResult(2);
                addMemberResp.setResultMessage(responseVO.getMsg());
            } else {
                addMemberResp.setResult(1);
                addMemberResp.setResultMessage(responseVO.getMsg());
            }
            resp.add(addMemberResp);
        }

//        AddGroupMemberPack addGroupMemberPack = new AddGroupMemberPack();
//        addGroupMemberPack.setGroupId(req.getGroupId());
//        addGroupMemberPack.setMembers(successId);
//        groupMessageProducer.producer(req.getOperater(), GroupEventCommand.ADDED_MEMBER, addGroupMemberPack
//                , new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));
//
        if(appConfig.isAddGroupMemberAfterCallback()){
            AddMemberAfterCallback dto = new AddMemberAfterCallback();
            dto.setGroupId(req.getGroupId());
            dto.setGroupType(group.getGroupType());
            dto.setMemberId(resp);
            dto.setOperater(req.getOperater());
            callbackService.callback(req.getAppId(), Constants.CallbackCommand.GroupMemberAddAfter, JSONObject.toJSONString(dto));
        }
        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO<List<GroupMemberDto>> getGroupMember(String groupId, Integer appId) {
        return ResponseVO.successResponse(imGroupMemberMapper.getGroupMember(appId, groupId));
    }

    @Override
    public List<String> getGroupMemberId(String groupId, Integer appId) {
        return imGroupMemberMapper.getGroupMemberId(appId, groupId);
    }

    @Override
    public ResponseVO<GetRoleInGroupResp> getRoleInGroupOne(String groupId, String memberId, Integer appId) {
        GetRoleInGroupResp resp = new GetRoleInGroupResp();

        QueryWrapper<ImGroupMemberEntity> queryOwner = new QueryWrapper<>();
        queryOwner.eq("group_id", groupId);
        queryOwner.eq("app_id", appId);
        queryOwner.eq("member_id", memberId);

        ImGroupMemberEntity imGroupMemberEntity = imGroupMemberMapper.selectOne(queryOwner);
        if (imGroupMemberEntity == null || imGroupMemberEntity.getRole() == GroupMemberRoleEnum.LEAVE.getCode()) {
            return ResponseVO.errorResponse(GroupErrorCode.MEMBER_IS_NOT_JOINED_GROUP);
        }

        BeanUtils.copyProperties(imGroupMemberEntity, resp);
        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO<Collection<String>> getMemberJoinedGroup(GetJoinedGroupReq req) {
        QueryWrapper<ImGroupMemberEntity> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("app_id", req.getAppId())
                .eq("member_id", req.getMemberId());

        if (req.getLimit() != null) {
            Page<ImGroupMemberEntity> page = new Page<>(req.getOffset(), req.getLimit());
            Page<ImGroupMemberEntity> result = imGroupMemberMapper.selectPage(page, queryWrapper);

            Set<String> groupIds = result.getRecords().stream().map(ImGroupMemberEntity::getGroupId).collect(Collectors.toSet());
            return ResponseVO.successResponse(groupIds);
        }
        return ResponseVO.successResponse(imGroupMemberMapper.getJoinedGroupId(req.getAppId(), req.getMemberId()));
    }

    @Override
    public ResponseVO transferGroupMember(String owner, String groupId, Integer appId) {
        //更新旧群主
        ImGroupMemberEntity imGroupMemberEntity = new ImGroupMemberEntity();
        imGroupMemberEntity.setRole(GroupMemberRoleEnum.ORDINARY.getCode());
        UpdateWrapper<ImGroupMemberEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("app_id", appId);
        updateWrapper.eq("group_id", groupId);
        updateWrapper.eq("role", GroupMemberRoleEnum.OWNER.getCode());
        imGroupMemberMapper.update(imGroupMemberEntity, updateWrapper);

        //更新新群主
        ImGroupMemberEntity newOwner = new ImGroupMemberEntity();
        newOwner.setRole(GroupMemberRoleEnum.OWNER.getCode());
        UpdateWrapper<ImGroupMemberEntity> ownerWrapper = new UpdateWrapper<>();
        ownerWrapper.eq("app_id", appId);
        ownerWrapper.eq("group_id", groupId);
        ownerWrapper.eq("member_id", owner);
        imGroupMemberMapper.update(newOwner, ownerWrapper);

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO removeMember(RemoveGroupMemberReq req) {
        boolean isAdmin = false;
        ResponseVO<ImGroupEntity> groupResp = imGroupService.getGroup(req.getGroupId(), req.getAppId());
        if (!groupResp.isOk()) {
            return groupResp;
        }

        ImGroupEntity group = groupResp.getData();
        if (!isAdmin) {

            //获取操作人的权限 是管理员or群主or群成员
            ResponseVO<GetRoleInGroupResp> role = getRoleInGroupOne(req.getGroupId(), req.getOperater(), req.getAppId());
            if (!role.isOk()) {
                return role;
            }

            GetRoleInGroupResp data = role.getData();
            Integer roleInfo = data.getRole();

            boolean isOwner = roleInfo == GroupMemberRoleEnum.OWNER.getCode();
            boolean isManager = roleInfo == GroupMemberRoleEnum.MANAGER.getCode();

            if (!isOwner && !isManager) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }

            if (!isOwner && GroupTypeEnum.PRIVATE.getCode() == group.getGroupType()) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
            }

            ResponseVO<GetRoleInGroupResp> roleInGroupOne = getRoleInGroupOne(req.getGroupId(), req.getMemberId(), req.getAppId());
            if (!roleInGroupOne.isOk()) {
                return roleInGroupOne;
            }

            Integer role1 = roleInGroupOne.getData().getRole();
            if (role1 == GroupMemberRoleEnum.OWNER.getCode()) {
                throw new ApplicationException(GroupErrorCode.GROUP_OWNER_IS_NOT_REMOVE);
            }
            if (!isOwner && GroupMemberRoleEnum.ORDINARY.getCode() != role1) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
            }
        }
        if(appConfig.isDeleteGroupMemberAfterCallback()){
            callbackService.callback(req.getAppId(), Constants.CallbackCommand.GroupMemberDeleteAfter, JSONObject.toJSONString(req));
        }
        return this.removeGroupMember(req.getGroupId(), req.getAppId(), req.getMemberId());
    }

    @Override
    public ResponseVO removeGroupMember(String groupId, Integer appId, String memberId) {
        ResponseVO<ImUserDataEntity> singleUserInfo = imUserService.getSingleUserInfo(memberId, appId);
        if(!singleUserInfo.isOk()){
            return singleUserInfo;
        }

        ResponseVO<GetRoleInGroupResp> roleInGroupOne = getRoleInGroupOne(groupId, memberId, appId);
        if (!roleInGroupOne.isOk()) {
            return roleInGroupOne;
        }

        GetRoleInGroupResp data = roleInGroupOne.getData();
        ImGroupMemberEntity imGroupMemberEntity = new ImGroupMemberEntity();
        imGroupMemberEntity.setRole(GroupMemberRoleEnum.LEAVE.getCode());
        imGroupMemberEntity.setLeaveTime(System.currentTimeMillis());
        imGroupMemberEntity.setGroupMemberId(data.getGroupMemberId());
        imGroupMemberMapper.updateById(imGroupMemberEntity);
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO updateGroupMember(UpdateGroupMemberReq req) {
        boolean isAdmin = false;

        ResponseVO<ImGroupEntity> groupResp = imGroupService.getGroup(req.getGroupId(), req.getAppId());
        if(!groupResp.isOk()) {
            return groupResp;
        }

        ImGroupEntity group = groupResp.getData();
        if (group.getStatus() == GroupStatusEnum.DESTROY.getCode()) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_DESTROY);
        }

        if(!isAdmin) {
            if(!StringUtils.isEmpty(req.getAlias()) && !req.getOperater().equals(req.getMemberId())) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_ONESELF);
            }

            if(group.getGroupType() == GroupTypeEnum.PRIVATE.getCode() && req.getRole() != null
                    && (req.getRole() == GroupMemberRoleEnum.MANAGER.getCode() || req.getRole() == GroupMemberRoleEnum.OWNER.getCode())) {
                return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }

            if(req.getRole() != null) {
                ResponseVO<GetRoleInGroupResp> roleInGroupOne = this.getRoleInGroupOne(req.getGroupId(), req.getOperater(), req.getAppId());
                if(!roleInGroupOne.isOk()) {
                    return roleInGroupOne;
                }
                if(roleInGroupOne.getData().getRole() != GroupMemberRoleEnum.OWNER.getCode()) {
                    return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
                }
            }
        }
        ImGroupMemberEntity update = new ImGroupMemberEntity();

        update.setAlias(req.getAlias());
        update.setAlias(req.getExtra());
        if(req.getRole() != null && req.getRole() != GroupMemberRoleEnum.OWNER.getCode()){
            update.setRole(req.getRole());
        }
        UpdateWrapper<ImGroupMemberEntity> objectUpdateWrapper = new UpdateWrapper<>();
        objectUpdateWrapper.eq("app_id", req.getAppId())
                            .eq("member_id", req.getMemberId())
                            .eq("group_id", req.getGroupId());
        imGroupMemberMapper.update(update,objectUpdateWrapper);
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO speak(SpeakMemberReq req) {
        ResponseVO<ImGroupEntity> groupResp = imGroupService.getGroup(req.getGroupId(), req.getAppId());
        if (!groupResp.isOk()) {
            return groupResp;
        }

        boolean isAdmin = false;
        boolean isOwner = false;
        boolean isManager = false;
        GetRoleInGroupResp memberRole = null;

        if (!isAdmin) {

            //获取操作人的权限 是管理员or群主or群成员
            ResponseVO<GetRoleInGroupResp> role = getRoleInGroupOne(req.getGroupId(), req.getOperater(), req.getAppId());
            if (!role.isOk()) {
                return role;
            }

            GetRoleInGroupResp data = role.getData();
            Integer roleInfo = data.getRole();

            isOwner = roleInfo == GroupMemberRoleEnum.OWNER.getCode();
            isManager = roleInfo == GroupMemberRoleEnum.MANAGER.getCode();

            if (!isOwner && !isManager) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }

            //获取被操作的权限
            ResponseVO<GetRoleInGroupResp> roleInGroupOne = this.getRoleInGroupOne(req.getGroupId(), req.getMemberId(), req.getAppId());
            if (!roleInGroupOne.isOk()) {
                return roleInGroupOne;
            }
            memberRole = roleInGroupOne.getData();
            //被操作人是群主只能app管理员操作
            if (memberRole.getRole() == GroupMemberRoleEnum.OWNER.getCode()) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_APPMANAGER_ROLE);
            }

            //是管理员并且被操作人不是群成员，无法操作
            if (isManager && memberRole.getRole() != GroupMemberRoleEnum.ORDINARY.getCode()) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
            }
        }

        ImGroupMemberEntity imGroupMemberEntity = new ImGroupMemberEntity();

        imGroupMemberEntity.setGroupMemberId(memberRole.getGroupMemberId());
        if(req.getSpeakDate() > 0){
            imGroupMemberEntity.setSpeakDate(System.currentTimeMillis() + req.getSpeakDate());
        }else{
            imGroupMemberEntity.setSpeakDate(req.getSpeakDate());
        }

        int i = imGroupMemberMapper.updateById(imGroupMemberEntity);
//        if(i == 1){
//            GroupMemberSpeakPack pack = new GroupMemberSpeakPack();
//            BeanUtils.copyProperties(req,pack);
//            groupMessageProducer.producer(req.getOperater(),GroupEventCommand.SPEAK_GOUP_MEMBER,pack,
//                    new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));
//        }
        return ResponseVO.successResponse();
    }

    @Override
    public List<GroupMemberDto> getGroupManager(String groupId, Integer appId) {
        return imGroupMemberMapper.getGroupManager(groupId, appId);
    }

    @Override
    public ResponseVO<Collection<String>> syncMemberJoinedGroup(String operater, Integer appId) {
        return ResponseVO.successResponse(imGroupMemberMapper.syncJoinedGroupId(appId,operater,GroupMemberRoleEnum.LEAVE.getCode()));
    }
}
