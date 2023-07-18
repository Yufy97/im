package com.nineSeven.publish;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.nineSeven.constant.Constants;
import com.nineSeven.enums.command.CommandType;
import com.nineSeven.pojo.Message;
import com.nineSeven.utils.MqFactory;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MqMessageProducer {

    public static void sendMessage(Message message, Integer command) {
        Channel channel;
        String exchangeName = null;
        CommandType commandType = CommandType.getCommandType(String.valueOf(command).substring(0, 1));
        if(commandType == CommandType.MESSAGE) {
            exchangeName = Constants.RabbitConstants.Im2MessageService;
        }else if(commandType == CommandType.GROUP) {
            exchangeName = Constants.RabbitConstants.Im2GroupService;
        }
        try {
            channel = MqFactory.getChannel(exchangeName);   //channelName与exchangeName同名
            JSONObject json = (JSONObject) JSON.toJSON(message.getMessagePack());
            json.put("command",command);
            json.put("clientType",message.getMessageHeader().getClientType());
            json.put("imei",message.getMessageHeader().getImei());
            json.put("appId",message.getMessageHeader().getAppId());
            channel.basicPublish(exchangeName, "", null, json.toJSONString().getBytes());
        }catch (Exception e) {
            log.error("发送消息出现异常：{}", e.getMessage());
        }
    }
}
