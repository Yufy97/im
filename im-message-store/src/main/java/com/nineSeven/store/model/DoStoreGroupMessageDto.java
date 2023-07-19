package com.nineSeven.store.model;

import com.nineSeven.store.dao.ImMessageBodyEntity;
import com.nineSeven.model.message.GroupChatMessageContent;
import lombok.Data;
@Data
public class DoStoreGroupMessageDto {

    private GroupChatMessageContent groupChatMessageContent;

    private ImMessageBodyEntity imMessageBodyEntity;

}
