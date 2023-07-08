package com.nineSeven.group.model.req;

import com.nineSeven.model.RequestBase;
import lombok.Data;

import java.util.List;


@Data
public class GetRoleInGroupReq extends RequestBase {

    private String groupId;

    private List<String> memberId;
}
