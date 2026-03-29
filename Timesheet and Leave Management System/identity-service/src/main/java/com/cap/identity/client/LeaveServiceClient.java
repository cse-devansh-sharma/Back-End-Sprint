package com.cap.identity.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "leave-service", fallback = LeaveServiceClientFallback.class)
public interface LeaveServiceClient {

    @PostMapping("/leave/internal/users/{userId}/allocate-initial")
    String allocateInitialLeaves(@PathVariable("userId") Long userId);
}
