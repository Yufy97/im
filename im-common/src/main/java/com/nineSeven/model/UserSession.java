package com.nineSeven.model;

import lombok.Data;


@Data
public class UserSession {

    private String userId;


    private Integer appId;

    private Integer clientType;

    //sdk 版本号
    private Integer version;

    //连接状态 1=在线 2=离线
    private Integer connectState;

    private Integer brokerId;

    private String brokerHost;

    private String imei;

}
