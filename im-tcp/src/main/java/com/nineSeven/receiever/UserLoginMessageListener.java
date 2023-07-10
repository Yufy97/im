package com.nineSeven.receiever;

import com.alibaba.fastjson.JSONObject;
import com.nineSeven.ClientType;
import com.nineSeven.constant.Constants;
import com.nineSeven.enums.DeviceMultiLoginEnum;
import com.nineSeven.enums.command.SystemCommand;
import com.nineSeven.model.UserClientDto;
import com.nineSeven.pack.MessagePack;
import com.nineSeven.redis.RedisManager;
import com.nineSeven.utils.SessionSocketHolder;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.redisson.api.RTopic;
import org.redisson.api.listener.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class UserLoginMessageListener {
    private final static Logger logger = LoggerFactory.getLogger(UserLoginMessageListener.class);

    private Integer loginModel;

    public UserLoginMessageListener(Integer loginModel) {
        this.loginModel = loginModel;
    }

    public void listenerUserLogin() {
        RTopic topic = RedisManager.getRedissonClient().getTopic(Constants.RedisConstants.UserLoginChannel);
        topic.addListener(String.class, (charSequence, msg) -> {
            UserClientDto dto = JSONObject.parseObject(msg, UserClientDto.class);
            List<NioSocketChannel> nioSocketChannels = SessionSocketHolder.get(dto.getAppId(), dto.getUserId());
            logger.info("收到用户上线通知：" + msg);

            for (NioSocketChannel nioSocketChannel : nioSocketChannels) {
                if(loginModel == DeviceMultiLoginEnum.ONE.getLoginMode()) {
                    Integer clientType = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ClientType)).get();
                    String imei = (String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.Imei)).get();

                    if(!(clientType + imei).equals(dto.getClientType() + dto.getImei())){
                        offline(nioSocketChannel);
                    }
                } else if(loginModel == DeviceMultiLoginEnum.TWO.getLoginMode()){
                    if(dto.getClientType() == ClientType.WEB.getCode()) {
                        continue;
                    }

                    Integer clientType = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ClientType)).get();
                    if(clientType == ClientType.WEB.getCode()) {
                        continue;
                    }

                    String imei = (String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.Imei)).get();
                    if(!(clientType + imei).equals(dto.getClientType() + dto.getImei())){
                        offline(nioSocketChannel);
                    }
                } else if (loginModel == DeviceMultiLoginEnum.THREE.getLoginMode()) {
                    Integer clientType = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ClientType)).get();
                    String imei = (String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.Imei)).get();
                    if(clientType == ClientType.WEB.getCode()) {
                        continue;
                    }

                    Boolean isSameClient = false;
                    if((clientType == ClientType.IOS.getCode() ||
                            clientType == ClientType.ANDROID.getCode()) &&
                            (dto.getClientType() == ClientType.IOS.getCode() ||
                                    dto.getClientType() == ClientType.ANDROID.getCode())){
                        isSameClient = true;
                    }

                    if((clientType == ClientType.MAC.getCode() ||
                            clientType == ClientType.WINDOWS.getCode()) &&
                            (dto.getClientType() == ClientType.MAC.getCode() ||
                                    dto.getClientType() == ClientType.WINDOWS.getCode())){
                        isSameClient = true;
                    }

                    if(isSameClient && !(clientType+imei).equals(dto.getClientType()+dto.getImei())){
                        offline(nioSocketChannel);
                    }
                }
            }
        });
    }

    private void offline(NioSocketChannel nioSocketChannel){
        MessagePack<Object> pack = new MessagePack<>();
        pack.setToId((String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get());
        pack.setUserId((String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.UserId)).get());
        pack.setCommand(SystemCommand.MUTUALLOGIN.getCommand());
        nioSocketChannel.writeAndFlush(pack);
    }
}
