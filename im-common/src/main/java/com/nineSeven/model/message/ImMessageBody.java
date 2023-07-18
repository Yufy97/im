package com.nineSeven.model.message;

import lombok.Data;

@Data
public class ImMessageBody {
    private Integer appId;

    private Long messageKey;

    private String messageBody;

    private String securityKey;

    private Long messageTime;

    private Long createTime;

    private String extra;

    private Integer delFlag;
}
