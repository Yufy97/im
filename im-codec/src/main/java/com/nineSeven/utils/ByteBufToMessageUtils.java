package com.nineSeven.utils;

import com.alibaba.fastjson.JSONObject;
import com.nineSeven.pojo.Message;
import com.nineSeven.pojo.MessageHeader;
import io.netty.buffer.ByteBuf;

public class ByteBufToMessageUtils {

    public static Message transition(ByteBuf in) {


        int command = in.readInt();

        int version = in.readInt();

        int clientType = in.readInt();

        int messageType = in.readInt();

        int appId= in.readInt();

        int imeiLength = in.readInt();

        int bodyLength = in.readInt();

        if(in.readableBytes() < bodyLength + imeiLength) {
            in.resetReaderIndex();
            return null;
        }
        byte[] imeiData = new byte[imeiLength];
        in.readBytes(imeiData);
        String imei = new String(imeiData);

        byte[] bodyData = new byte[bodyLength];
        in.readBytes(bodyData);


        MessageHeader messageHeader = new MessageHeader();
        messageHeader.setAppId(appId);
        messageHeader.setClientType(clientType);
        messageHeader.setCommand(command);
        messageHeader.setBodyLength(bodyLength);
        messageHeader.setVersion(version);
        messageHeader.setMessageType(messageType);
        messageHeader.setImeiLength(imeiLength);
        messageHeader.setImei(imei);

        Message message = new Message();
        message.setMessageHeader(messageHeader);

        if(messageType == 0x0) {
            String body = new String(bodyData);
            JSONObject parse = (JSONObject) JSONObject.parse(body);
            message.setMessagePack(parse);
        }

        in.markReaderIndex();
        return message;
    }
}
