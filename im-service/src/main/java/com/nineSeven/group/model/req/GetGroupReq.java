package com.nineSeven.group.model.req;

import com.nineSeven.model.RequestBase;
import lombok.Data;


@Data
public class GetGroupReq extends RequestBase {

    private String groupId;

}
