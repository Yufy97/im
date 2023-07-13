package com.nineSeven.route.algorithm.consistentHash;

import com.nineSeven.enums.UserErrorCode;
import com.nineSeven.exception.ApplicationException;
import com.nineSeven.route.RouteHandle;

import java.util.List;

public class ConsistentHashHandle implements RouteHandle {

    private AbstractConsistentHash hash;

    private void setHash(AbstractConsistentHash hash) {
        this.hash = hash;
    }

    @Override
    public String routeServer(List<String> values, String key) {
        int size = values.size();
        if(size == 0) {
            throw new ApplicationException(UserErrorCode.SERVER_NOT_AVAILABLE);
        }

        return hash.process(values, key);
    }
}
