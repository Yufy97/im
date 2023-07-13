package com.nineSeven.route.algorithm.random;

import com.nineSeven.enums.UserErrorCode;
import com.nineSeven.exception.ApplicationException;
import com.nineSeven.route.RouteHandle;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomHandle implements RouteHandle {
    @Override
    public String routeServer(List<String> values, String key) {
        int size = values.size();
        if(size == 0) {
            throw new ApplicationException(UserErrorCode.SERVER_NOT_AVAILABLE);
        }

        int i = ThreadLocalRandom.current().nextInt(size);
        return values.get(i);
    }
}
