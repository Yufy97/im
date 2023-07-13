package com.nineSeven.route.algorithm.loop;

import com.nineSeven.enums.UserErrorCode;
import com.nineSeven.exception.ApplicationException;
import com.nineSeven.route.RouteHandle;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class LoopHandle implements RouteHandle {

    private AtomicLong index = new AtomicLong();

    @Override
    public String routeServer(List<String> values, String key) {
        int size = values.size();
        if(size == 0) {
            throw new ApplicationException(UserErrorCode.SERVER_NOT_AVAILABLE);
        }

        Long i = index.incrementAndGet() % size;
        return values.get(i.intValue());
    }
}
