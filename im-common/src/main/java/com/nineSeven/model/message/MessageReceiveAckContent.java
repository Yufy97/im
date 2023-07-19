package com.nineSeven.model.message;

import com.nineSeven.model.ClientInfo;
import lombok.Data;


@Data
public class MessageReceiveAckContent extends ClientInfo {

    private Long messageKey;

    private String fromId;

    private String toId;

    private Long messageSequence;


}
