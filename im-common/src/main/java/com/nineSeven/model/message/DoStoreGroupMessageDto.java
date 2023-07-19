package com.nineSeven.model.message;

import lombok.Data;

@Data
public class DoStoreGroupMessageDto {

    private GroupChatMessageContent groupChatMessageContent;

    private ImMessageBody messageBody;

}
