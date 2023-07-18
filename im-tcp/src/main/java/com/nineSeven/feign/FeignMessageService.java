package com.nineSeven.feign;

import com.nineSeven.ResponseVO;
import com.nineSeven.model.message.CheckSendMessageReq;
import feign.Headers;
import feign.RequestLine;


public interface FeignMessageService {

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @RequestLine("POST /message/checkSend")
    ResponseVO checkSendMessage(CheckSendMessageReq o);

}
