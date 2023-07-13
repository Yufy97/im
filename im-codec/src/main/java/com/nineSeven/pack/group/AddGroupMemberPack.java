package com.nineSeven.pack.group;

import lombok.Data;

import java.util.List;


@Data
public class AddGroupMemberPack {

    private String groupId;

    private List<String> members;

}
