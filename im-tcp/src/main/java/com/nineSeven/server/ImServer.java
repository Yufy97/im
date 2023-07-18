package com.nineSeven.server;

import com.nineSeven.MessageDecoder;
import com.nineSeven.MessageEncoder;
import com.nineSeven.config.BootStrapConfig;
import com.nineSeven.handler.HeartBeatHandler;
import com.nineSeven.handler.NettyServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImServer {
    private final static Logger logger = LoggerFactory.getLogger(ImServer.class);
    BootStrapConfig.TcpConfig config;

    EventLoopGroup bossGroup;

    EventLoopGroup childrenGroup;

    ServerBootstrap server;


    public ImServer(BootStrapConfig.TcpConfig config) {
        this.config = config;
        bossGroup = new NioEventLoopGroup(config.getBossThreadSize());
        childrenGroup = new NioEventLoopGroup(config.getWorkThreadSize());
        server = new ServerBootstrap();
        server.group(bossGroup, childrenGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 10240) // 服务端可连接队列大小
                .option(ChannelOption.SO_REUSEADDR, true) // 参数表示允许重复使用本地地址和端口
                .childOption(ChannelOption.TCP_NODELAY, true) // 是否禁用Nagle算法 简单点说是否批量发送数据 true关闭 false开启。 开启的话可以减少一定的网络开销，但影响消息实时性
                .childOption(ChannelOption.SO_KEEPALIVE, true) // 保活开关2h没有数据服务端会发送心跳包
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new MessageDecoder());
                        pipeline.addLast(new MessageEncoder());
                        pipeline.addLast(new IdleStateHandler(0, 0, 1));
                        pipeline.addLast(new HeartBeatHandler(config.getHeartBeatTime()));
                        pipeline.addLast(new NettyServerHandler(config.getBrokerId(), config.getLogicUrl()));
                    }
                }).bind(config.getTcpPort());
    }

    public void start() {
        this.server.bind(this.config.getTcpPort());
    }
}
