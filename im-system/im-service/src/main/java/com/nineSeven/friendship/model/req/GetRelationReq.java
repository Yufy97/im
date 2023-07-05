package com.nineSeven.friendship.model.req;

import com.nineSeven.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class GetRelationReq extends RequestBase {
    @NotBlank(message = "用户id不能为空")
    private String fromId;

    @NotBlank(message = "用户id不能为空")
    private String toId;
}
