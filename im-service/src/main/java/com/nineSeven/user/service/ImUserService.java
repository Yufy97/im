package com.nineSeven.user.service;

import com.nineSeven.ResponseVO;
import com.nineSeven.user.dao.ImUserDataEntity;
import com.nineSeven.user.model.req.*;
import com.nineSeven.user.model.resp.GetUserInfoResp;


public interface ImUserService {
    ResponseVO importUser(ImportUserReq req);

    ResponseVO<GetUserInfoResp> getUserInfo(GetUserInfoReq req);

    ResponseVO<ImUserDataEntity> getSingleUserInfo(String userId, Integer appId);

    ResponseVO deleteUser(DeleteUserReq req);

    ResponseVO modifyUserInfo(ModifyUserInfoReq req);

    ResponseVO login(LoginReq req);

    ResponseVO getUserSequence(GetUserSequenceReq req);
}
