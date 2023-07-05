package com.nineSeven.friendship.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nineSeven.ResponseVO;
import com.nineSeven.enums.DelFlagEnum;
import com.nineSeven.enums.FriendShipErrorCode;
import com.nineSeven.friendship.dao.ImFriendShipGroupEntity;
import com.nineSeven.friendship.dao.mapper.ImFriendShipGroupMapper;
import com.nineSeven.friendship.model.req.AddFriendShipGroupMemberReq;
import com.nineSeven.friendship.model.req.AddFriendShipGroupReq;
import com.nineSeven.friendship.model.req.DeleteFriendShipGroupReq;
import com.nineSeven.friendship.service.ImFriendShipGroupMemberService;
import com.nineSeven.friendship.service.ImFriendShipGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImFriendShipGroupServiceImpl implements ImFriendShipGroupService {

    @Autowired
    ImFriendShipGroupMapper imFriendShipGroupMapper;

    @Autowired
    ImFriendShipGroupMemberService imFriendShipGroupMemberService;


    @Override
    @Transactional
    public ResponseVO addGroup(AddFriendShipGroupReq req) {

        QueryWrapper<ImFriendShipGroupEntity> query = new QueryWrapper<>();
        query.eq("group_name", req.getGroupName());
        query.eq("app_id", req.getAppId());
        query.eq("from_id", req.getFromId());
        query.eq("del_flag", DelFlagEnum.NORMAL.getCode());

        ImFriendShipGroupEntity entity = imFriendShipGroupMapper.selectOne(query);

        if (entity != null) {
            return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_SHIP_GROUP_IS_EXIST);
        }


        ImFriendShipGroupEntity insert = new ImFriendShipGroupEntity();
        insert.setAppId(req.getAppId());
        insert.setCreateTime(System.currentTimeMillis());
        insert.setDelFlag(DelFlagEnum.NORMAL.getCode());
        insert.setGroupName(req.getGroupName());
//        long seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FriendshipGroup);
//        insert.setSequence(seq);
        insert.setFromId(req.getFromId());
        try {
            int insert1 = imFriendShipGroupMapper.insert(insert);

            if (insert1 != 1) {
                return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_SHIP_GROUP_CREATE_ERROR);
            }
            if (CollectionUtil.isNotEmpty(req.getToIds())) {
                AddFriendShipGroupMemberReq addFriendShipGroupMemberReq = new AddFriendShipGroupMemberReq();
                addFriendShipGroupMemberReq.setFromId(req.getFromId());
                addFriendShipGroupMemberReq.setGroupName(req.getGroupName());
                addFriendShipGroupMemberReq.setToIds(req.getToIds());
                addFriendShipGroupMemberReq.setAppId(req.getAppId());
                imFriendShipGroupMemberService.addGroupMember(addFriendShipGroupMemberReq);
                return ResponseVO.successResponse();
            }
        } catch (DuplicateKeyException e) {
            e.getStackTrace();
            return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_SHIP_GROUP_IS_EXIST);
        }

//        AddFriendGroupPack addFriendGropPack = new AddFriendGroupPack();
//        addFriendGropPack.setFromId(req.getFromId());
//        addFriendGropPack.setGroupName(req.getGroupName());
//        addFriendGropPack.setSequence(seq);
//        messageProducer.sendToUserExceptClient(req.getFromId(), FriendshipEventCommand.FRIEND_GROUP_ADD,
//                addFriendGropPack,new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));
//        //写入seq
//        writeUserSeq.writeUserSeq(req.getAppId(), req.getFromId(), Constants.SeqConstants.FriendshipGroup, seq);

        return ResponseVO.successResponse();
    }

    @Override
    @Transactional
    public ResponseVO deleteGroup(DeleteFriendShipGroupReq req) {

        for (String groupName : req.getGroupName()) {
            QueryWrapper<ImFriendShipGroupEntity> query = new QueryWrapper<>();
            query.eq("group_name", groupName);
            query.eq("app_id", req.getAppId());
            query.eq("from_id", req.getFromId());
            query.eq("del_flag", DelFlagEnum.NORMAL.getCode());

            ImFriendShipGroupEntity entity = imFriendShipGroupMapper.selectOne(query);

            if (entity != null) {
//                long seq = redisSeq.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FriendshipGroup);
//                update.setSequence(seq);

                imFriendShipGroupMemberService.clearGroupMember(entity.getGroupId());
                imFriendShipGroupMapper.delete(query);
//                DeleteFriendGroupPack deleteFriendGroupPack = new DeleteFriendGroupPack();
//                deleteFriendGroupPack.setFromId(req.getFromId());
//                deleteFriendGroupPack.setGroupName(groupName);
//                deleteFriendGroupPack.setSequence(seq);
//                //TCP通知
//                messageProducer.sendToUserExceptClient(req.getFromId(), FriendshipEventCommand.FRIEND_GROUP_DELETE,
//                        deleteFriendGroupPack,new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));
//                //写入seq
//                writeUserSeq.writeUserSeq(req.getAppId(), req.getFromId(), Constants.SeqConstants.FriendshipGroup, seq);
            }
        }
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO getGroup(String fromId, String groupName, Integer appId) {
        QueryWrapper<ImFriendShipGroupEntity> query = new QueryWrapper<>();
        query.eq("group_name", groupName);
        query.eq("app_id", appId);
        query.eq("from_id", fromId);
        query.eq("del_flag", DelFlagEnum.NORMAL.getCode());

        ImFriendShipGroupEntity entity = imFriendShipGroupMapper.selectOne(query);
        if (entity == null) {
            return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_SHIP_GROUP_IS_NOT_EXIST);
        }
        return ResponseVO.successResponse(entity);
    }

    @Override
    public Long updateSeq(String fromId, String groupName, Integer appId) {
        QueryWrapper<ImFriendShipGroupEntity> query = new QueryWrapper<>();
        query.eq("group_name", groupName);
        query.eq("app_id", appId);
        query.eq("from_id", fromId);

        ImFriendShipGroupEntity entity = imFriendShipGroupMapper.selectOne(query);

//        long seq = redisSeq.doGetSeq(appId + ":" + Constants.SeqConstants.FriendshipGroup);

        ImFriendShipGroupEntity group = new ImFriendShipGroupEntity();
        group.setGroupId(entity.getGroupId());
//        group.setSequence(seq);
        imFriendShipGroupMapper.updateById(group);
        return null;
    }

}
