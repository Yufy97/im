package com.nineSeven.user.model.req;

import com.nineSeven.model.RequestBase;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * @author: Chackylee
 * @description:
 **/
@Data
public class UserId extends RequestBase {

    @NotBlank(message = "userId不能为空")
    private String userId;

}
