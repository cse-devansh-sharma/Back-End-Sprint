package com.cap.identity.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LeaveServiceClientFallback implements LeaveServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(LeaveServiceClientFallback.class);

    @Override
    public String allocateInitialLeaves(Long userId) {
        logger.error("Leave Service is unavailable. Fallback triggered for userId: {}", userId);
        return "Leave allocation deferred - Leave Service unreachable";
    }
}
