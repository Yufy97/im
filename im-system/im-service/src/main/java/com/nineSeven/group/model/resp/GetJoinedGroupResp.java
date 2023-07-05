package com.nineSeven.group.model.resp;

import com.nineSeven.group.dao.ImGroupEntity;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GetJoinedGroupResp {

    private Integer totalCount;

    private List<ImGroupEntity> groupList;

    public GetJoinedGroupResp(Integer totalCount, List<ImGroupEntity> groupList) {
        this.totalCount = totalCount;
        this.groupList = groupList;
    }
}
