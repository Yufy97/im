package com.nineSeven.message.service;

import com.nineSeven.ResponseVO;
import com.nineSeven.config.AppConfig;
import com.nineSeven.enums.*;
import com.nineSeven.friendship.dao.ImFriendShipEntity;
import com.nineSeven.friendship.model.req.GetRelationReq;
import com.nineSeven.friendship.service.ImFriendShipService;
import com.nineSeven.group.dao.ImGroupEntity;
import com.nineSeven.group.model.resp.GetRoleInGroupResp;
import com.nineSeven.group.service.ImGroupMemberService;
import com.nineSeven.group.service.ImGroupService;
import com.nineSeven.user.dao.ImUserDataEntity;
import com.nineSeven.user.service.ImUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class CheckSendMessageService {

    @Autowired
    ImUserService imUserService;

    @Autowired
    ImFriendShipService imFriendService;

    @Autowired
    ImGroupService imGroupService;

    @Autowired
    ImGroupMemberService imGroupMemberService;

    @Autowired
    AppConfig appConfig;


    public ResponseVO checkSenderForbidAndMute(String fromId, Integer appId){

        ResponseVO<ImUserDataEntity> singleUserInfo = imUserService.getSingleUserInfo(fromId, appId);
        if(!singleUserInfo.isOk()){
            return singleUserInfo;
        }

        ImUserDataEntity user = singleUserInfo.getData();
        if(user.getForbiddenFlag() == UserForbiddenFlagEnum.FORBIBBEN.getCode()){
            return ResponseVO.errorResponse(MessageErrorCode.FROMER_IS_FORBIBBEN);
        }else if (user.getSilentFlag() == UserSilentFlagEnum.MUTE.getCode()){
            return ResponseVO.errorResponse(MessageErrorCode.FROMER_IS_MUTE);
        }

        return ResponseVO.successResponse();
    }

    public ResponseVO checkFriendShip(String fromId,String toId,Integer appId){

        if(appConfig.isSendMessageCheckFriend()){
            GetRelationReq fromReq = new GetRelationReq();
            fromReq.setFromId(fromId);
            fromReq.setToId(toId);
            fromReq.setAppId(appId);
            ResponseVO<ImFriendShipEntity> fromRelation = imFriendService.getRelation(fromReq);
            if(!fromRelation.isOk()){
                return fromRelation;
            }
            GetRelationReq toReq = new GetRelationReq();
            fromReq.setFromId(toId);
            fromReq.setToId(fromId);
            fromReq.setAppId(appId);
            ResponseVO<ImFriendShipEntity> toRelation = imFriendService.getRelation(fromReq);
            if(!toRelation.isOk()){
                return toRelation;
            }

            if(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode() != fromRelation.getData().getStatus()){
                return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_DELETED);
            }

            if(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode() != toRelation.getData().getStatus()){
                return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_DELETED);
            }

            if(appConfig.isSendMessageCheckBlack()){
                if(FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode() != fromRelation.getData().getBlack()){
                    return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_BLACK);
                }

                if(FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode() != toRelation.getData().getBlack()){
                    return ResponseVO.errorResponse(FriendShipErrorCode.TARGET_IS_BLACK_YOU);
                }
            }
        }
        return ResponseVO.successResponse();
    }

    public ResponseVO checkGroupMessage(String fromId,String groupId,Integer appId){

        ResponseVO responseVO = checkSenderForbidAndMute(fromId, appId);
        if(!responseVO.isOk()){
            return responseVO;
        }


        ResponseVO<ImGroupEntity> group = imGroupService.getGroup(groupId, appId);
        if(!group.isOk()){
            return group;
        }


        ResponseVO<GetRoleInGroupResp> roleInGroupOne = imGroupMemberService.getRoleInGroupOne(groupId, fromId, appId);
        if(!roleInGroupOne.isOk()){
            return roleInGroupOne;
        }
        GetRoleInGroupResp data = roleInGroupOne.getData();

        ImGroupEntity groupData = group.getData();
        if(groupData.getMute() == GroupMuteTypeEnum.MUTE.getCode()
         && (data.getRole() != GroupMemberRoleEnum.MANAGER.getCode() && data.getRole() != GroupMemberRoleEnum.OWNER.getCode())){
            return ResponseVO.errorResponse(GroupErrorCode.THIS_GROUP_IS_MUTE);
        }

        if(data.getSpeakDate() != null && data.getSpeakDate() > System.currentTimeMillis()){
            return ResponseVO.errorResponse(GroupErrorCode.GROUP_MEMBER_IS_SPEAK);
        }

        return ResponseVO.successResponse();
    }


}
