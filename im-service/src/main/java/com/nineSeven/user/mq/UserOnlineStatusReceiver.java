package com.nineSeven.user.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.nineSeven.constant.Constants;
import com.nineSeven.enums.command.UserEventCommand;
import com.nineSeven.message.mq.ChatOperateReceiver;
import com.nineSeven.user.model.UserStatusChangeNotifyContent;
import com.nineSeven.user.service.ImUserStatusService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

@Component
public class UserOnlineStatusReceiver {

    private static Logger logger = LoggerFactory.getLogger(ChatOperateReceiver.class);

    @Autowired
    ImUserStatusService imUserStatusService;

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = Constants.RabbitConstants.Im2UserService, durable = "true"),
            exchange = @Exchange(value = Constants.RabbitConstants.Im2UserService)), concurrency = "1")
    @RabbitHandler
    public void onChatMessage(@Payload Message message, @Headers Map<String, Object> headers, Channel channel) throws Exception {
        long start = System.currentTimeMillis();
        String msg = new String(message.getBody(), StandardCharsets.UTF_8);
        logger.info("CHAT MSG FROM QUEUE :::::" + msg);

        Long deliveryTag = (Long) headers.get(AmqpHeaders.DELIVERY_TAG);

        try {
            JSONObject jsonObject = JSON.parseObject(msg);
            Integer command = jsonObject.getInteger("command");
            if(Objects.equals(command, UserEventCommand.USER_ONLINE_STATUS_CHANGE.getCommand())){
                UserStatusChangeNotifyContent content = JSON.parseObject(msg, new TypeReference<UserStatusChangeNotifyContent>() {}.getType());

                imUserStatusService.processUserOnlineStatusNotify(content);
            }

            channel.basicAck(deliveryTag,false);
        }catch (Exception e){
            logger.error("处理消息出现异常：{}",e.getMessage());
            logger.error("RMQ_CHAT_TRAN_ERROR", e);
            logger.error("NACK_MSG:{}", msg);
            //第一个false 表示不批量拒绝，第二个false表示不重回队列
            channel.basicNack(deliveryTag, false, false);
        }finally {
            long end = System.currentTimeMillis();
            logger.debug("channel {} basic-Ack ,it costs {} ms,threadName = {},threadId={}", channel, end - start, Thread.currentThread().getName(), Thread.currentThread().getId());
        }
    }
}
