package com.nineSeven.handler;

import com.nineSeven.constant.Constants;
import com.nineSeven.utils.SessionSocketHolder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.util.AttributeKey;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.netty.handler.timeout.IdleStateEvent;

@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class HeartBeatHandler extends ChannelInboundHandlerAdapter {

    private Long heartBeatTime;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof IdleStateEvent){
           IdleStateEvent event = (IdleStateEvent) evt;
           if(event.state() == IdleState.READER_IDLE) {
               log.info("读空闲");
           } else if(event.state() == IdleState.WRITER_IDLE) {
               log.info("写空闲");
           } else if(event.state() == IdleState.ALL_IDLE) {
               Long lastTime = (Long) ctx.channel().attr(AttributeKey.valueOf(Constants.ReadTime)).get();
               long now = System.currentTimeMillis();
               if(lastTime != null && now - lastTime > heartBeatTime) {
                   SessionSocketHolder.offlineUserSession((NioSocketChannel) ctx.channel());
               }
           }
        }
    }
}
