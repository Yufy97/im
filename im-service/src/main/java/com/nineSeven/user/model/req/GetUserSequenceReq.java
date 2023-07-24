package com.nineSeven.user.model.req;

import com.nineSeven.model.RequestBase;
import lombok.Data;

@Data
public class GetUserSequenceReq extends RequestBase {

    private String userId;

}
