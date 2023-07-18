package com.nineSeven.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class BootStrapConfig {

    private TcpConfig im;

    @Data
    public static class TcpConfig {
        private Integer tcpPort;// tcp 绑定的端口号

        private Integer webSocketPort; // webSocket 绑定的端口号

        private boolean enableWebSocket; //是否启用webSocket

        private Integer bossThreadSize; // boss线程 默认=1

        private Integer workThreadSize; //work线程

        private Long heartBeatTime;

        private RedisConfig redis;

        private Rabbitmq rabbitmq;

        private ZkConfig zkConfig;

        private Integer brokerId;

        private Integer loginModel;

        private String logicUrl;
    }

    @Data
    public static class ZkConfig {

        private String zkAddr;

        private Integer zkConnectTimeOut;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedisConfig {

        /**
         * 单机模式：single 哨兵模式：sentinel 集群模式：cluster
         */
        private String mode;
        /**
         * 数据库
         */
        private Integer database;
        /**
         * 密码
         */
        private String password;
        /**
         * 超时时间
         */
        private Integer timeout;
        /**
         * 最小空闲数
         */
        private Integer poolMinIdle;
        /**
         * 连接超时时间(毫秒)
         */
        private Integer poolConnTimeout;
        /**
         * 连接池大小
         */
        private Integer poolSize;

        /**
         * redis单机配置
         */
        private RedisSingle single;

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedisSingle {
        private String address;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rabbitmq {
        private String host;

        private Integer port;

        private String virtualHost;

        private String userName;

        private String password;
    }
}
