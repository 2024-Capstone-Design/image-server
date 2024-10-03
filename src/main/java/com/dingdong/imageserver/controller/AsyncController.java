package com.dingdong.imageserver.controller;

import com.dingdong.imageserver.service.AsyncTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/test")
public class AsyncController {

    @Autowired
    private AsyncTaskService asyncTaskService;

    @Qualifier("taskExecutor")
    private final ThreadPoolTaskExecutor taskExecutor;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getThreadPoolStatus() {
        Map<String, Object> status = new HashMap<>();

        // 현재 스레드 풀 상태 확인
        status.put("corePoolSize", taskExecutor.getCorePoolSize());
        status.put("maxPoolSize", taskExecutor.getMaxPoolSize());
        status.put("activeThreadCount", taskExecutor.getActiveCount());
        status.put("poolSize", taskExecutor.getPoolSize());
        status.put("queueSize", taskExecutor.getThreadPoolExecutor().getQueue().size());

        return ResponseEntity.ok(status);
    }

    @GetMapping("/threads")
    public String testThreadExecution() {
        for (int i = 0; i < 20; i++) {
            asyncTaskService.executeAsyncTask(i);
        }
        return "Tasks initiated, check logs for thread execution.";
    }
}

