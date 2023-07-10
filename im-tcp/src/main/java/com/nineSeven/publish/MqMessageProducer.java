package com.nineSeven.publish;

import com.alibaba.fastjson.JSONObject;
import com.nineSeven.constant.Constants;
import com.nineSeven.utils.MqFactory;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MqMessageProducer {

    public static void sendMessage(Object message) {
        Channel channel;
        String channelName = Constants.RabbitConstants.Im2MessageService;
        try {
            channel = MqFactory.getChannel(channelName);
            channel.basicPublish(channelName, "", null, JSONObject.toJSONString(message).getBytes());
        }catch (Exception e) {
            log.error("发送消息出现异常：{}", e.getMessage());
        }
    }
}
