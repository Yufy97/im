package com.nineSeven.message.dao;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


@Data
@TableName("im_message_body")
public class ImMessageBodyEntity {

    private Integer appId;

    private Long messageKey;

    private String messageBody;

    private String securityKey;

    private Long messageTime;

    private Long createTime;

    private String extra;

    private Integer delFlag;

}
