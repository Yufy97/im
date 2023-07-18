package com.nineSeven.model.message;

import lombok.Data;

@Data
public class DoStoreP2PMessageDto {

    private MessageContent groupChatMessageContent;

    private ImMessageBody messageBody;
}
