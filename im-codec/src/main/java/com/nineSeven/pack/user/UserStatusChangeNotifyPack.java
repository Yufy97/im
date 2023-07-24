package com.nineSeven.pack.user;

import com.nineSeven.model.UserSession;
import lombok.Data;

import java.util.List;

@Data
public class UserStatusChangeNotifyPack {

    private Integer appId;

    private String userId;

    private Integer status;

    private List<UserSession> client;
}
