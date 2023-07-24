package com.nineSeven.model;

import lombok.Data;


@Data
public class SyncReq extends RequestBase {


    private Long lastSequence;

    private Integer maxLimit;

}
