package com.nineSeven.pack;

import lombok.Data;

import java.io.Serializable;

@Data
public class MessagePack<T> implements Serializable {

    private String userId;

    private Integer appId;

    private String toId;

    private int clientType;

    private String messageId;

    private String imei;

    private Integer command;

    private T data;



}
