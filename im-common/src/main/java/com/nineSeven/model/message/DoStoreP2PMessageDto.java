package com.nineSeven.model.message;

import lombok.Data;

@Data
public class DoStoreP2PMessageDto {

    private MessageContent messageContent;

    private ImMessageBody messageBody;
}
