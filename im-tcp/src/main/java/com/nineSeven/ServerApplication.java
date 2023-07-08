package com.nineSeven;

import com.nineSeven.config.BootStrapConfig;
import com.nineSeven.redis.RedisManager;
import com.nineSeven.server.ImServer;
import com.nineSeven.server.ImWebSocketServer;
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

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(500);
        }
    }
}
