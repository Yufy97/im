package com.nineSeven.receiever;

import com.alibaba.fastjson.JSONObject;
import com.nineSeven.constant.Constants;
import com.nineSeven.pack.MessagePack;
import com.nineSeven.receiever.process.BaseProcess;
import com.nineSeven.receiever.process.ProcessFactory;
import com.nineSeven.utils.MqFactory;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

@Slf4j
public class MessageReceiver{

    private static String brokerId;

    public static void startReceiveMessage() {
        try {
            Channel channel = MqFactory.getChannel(Constants.RabbitConstants.MessageService2Im + brokerId);

                                        //queue_name
            channel.queueDeclare(Constants.RabbitConstants.MessageService2Im + brokerId, true, false, false, null);

            //queueBind(String queue_name, String exchange, String routingKey)
            channel.queueBind(Constants.RabbitConstants.MessageService2Im + brokerId, Constants.RabbitConstants.MessageService2Im, brokerId);

                                    //queue_name
            channel.basicConsume(Constants.RabbitConstants.MessageService2Im + brokerId, false, new DefaultConsumer(channel){
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    try {
                        MessagePack messagePack = JSONObject.parseObject(new String(body), MessagePack.class);
                        BaseProcess messageProcess = ProcessFactory.getMessageProcess(messagePack.getCommand());
                        messageProcess.process(messagePack);
                        log.info(new String(body));

                        channel.basicAck(envelope.getDeliveryTag(), false);
                    } catch (Exception e) {
                        channel.basicNack(envelope.getDeliveryTag(), false, false);
                    }
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static void init(String brokerId) {
        if (StringUtils.isBlank(MessageReceiver.brokerId)) {
            MessageReceiver.brokerId = brokerId;
        }
        startReceiveMessage();
    }
}
