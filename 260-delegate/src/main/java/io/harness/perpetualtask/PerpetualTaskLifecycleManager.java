/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.perpetualtask;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.concurrent.HTimeLimiter;
import io.harness.grpc.utils.AnyUtils;
import io.harness.grpc.utils.HTimestamps;
import io.harness.logging.AutoLogContext;
import io.harness.perpetualtask.grpc.PerpetualTaskServiceGrpcClient;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.protobuf.util.Durations;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@TargetModule(HarnessModule._420_DELEGATE_AGENT)
public class PerpetualTaskLifecycleManager {
  private final long timeoutMillis;
  private final PerpetualTaskId taskId;
  private final TimeLimiter timeLimiter;
  private final PerpetualTaskExecutionParams params;
  private final PerpetualTaskExecutionContext context;
  private final PerpetualTaskExecutor perpetualTaskExecutor;
  private final PerpetualTaskServiceGrpcClient perpetualTaskServiceGrpcClient;
  private final AtomicInteger currentlyExecutingPerpetualTasksCount;

  private final Cache<String, PerpetualTaskResponse> perpetualTaskResponseCache =
      Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();

  PerpetualTaskLifecycleManager(PerpetualTaskId taskId, PerpetualTaskExecutionContext context,
      Map<String, PerpetualTaskExecutor> factoryMap, PerpetualTaskServiceGrpcClient perpetualTaskServiceGrpcClient,
      TimeLimiter timeLimiter, AtomicInteger currentlyExecutingPerpetualTasksCount) {
    this.taskId = taskId;
    this.context = context;
    this.timeLimiter = timeLimiter;
    this.perpetualTaskServiceGrpcClient = perpetualTaskServiceGrpcClient;
    params = context.getTaskParams();
    perpetualTaskExecutor = factoryMap.get(getTaskType(params));
    timeoutMillis = Durations.toMillis(context.getTaskSchedule().getTimeout());
    this.currentlyExecutingPerpetualTasksCount = currentlyExecutingPerpetualTasksCount;
  }

  void startTask() {
    try {
      HTimeLimiter.callInterruptible21(timeLimiter, Duration.ofMillis(timeoutMillis), this::call);
    } catch (UncheckedTimeoutException tex) {
      log.debug("Timed out starting task", tex);
    } catch (Exception ex) {
      log.error("Exception is ", ex);
    }
  }

  void stopTask() {
    try {
      if (perpetualTaskExecutor != null) {
        perpetualTaskExecutor.cleanup(taskId, params);
      }
    } catch (Exception ex) {
      log.error("Error while stopping task ", ex);
    }
  }

  Void call() {
    Instant taskStartTime = Instant.now();
    PerpetualTaskResponse perpetualTaskResponse;
    try (AutoLogContext ignore1 = new PerpetualTaskLogContext(taskId.getId(), OVERRIDE_ERROR)) {
      currentlyExecutingPerpetualTasksCount.getAndIncrement();

      perpetualTaskResponse =
          perpetualTaskExecutor.runOnce(taskId, params, HTimestamps.toInstant(context.getHeartbeatTimestamp()));

      decrementTaskCounter();
    } catch (UncheckedTimeoutException tex) {
      perpetualTaskResponse = PerpetualTaskResponse.builder().responseCode(408).responseMessage("failed").build();
      log.debug("Timed out starting task", tex);

      decrementTaskCounter();
    } catch (Exception ex) {
      perpetualTaskResponse = PerpetualTaskResponse.builder().responseCode(500).responseMessage("failed").build();
      log.error("Exception is ", ex);
      decrementTaskCounter();
    }

    String perpetualTaskId = taskId.getId();
    PerpetualTaskResponse cachedPerpetualTaskResponse = perpetualTaskResponseCache.getIfPresent(perpetualTaskId);
    if (null == cachedPerpetualTaskResponse || !cachedPerpetualTaskResponse.equals(perpetualTaskResponse)) {
      perpetualTaskServiceGrpcClient.heartbeat(taskId, taskStartTime, perpetualTaskResponse);
      perpetualTaskResponseCache.put(perpetualTaskId, perpetualTaskResponse);
    }
    return null;
  }

  private String getTaskType(PerpetualTaskExecutionParams params) {
    String fullyQualifiedClassName = AnyUtils.toFqcn(params.getCustomizedParams());
    return StringUtils.substringAfterLast(fullyQualifiedClassName, ".");
  }

  private void decrementTaskCounter() {
    if (currentlyExecutingPerpetualTasksCount.get() > 0) {
      currentlyExecutingPerpetualTasksCount.getAndDecrement();
    }
  }
}
