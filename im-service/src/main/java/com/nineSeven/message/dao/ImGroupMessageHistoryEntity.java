package com.nineSeven.message.dao;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


@Data
@TableName("im_group_message_history")
public class ImGroupMessageHistoryEntity {

    private Integer appId;

    private String fromId;

    private String groupId;

    private Long messageKey;

    private Long sequence;

    private String messageRandom;

    private Long messageTime;

    private Long createTime;


}
