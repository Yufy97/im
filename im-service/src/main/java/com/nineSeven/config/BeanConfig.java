package com.nineSeven.config;

import com.nineSeven.enums.ImUrlRouteWayEnum;
import com.nineSeven.enums.RouteHashMethodEnum;
import com.nineSeven.route.RouteHandle;
import com.nineSeven.route.algorithm.consistentHash.AbstractConsistentHash;
import com.nineSeven.route.algorithm.random.RandomHandle;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Configuration
public class BeanConfig {
    @Autowired
    AppConfig appConfig;

    @Bean
    public RouteHandle routeHandle() throws Exception{
        Integer imRouteWay = appConfig.getImRouteWay();
        ImUrlRouteWayEnum handler = ImUrlRouteWayEnum.getHandler(imRouteWay);
        String routWay = handler.getClazz();

        RouteHandle routeHandle = (RouteHandle) Class.forName(routWay).newInstance();
        if (handler == ImUrlRouteWayEnum.HASH) {
            Method method = Class.forName(routWay).getMethod("setHash", AbstractConsistentHash.class);
            Integer consistentHashWay = appConfig.getConsistentHashWay();

            String hashWay = RouteHashMethodEnum.getHandler(consistentHashWay).getClazz();
            AbstractConsistentHash consistentHash = (AbstractConsistentHash) Class.forName(hashWay).newInstance();
            method.invoke(routeHandle, consistentHash);
        }
        return routeHandle;
    }

    @Bean
    public ZkClient buildZKClient() {
        return new ZkClient(appConfig.getZkAddr(), appConfig.getZkConnectTimeOut());
    }
}
