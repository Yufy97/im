package com.nineSeven.group.model.req;

import com.nineSeven.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class DestroyGroupReq extends RequestBase {

    @NotNull(message = "群id不能为空")
    private String groupId;

}