package com.nineSeven.message.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.nineSeven.constant.Constants;
import com.nineSeven.enums.command.MessageCommand;
import com.nineSeven.message.service.MessageStoreService;
import com.nineSeven.message.service.MessageSyncService;
import com.nineSeven.message.service.P2PMessageService;
import com.nineSeven.model.message.MessageContent;
import com.nineSeven.model.message.MessageReadedContent;
import com.nineSeven.model.message.MessageReceiveAckContent;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class ChatOperateReceiver {

    private static Logger logger = LoggerFactory.getLogger(ChatOperateReceiver.class);

    @Autowired
    P2PMessageService p2PMessageService;

    @Autowired
    MessageSyncService messageSyncService;

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = Constants.RabbitConstants.Im2MessageService, durable = "true"),
                 exchange = @Exchange(value = Constants.RabbitConstants.Im2MessageService)),concurrency = "1")
    public void onChatMessage(@Payload Message message, @Headers Map<String,Object> headers, Channel channel) throws Exception {
        String msg = new String(message.getBody(), StandardCharsets.UTF_8);
        Long deliveryTag = (Long) headers.get(AmqpHeaders.DELIVERY_TAG);
        try {
            JSONObject jsonObject = JSON.parseObject(msg);
            Integer command = jsonObject.getInteger("command");
            if(command.equals(MessageCommand.MSG_P2P.getCommand())) {
                MessageContent messageContent = jsonObject.toJavaObject(MessageContent.class);
                p2PMessageService.process(messageContent);
            } else if (command.equals(MessageCommand.MSG_RECIVE_ACK.getCommand())) {
                messageSyncService.receiveMark(jsonObject.toJavaObject(MessageReceiveAckContent.class));
            } else if (command.equals(MessageCommand.MSG_READED.getCommand())) {
                MessageReadedContent messageReadedContent = jsonObject.toJavaObject(MessageReadedContent.class);
                messageSyncService.readMark(messageReadedContent);
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            logger.error("处理消息出现异常：{}", e.getMessage());
            logger.error("RMQ_CHAT_TRAN_ERROR", e);
            logger.error("NACK_MSG:{}", msg);

            channel.basicNack(deliveryTag, false, false);
        }
    }

}
