package com.nineSeven;

import com.nineSeven.config.BootStrapConfig;
import com.nineSeven.receiever.MessageReceiver;
import com.nineSeven.redis.RedisManager;
import com.nineSeven.register.RegistryZK;
import com.nineSeven.register.ZKit;
import com.nineSeven.server.ImServer;
import com.nineSeven.server.ImWebSocketServer;
import com.nineSeven.utils.MqFactory;
import org.I0Itec.zkclient.ZkClient;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;


public class ServerApplication {
    public static void main(String[] args) throws FileNotFoundException {
        if(args.length > 0) {
            start(args[0]);
        }
    }

    private static void start(String path) throws FileNotFoundException {
        try {
            Yaml yaml = new Yaml();
            InputStream inputStream = new FileInputStream(path);
            BootStrapConfig bootStrapConfig = yaml.loadAs(inputStream, BootStrapConfig.class);
            new ImServer(bootStrapConfig.getIm()).start();
            new ImWebSocketServer(bootStrapConfig.getIm()).start();

            RedisManager.init(bootStrapConfig);
            MqFactory.init(bootStrapConfig.getIm().getRabbitmq());
            MessageReceiver.init(String.valueOf(bootStrapConfig.getIm().getBrokerId()));
            registerZK(bootStrapConfig);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(500);
        }
    }

    public static void registerZK(BootStrapConfig config) {
        ZkClient zkClient = new ZkClient(config.getIm().getZkConfig().getZkAddr(), config.getIm().getZkConfig().getZkConnectTimeOut());
        ZKit zKit = new ZKit(zkClient);
        RegistryZK registryZK = new RegistryZK(zKit, "127.0.0.1", config.getIm());
        Thread thread = new Thread(registryZK);
        thread.start();
    }

}
