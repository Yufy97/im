package com.nineSeven.conversation.model;

import com.nineSeven.model.RequestBase;
import lombok.Data;


@Data
public class UpdateConversationReq extends RequestBase {

    private String conversationId;

    private Integer isMute;

    private Integer isTop;

    private String fromId;


}
