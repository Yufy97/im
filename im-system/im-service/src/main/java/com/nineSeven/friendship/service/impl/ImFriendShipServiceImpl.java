package com.nineSeven.friendship.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.nineSeven.ResponseVO;
import com.nineSeven.enums.AllowFriendTypeEnum;
import com.nineSeven.enums.CheckFriendShipTypeEnum;
import com.nineSeven.enums.FriendShipErrorCode;
import com.nineSeven.enums.FriendShipStatusEnum;
import com.nineSeven.exception.ApplicationException;
import com.nineSeven.friendship.dao.ImFriendShipEntity;
import com.nineSeven.friendship.dao.mapper.ImFriendShipMapper;
import com.nineSeven.friendship.model.req.*;
import com.nineSeven.friendship.model.resp.CheckFriendShipResp;
import com.nineSeven.friendship.model.resp.ImportFriendShipResp;
import com.nineSeven.friendship.service.ImFriendShipRequestService;
import com.nineSeven.friendship.service.ImFriendShipService;
import com.nineSeven.model.RequestBase;
import com.nineSeven.user.dao.ImUserDataEntity;
import com.nineSeven.user.service.ImUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ImFriendShipServiceImpl implements ImFriendShipService {

    @Autowired
    ImFriendShipMapper imFriendShipMapper;

    @Autowired
    ImUserService imUserService;

    @Autowired
    ImFriendShipRequestService imFriendShipRequestService;

    @Override
    public ResponseVO importFriendShip(ImportFriendShipReq req) {
        if(req.getFriendItem().size() > 100) {
            // todo
        }
        ImportFriendShipResp resp = new ImportFriendShipResp();
        List<String> successId = new ArrayList<>();
        List<String> errorId = new ArrayList<>();

        for (ImportFriendShipReq.ImportFriendDto dto: req.getFriendItem()) {
            ImFriendShipEntity entity = new ImFriendShipEntity();
            BeanUtils.copyProperties(dto,entity);
            entity.setAppId(req.getAppId());
            entity.setFromId(req.getFromId());
            try {
                int insert = imFriendShipMapper.insert(entity);
                if(insert == 1){
                    successId.add(dto.getToId());
                }else{
                    errorId.add(dto.getToId());
                }
            }catch (Exception e){
                e.printStackTrace();
                errorId.add(dto.getToId());
            }

        }

        resp.setErrorId(errorId);
        resp.setSuccessId(successId);

        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO addFriend(AddFriendReq req) {
        ResponseVO<ImUserDataEntity> fromInfo = imUserService.getSingleUserInfo(req.getFromId(), req.getAppId());
        if(!fromInfo.isOk()){
            return fromInfo;
        }

        ResponseVO<ImUserDataEntity> toInfo = imUserService.getSingleUserInfo(req.getToItem().getToId(), req.getAppId());
        if(!toInfo.isOk()){
            return toInfo;
        }

        ImUserDataEntity data = toInfo.getData();

        if(data.getFriendAllowType() != null && data.getFriendAllowType() == AllowFriendTypeEnum.NOT_NEED.getCode()) {
            return this.doAddFriend(req, req.getFromId(), req.getToItem(), req.getAppId());
        } else {
            imFriendShipRequestService.addFriendShipRequest(req.getFromId(), req.getToItem(), req.getAppId());
        }
        return ResponseVO.errorResponse(FriendShipErrorCode.ADD_FRIEND_ERROR);
    }

    @Override
    public ResponseVO updateFriend(UpdateFriendReq req) {

        ResponseVO<ImUserDataEntity> fromInfo = imUserService.getSingleUserInfo(req.getFromId(), req.getAppId());
        if(!fromInfo.isOk()){
            return fromInfo;
        }

        ResponseVO<ImUserDataEntity> toInfo = imUserService.getSingleUserInfo(req.getToItem().getToId(), req.getAppId());
        if(!toInfo.isOk()){
            return toInfo;
        }

        ResponseVO responseVO = this.doUpdate(req.getFromId(), req.getToItem(), req.getAppId());
//        if(responseVO.isOk()){
//            UpdateFriendPack updateFriendPack = new UpdateFriendPack();
//            updateFriendPack.setRemark(req.getToItem().getRemark());
//            updateFriendPack.setToId(req.getToItem().getToId());
//            messageProducer.sendToUser(req.getFromId(),
//                    req.getClientType(),req.getImei(),FriendshipEventCommand
//                            .FRIEND_UPDATE,updateFriendPack,req.getAppId());
//
//            if (appConfig.isModifyFriendAfterCallback()) {
//                AddFriendAfterCallbackDto callbackDto = new AddFriendAfterCallbackDto();
//                callbackDto.setFromId(req.getFromId());
//                callbackDto.setToItem(req.getToItem());
//                callbackService.beforeCallback(req.getAppId(),
//                        Constants.CallbackCommand.UpdateFriendAfter, JSONObject
//                                .toJSONString(callbackDto));
//            }
//        }
        return responseVO;
    }

    @Override
    public ResponseVO deleteFriend(DeleteFriendReq req) {
        QueryWrapper<ImFriendShipEntity> query = new QueryWrapper<>();
        query.eq("app_id",req.getAppId());
        query.eq("from_id",req.getFromId());
        query.eq("to_id",req.getToId());
        ImFriendShipEntity fromItem = imFriendShipMapper.selectOne(query);

        if(fromItem == null) return ResponseVO.errorResponse(FriendShipErrorCode.TO_IS_NOT_YOUR_FRIEND);
        else {
            if(fromItem.getStatus() != null && fromItem.getStatus() == FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode()){
                ImFriendShipEntity imFriendShipEntity = new ImFriendShipEntity();
                imFriendShipEntity.setStatus(FriendShipStatusEnum.FRIEND_STATUS_DELETE.getCode());
                imFriendShipMapper.update(fromItem, query);
            } else {
                return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_DELETED);
            }
        }
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO deleteAllFriend(DeleteFriendReq req) {
        QueryWrapper<ImFriendShipEntity> query = new QueryWrapper<>();
        query.eq("app_id",req.getAppId());
        query.eq("from_id",req.getFromId());
        query.eq("status", FriendShipStatusEnum.FRIEND_STATUS_NORMAL);

        ImFriendShipEntity update = new ImFriendShipEntity();
        update.setStatus(FriendShipStatusEnum.FRIEND_STATUS_DELETE.getCode());
        imFriendShipMapper.update(update,query);

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO getAllFriendShip(GetAllFriendShipReq req) {
        QueryWrapper<ImFriendShipEntity> query = new QueryWrapper<>();
        query.eq("app_id",req.getAppId());
        query.eq("from_id",req.getFromId());
        return ResponseVO.successResponse(imFriendShipMapper.selectList(query));
    }

    @Override
    public ResponseVO getRelation(GetRelationReq req) {
        QueryWrapper<ImFriendShipEntity> query = new QueryWrapper<>();
        query.eq("app_id",req.getAppId());
        query.eq("from_id",req.getFromId());
        query.eq("to_id", req.getToId());

        ImFriendShipEntity imFriendShipEntity = imFriendShipMapper.selectOne(query);
        if(imFriendShipEntity == null) {
            // todo 返回记录不存在
        }
        return ResponseVO.successResponse(imFriendShipEntity);
    }



    @Override
    public ResponseVO checkFriendship(CheckFriendShipReq req) {
        Map<String, Integer> map = req.getToIds().stream().collect(Collectors.toMap(Function.identity(), s -> 0));

        List<CheckFriendShipResp> resp = null;

        if(req.getCheckType() == CheckFriendShipTypeEnum.SINGLE.getType()) {
            resp = imFriendShipMapper.checkFriendShip(req);
        } else {
            resp = imFriendShipMapper.checkFriendShipBoth(req);
        }

        Map<String, Integer> collect = resp.stream().collect(Collectors.toMap(CheckFriendShipResp::getToId, CheckFriendShipResp::getStatus));

        for(String toId : map.keySet()) {
            if(!collect.containsKey(toId)) {
                CheckFriendShipResp checkFriendShipResp = new CheckFriendShipResp();
                checkFriendShipResp.setFromId(req.getFromId());
                checkFriendShipResp.setToId(toId);
                checkFriendShipResp.setStatus(map.get(toId));
                resp.add(checkFriendShipResp);
            }
        }
        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO checkBlck(CheckFriendShipReq req) {
        Map<String, Integer> toIdMap
                = req.getToIds().stream().collect(Collectors
                .toMap(Function.identity(), s -> 0));
        List<CheckFriendShipResp> result;
        if (req.getCheckType() == CheckFriendShipTypeEnum.SINGLE.getType()) {
            result = imFriendShipMapper.checkFriendShipBlack(req);
        } else {
            result = imFriendShipMapper.checkFriendShipBlackBoth(req);
        }

        Map<String, Integer> collect = result.stream().collect(Collectors.toMap(CheckFriendShipResp::getToId, CheckFriendShipResp::getStatus));
        for (String toId: toIdMap.keySet()) {
            if(!collect.containsKey(toId)){
                CheckFriendShipResp checkFriendShipResp = new CheckFriendShipResp();
                checkFriendShipResp.setToId(toId);
                checkFriendShipResp.setFromId(req.getFromId());
                checkFriendShipResp.setStatus(toIdMap.get(toId));
                result.add(checkFriendShipResp);
            }
        }

        return ResponseVO.successResponse(result);
    }

    @Override
    public ResponseVO addBlack(AddFriendShipBlackReq req) {
        ResponseVO<ImUserDataEntity> fromInfo = imUserService.getSingleUserInfo(req.getFromId(), req.getAppId());
        if(!fromInfo.isOk()){
            return fromInfo;
        }

        ResponseVO<ImUserDataEntity> toInfo = imUserService.getSingleUserInfo(req.getToId(), req.getAppId());
        if(!toInfo.isOk()){
            return toInfo;
        }

        LambdaQueryWrapper<ImFriendShipEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(ImFriendShipEntity::getFromId, req.getFromId())
                .eq(ImFriendShipEntity::getToId, req.getToId())
                .eq(ImFriendShipEntity::getAppId, req.getAppId());

        ImFriendShipEntity imFriendShipEntity = imFriendShipMapper.selectOne(lqw);
        if(imFriendShipEntity == null) {
            //todo
        } else {
            if(imFriendShipEntity.getBlack() != null && imFriendShipEntity.getBlack() == FriendShipStatusEnum.BLACK_STATUS_BLACKED.getCode()) {
                return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_BLACK);
            } else {
                ImFriendShipEntity imFriendShipEntity1 = new ImFriendShipEntity();
                imFriendShipEntity1.setBlack(FriendShipStatusEnum.BLACK_STATUS_BLACKED.getCode());
                int update = imFriendShipMapper.update(imFriendShipEntity1, lqw);
            }
        }
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO deleteBlack(DeleteBlackReq req) {
        QueryWrapper queryFrom = new QueryWrapper<>()
                .eq("from_id", req.getFromId())
                .eq("app_id", req.getAppId())
                .eq("to_id", req.getToId());
        ImFriendShipEntity fromItem = imFriendShipMapper.selectOne(queryFrom);
        if (fromItem.getBlack() != null && fromItem.getBlack() == FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode()) {
            throw new ApplicationException(FriendShipErrorCode.FRIEND_IS_NOT_YOUR_BLACK);
        }

        ImFriendShipEntity update = new ImFriendShipEntity();
        update.setBlack(FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode());
        int update1 = imFriendShipMapper.update(update, queryFrom);
        return ResponseVO.successResponse();
    }

    @Transactional
    public ResponseVO doUpdate(String fromId, FriendDto dto, Integer appId) {
        UpdateWrapper<ImFriendShipEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().set(ImFriendShipEntity::getAddSource,dto.getAddSource())
                .set(ImFriendShipEntity::getExtra,dto.getExtra())
//                .set(ImFriendShipEntity::getFriendSequence,seq)
                .set(ImFriendShipEntity::getRemark,dto.getRemark())
                .eq(ImFriendShipEntity::getAppId,appId)
                .eq(ImFriendShipEntity::getToId,dto.getToId())
                .eq(ImFriendShipEntity::getFromId,fromId);

        int update = imFriendShipMapper.update(null, updateWrapper);

        if(update == 1){
            //之后回调
//            if (appConfig.isModifyFriendAfterCallback()){
//                AddFriendAfterCallbackDto callbackDto = new AddFriendAfterCallbackDto();
//                callbackDto.setFromId(fromId);
//                callbackDto.setToItem(dto);
//                callbackService.beforeCallback(appId,
//                        Constants.CallbackCommand.UpdateFriendAfter, JSONObject
//                                .toJSONString(callbackDto));
//            }
//            writeUserSeq.writeUserSeq(appId,fromId,Constants.SeqConstants.Friendship,seq);
            return ResponseVO.successResponse();
        }
        return ResponseVO.errorResponse();
    }

    @Transactional
    @Override
    public ResponseVO doAddFriend(RequestBase req, String fromId, FriendDto dto, Integer appId) {
        QueryWrapper<ImFriendShipEntity> query = new QueryWrapper<>();
        query.eq("app_id",appId);
        query.eq("from_id",fromId);
        query.eq("to_id",dto.getToId());
        ImFriendShipEntity fromItem = imFriendShipMapper.selectOne(query);

        if(fromItem == null) {
            fromItem = new ImFriendShipEntity();
            fromItem.setAppId(appId);
//            fromItem.setFriendSequence(seq);
            fromItem.setFromId(fromId);
            BeanUtils.copyProperties(dto,fromItem);
            fromItem.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
            fromItem.setCreateTime(System.currentTimeMillis());
            int insert = imFriendShipMapper.insert(fromItem);
            if(insert != 1){
                return ResponseVO.errorResponse(FriendShipErrorCode.ADD_FRIEND_ERROR);
            } else {
                if(fromItem.getStatus() == FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode()){
                    return ResponseVO.errorResponse(FriendShipErrorCode.TO_IS_YOUR_FRIEND);
                } else{
                    ImFriendShipEntity update = new ImFriendShipEntity();

                    if(StringUtils.isNotBlank(dto.getAddSource())){
                        update.setAddSource(dto.getAddSource());
                    }

                    if(StringUtils.isNotBlank(dto.getRemark())){
                        update.setRemark(dto.getRemark());
                    }

                    if(StringUtils.isNotBlank(dto.getExtra())){
                        update.setExtra(dto.getExtra());
                    }
//                    seq = redisSeq.doGetSeq(appId+":"+Constants.SeqConstants.Friendship);
//                    update.setFriendSequence(seq);
                    update.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());

                    int result = imFriendShipMapper.update(update, query);
                    if(result != 1){
                        return ResponseVO.errorResponse(FriendShipErrorCode.ADD_FRIEND_ERROR);
                    }
//                    writeUserSeq.writeUserSeq(appId,fromId,Constants.SeqConstants.Friendship,seq);
                }
            }
        }

        QueryWrapper<ImFriendShipEntity> toQuery = new QueryWrapper<>();
        toQuery.eq("app_id",appId);
        toQuery.eq("from_id",dto.getToId());
        toQuery.eq("to_id",fromId);
        ImFriendShipEntity toItem = imFriendShipMapper.selectOne(toQuery);

        if(toItem == null){
            toItem = new ImFriendShipEntity();
            toItem.setAppId(appId);
            toItem.setFromId(dto.getToId());
            BeanUtils.copyProperties(dto,toItem);
            toItem.setToId(fromId);
//            toItem.setFriendSequence(seq);
            toItem.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
            toItem.setCreateTime(System.currentTimeMillis());
//            toItem.setBlack(FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode());
            imFriendShipMapper.insert(toItem);
//            writeUserSeq.writeUserSeq(appId,dto.getToId(),Constants.SeqConstants.Friendship,seq);
        } else {
            if(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode() != toItem.getStatus()) {
                ImFriendShipEntity update = new ImFriendShipEntity();
//                update.setFriendSequence(seq);
                update.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
                imFriendShipMapper.update(update,toQuery);
//                writeUserSeq.writeUserSeq(appId,dto.getToId(),Constants.SeqConstants.Friendship,seq);
            }
        }
        return ResponseVO.successResponse();
    }
}
