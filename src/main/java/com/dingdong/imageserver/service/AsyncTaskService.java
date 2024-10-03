package com.dingdong.imageserver.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AsyncTaskService {

    @Async("taskExecutor")
    public void executeAsyncTask(int taskId) {
        log.info("Task ID: " + taskId + " started on thread: " + Thread.currentThread().getName());
        try {
            // 스레드가 5초 동안 동작하도록 설정하여 동시에 실행되는 스레드 확인
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            log.error("Task interrupted: " + taskId);
        }
        log.info("Task ID: " + taskId + " completed on thread: " + Thread.currentThread().getName());
    }
}

