package com.nineSeven.user.model.req;

import com.nineSeven.model.RequestBase;
import lombok.Data;

import java.util.List;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
@Data
public class PullUserOnlineStatusReq extends RequestBase {

    private List<String> userList;

}
