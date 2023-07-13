package com.nineSeven.friendship.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nineSeven.ResponseVO;
import com.nineSeven.enums.ApproverFriendRequestStatusEnum;
import com.nineSeven.enums.FriendShipErrorCode;
import com.nineSeven.enums.command.FriendshipEventCommand;
import com.nineSeven.exception.ApplicationException;
import com.nineSeven.friendship.dao.ImFriendShipRequestEntity;
import com.nineSeven.friendship.dao.mapper.ImFriendShipRequestMapper;
import com.nineSeven.friendship.model.req.ApproveFriendRequestReq;
import com.nineSeven.friendship.model.req.FriendDto;
import com.nineSeven.friendship.model.req.ReadFriendShipRequestReq;
import com.nineSeven.friendship.service.ImFriendShipRequestService;
import com.nineSeven.friendship.service.ImFriendShipService;
import com.nineSeven.pack.friendship.ApproverFriendRequestPack;
import com.nineSeven.pack.friendship.ReadAllFriendRequestPack;
import com.nineSeven.utils.MessageProducer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImFriendShipRequestServiceImpl implements ImFriendShipRequestService {

    @Autowired
    ImFriendShipRequestMapper imFriendShipRequestMapper;

    @Autowired
    ImFriendShipService imFriendShipService;

    @Autowired
    MessageProducer messageProducer;

    @Override
    public ResponseVO addFriendShipRequest(String fromId, FriendDto dto, Integer appId) {
        QueryWrapper<ImFriendShipRequestEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_id", appId)
                .eq("from_id", fromId)
                .eq("to_id", dto.getToId());

        ImFriendShipRequestEntity imFriendShipRequestEntity = imFriendShipRequestMapper.selectOne(queryWrapper);
        if(imFriendShipRequestEntity == null) {
            imFriendShipRequestEntity = new ImFriendShipRequestEntity();
            BeanUtils.copyProperties(dto, imFriendShipRequestEntity);
            imFriendShipRequestEntity.setAppId(appId);
            imFriendShipRequestEntity.setFromId(fromId);
            imFriendShipRequestEntity.setAddWording(dto.getAddWording());
            imFriendShipRequestEntity.setReadStatus(0);
            imFriendShipRequestEntity.setApproveStatus(0);
            imFriendShipRequestEntity.setCreateTime(System.currentTimeMillis());
            imFriendShipRequestMapper.insert(imFriendShipRequestEntity);
        } else {
            if(StringUtils.isNotBlank(dto.getAddSource())) {
                imFriendShipRequestEntity.setAddSource(dto.getAddSource());
            }
            if(StringUtils.isNotBlank(dto.getRemark())) {
                imFriendShipRequestEntity.setRemark(dto.getRemark());
            }
            if(StringUtils.isNotBlank(dto.getAddWording())) {
                imFriendShipRequestEntity.setAddWording(dto.getAddWording());
            }

            imFriendShipRequestEntity.setApproveStatus(0);
            imFriendShipRequestEntity.setReadStatus(0);
            imFriendShipRequestEntity.setCreateTime(System.currentTimeMillis());
            imFriendShipRequestMapper.updateById(imFriendShipRequestEntity);
        }

        messageProducer.sendToUser(dto.getToId(), null, "", FriendshipEventCommand.FRIEND_REQUEST, imFriendShipRequestEntity, appId);
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO approveFriendRequest(ApproveFriendRequestReq req) {
        ImFriendShipRequestEntity imFriendShipRequestEntity = imFriendShipRequestMapper.selectById(req.getId());
        if(imFriendShipRequestEntity == null) {
            throw new ApplicationException(FriendShipErrorCode.FRIEND_REQUEST_IS_NOT_EXIST);
        }

//        if(!req.getOperater().equals(imFriendShipRequestEntity.getToId())){
//            //只能审批发给自己的好友请求
//            throw new ApplicationException(FriendShipErrorCode.NOT_APPROVER_OTHER_MAN_REQUEST);
//        }

//        long seq = redisSeq.doGetSeq(req.getAppId()+":"+
//                Constants.SeqConstants.FriendshipRequest);

        ImFriendShipRequestEntity update = new ImFriendShipRequestEntity();
        update.setApproveStatus(req.getStatus());
        update.setUpdateTime(System.currentTimeMillis());
//        update.setSequence(seq);
        update.setId(req.getId());
        imFriendShipRequestMapper.updateById(update);

        if(ApproverFriendRequestStatusEnum.AGREE.getCode() == req.getStatus()) {
            FriendDto dto = new FriendDto();
            dto.setAddSource(imFriendShipRequestEntity.getAddSource());
            dto.setAddWording(imFriendShipRequestEntity.getAddWording());
            dto.setRemark(imFriendShipRequestEntity.getRemark());
            dto.setToId(imFriendShipRequestEntity.getToId());
            ResponseVO responseVO = imFriendShipService.doAddFriend(req, imFriendShipRequestEntity.getFromId(), dto, req.getAppId());
//            if(!responseVO.isOk()){
////                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//                return responseVO;
//            }
            if (!responseVO.isOk() && responseVO.getCode() != FriendShipErrorCode.TO_IS_YOUR_FRIEND.getCode()) {
                return responseVO;
            }
        }

        ApproverFriendRequestPack approverFriendRequestPack = new ApproverFriendRequestPack();
        approverFriendRequestPack.setId(req.getId());
//        approverFriendRequestPack.setSequence(seq);
        approverFriendRequestPack.setStatus(req.getStatus());
        messageProducer.sendToUser(imFriendShipRequestEntity.getToId(),req.getClientType(),req.getImei(), FriendshipEventCommand
                .FRIEND_REQUEST_APPROVER,approverFriendRequestPack,req.getAppId());
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO getFriendRequest(String fromId, Integer appId) {
        QueryWrapper<ImFriendShipRequestEntity> query = new QueryWrapper();
        query.eq("app_id", appId);
        query.eq("to_id", fromId);

        List<ImFriendShipRequestEntity> requestList = imFriendShipRequestMapper.selectList(query);

        return ResponseVO.successResponse(requestList);
    }

    @Override
    public ResponseVO readFriendShipRequestReq(ReadFriendShipRequestReq req) {
        QueryWrapper<ImFriendShipRequestEntity> query = new QueryWrapper<>();
        query.eq("app_id", req.getAppId());
        query.eq("to_id", req.getFromId());

//        long seq = redisSeq.doGetSeq(req.getAppId()+":"+ Constants.SeqConstants.FriendshipRequest);
        ImFriendShipRequestEntity update = new ImFriendShipRequestEntity();
        update.setReadStatus(1);
//        update.setSequence(seq);
        imFriendShipRequestMapper.update(update, query);
//        writeUserSeq.writeUserSeq(req.getAppId(),req.getOperater(),
//                Constants.SeqConstants.FriendshipRequest,seq);
        ReadAllFriendRequestPack readAllFriendRequestPack = new ReadAllFriendRequestPack();
        readAllFriendRequestPack.setFromId(req.getFromId());
//        readAllFriendRequestPack.setSequence(seq);
        messageProducer.sendToUser(req.getFromId(),req.getClientType(),req.getImei(),FriendshipEventCommand
                .FRIEND_REQUEST_READ,readAllFriendRequestPack,req.getAppId());

        return ResponseVO.successResponse();
    }
}
