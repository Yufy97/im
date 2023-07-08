package com.nineSeven;

import com.alibaba.fastjson.JSONObject;
import com.nineSeven.pojo.Message;
import com.nineSeven.pojo.MessageHeader;
import com.nineSeven.utils.ByteBufToMessageUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class MessageDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> out) throws Exception {
        if(in.readableBytes() < 28) {
            return;
        }
        Message message = ByteBufToMessageUtils.transition(in);
        if(message == null){
            return;
        }
        out.add(message);
    }
}
