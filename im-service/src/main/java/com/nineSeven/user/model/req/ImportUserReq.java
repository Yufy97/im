package com.nineSeven.user.model.req;

import com.nineSeven.model.RequestBase;
import com.nineSeven.user.dao.ImUserDataEntity;
import lombok.Data;

import java.util.List;


@Data
public class ImportUserReq extends RequestBase {

    private List<ImUserDataEntity> userData;

}
