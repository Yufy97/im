package com.nineSeven.store.model;

import com.nineSeven.store.dao.ImMessageBodyEntity;
import com.nineSeven.model.message.MessageContent;
import lombok.Data;

@Data
public class DoStoreP2PMessageDto {

    private MessageContent messageContent;

    private ImMessageBodyEntity imMessageBodyEntity;

}
