package com.nineSeven.friendship.model.req;

import com.nineSeven.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;


@Data
public class AddFriendShipGroupMemberReq extends RequestBase {

    @NotBlank(message = "fromId不能为空")
    private String fromId;

    @NotBlank(message = "分组名称不能为空")
    private String groupName;

    @NotEmpty(message = "请选择用户")
    private List<String> toIds;


}
