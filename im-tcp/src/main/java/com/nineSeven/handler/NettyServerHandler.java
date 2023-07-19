package com.nineSeven.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.nineSeven.ResponseVO;
import com.nineSeven.constant.Constants;
import com.nineSeven.enums.ImConnectStatusEnum;
import com.nineSeven.enums.command.GroupEventCommand;
import com.nineSeven.enums.command.MessageCommand;
import com.nineSeven.enums.command.SystemCommand;
import com.nineSeven.feign.FeignMessageService;
import com.nineSeven.model.UserClientDto;
import com.nineSeven.model.UserSession;
import com.nineSeven.model.message.CheckSendMessageReq;
import com.nineSeven.pack.LoginPack;
import com.nineSeven.pack.MessagePack;
import com.nineSeven.pack.message.ChatMessageAck;
import com.nineSeven.pojo.Message;
import com.nineSeven.publish.MqMessageProducer;
import com.nineSeven.redis.RedisManager;
import com.nineSeven.utils.SessionSocketHolder;
import feign.Feign;
import feign.Request;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.redisson.api.RMap;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;

import java.net.InetAddress;

public class NettyServerHandler extends SimpleChannelInboundHandler<Message> {

    private Integer brokerId;

    private FeignMessageService feignMessageService;

    public NettyServerHandler(Integer brokerId, String logicUrl) {
        this.brokerId = brokerId;

        feignMessageService = Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                .options(new Request.Options(1000, 3000))
                .target(FeignMessageService.class, logicUrl);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        Integer command = msg.getMessageHeader().getCommand();

        if(command == SystemCommand.LOGIN.getCommand()) {
            //发布订阅通知上线，判断是否多端登录下线
            RedissonClient redissonClient = RedisManager.getRedissonClient();
            LoginPack loginPack = JSON.parseObject(JSONObject.toJSONString(msg.getMessagePack()), new TypeReference<LoginPack>() {}.getType());
            UserClientDto dto = new UserClientDto();
            dto.setImei(msg.getMessageHeader().getImei());
            dto.setUserId(loginPack.getUserId());
            dto.setClientType(msg.getMessageHeader().getClientType());
            dto.setAppId(msg.getMessageHeader().getAppId());
            RTopic topic = redissonClient.getTopic(Constants.RedisConstants.UserLoginChannel);
            topic.publish(JSONObject.toJSONString(dto));

            //绑定管道和userId,appId,clientType
            ctx.channel().attr(AttributeKey.valueOf(Constants.AppId)).set(msg.getMessageHeader().getAppId());
            ctx.channel().attr(AttributeKey.valueOf(Constants.UserId)).set(loginPack.getUserId());
            ctx.channel().attr(AttributeKey.valueOf(Constants.ClientType)).set(msg.getMessageHeader().getClientType());
            ctx.channel().attr(AttributeKey.valueOf(Constants.Imei)).set(msg.getMessageHeader().getImei());
            //放入维护的管道集合中
            SessionSocketHolder.put(msg.getMessageHeader().getAppId(), loginPack.getUserId(), msg.getMessageHeader().getClientType(),msg.getMessageHeader().getImei(), (NioSocketChannel) ctx.channel());

            //redis中存入userSession
            UserSession userSession = new UserSession();
            userSession.setAppId(msg.getMessageHeader().getAppId());
            userSession.setClientType(msg.getMessageHeader().getClientType());
            userSession.setUserId(loginPack.getUserId());
            userSession.setConnectState(ImConnectStatusEnum.ONLINE_STATUS.getCode());
            userSession.setBrokerId(brokerId);
            userSession.setImei(msg.getMessageHeader().getImei());
            try {
                InetAddress localHost = InetAddress.getLocalHost();
                userSession.setBrokerHost(localHost.getHostAddress());
            }catch (Exception e){
                e.printStackTrace();
            }
            RMap<String, String> map = redissonClient.getMap(msg.getMessageHeader().getAppId() + Constants.RedisConstants.UserSessionConstants + loginPack.getUserId());
            map.put(msg.getMessageHeader().getClientType() + ":" + msg.getMessageHeader().getImei(), JSONObject.toJSONString(userSession));

        } else if (command == SystemCommand.LOGOUT.getCommand()) {
            SessionSocketHolder.removeUserSession((NioSocketChannel) ctx.channel());
        } else if (command == SystemCommand.PING.getCommand()) {
            ctx.channel().attr(AttributeKey.valueOf(Constants.ReadTime)).set(System.currentTimeMillis());
        } else if(command == MessageCommand.MSG_P2P.getCommand() || command == GroupEventCommand.MSG_GROUP.getCommand()){
            String toId = "";
            CheckSendMessageReq req = new CheckSendMessageReq();
            req.setAppId(msg.getMessageHeader().getAppId());
            req.setCommand(msg.getMessageHeader().getCommand());
            JSONObject jsonObject = JSON.parseObject(JSONObject.toJSONString(msg.getMessagePack()));
            String fromId = jsonObject.getString("fromId");
            if(command == MessageCommand.MSG_P2P.getCommand()){
                toId = jsonObject.getString("toId");
            }else {
                toId = jsonObject.getString("groupId");
            }
            req.setToId(toId);
            req.setFromId(fromId);

            ResponseVO responseVO = feignMessageService.checkSendMessage(req);
            if(responseVO.isOk()){
                MqMessageProducer.sendMessage(msg,command);
            }else{
                Integer ackCommand = 0;
                if(command == MessageCommand.MSG_P2P.getCommand()){
                    ackCommand = MessageCommand.MSG_ACK.getCommand();
                }else {
                    ackCommand = GroupEventCommand.GROUP_MSG_ACK.getCommand();
                }

                ChatMessageAck chatMessageAck = new ChatMessageAck(jsonObject.getString("messageId"));
                responseVO.setData(chatMessageAck);
                MessagePack<ResponseVO> ack = new MessagePack<>();
                ack.setData(responseVO);
                ack.setCommand(ackCommand);
                ctx.channel().writeAndFlush(ack);
            }
        } else {
            MqMessageProducer.sendMessage(msg,command);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        SessionSocketHolder.offlineUserSession((NioSocketChannel) ctx.channel());
        ctx.close();
    }
}
