package io.harness.perpetualtask;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.protobuf.util.Durations;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.harness.grpc.utils.AnyUtils;
import io.harness.grpc.utils.HTimestamps;
import io.harness.logging.AutoLogContext;
import io.harness.perpetualtask.grpc.PerpetualTaskServiceGrpcClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PerpetualTaskLifecycleManager {
  private final long timeoutMillis;
  private final PerpetualTaskId taskId;
  private final TimeLimiter timeLimiter;
  private final PerpetualTaskExecutionParams params;
  private final PerpetualTaskExecutionContext context;
  private final PerpetualTaskExecutor perpetualTaskExecutor;
  private final PerpetualTaskServiceGrpcClient perpetualTaskServiceGrpcClient;

  private Cache<String, PerpetualTaskResponse> perpetualTaskResponseCache =
      Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();

  PerpetualTaskLifecycleManager(PerpetualTaskId taskId, PerpetualTaskExecutionContext context,
      Map<String, PerpetualTaskExecutor> factoryMap, PerpetualTaskServiceGrpcClient perpetualTaskServiceGrpcClient,
      TimeLimiter timeLimiter) {
    this.taskId = taskId;
    this.context = context;
    this.timeLimiter = timeLimiter;
    this.perpetualTaskServiceGrpcClient = perpetualTaskServiceGrpcClient;
    params = context.getTaskParams();
    perpetualTaskExecutor = factoryMap.get(getTaskType(params));
    timeoutMillis = Durations.toMillis(context.getTaskSchedule().getTimeout());
  }

  void startTask() {
    try {
      timeLimiter.callWithTimeout(this ::call, timeoutMillis, TimeUnit.MILLISECONDS, true);
    } catch (UncheckedTimeoutException tex) {
      log.warn("Timed out starting task", tex);
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
      perpetualTaskResponse =
          perpetualTaskExecutor.runOnce(taskId, params, HTimestamps.toInstant(context.getHeartbeatTimestamp()));
    } catch (UncheckedTimeoutException tex) {
      perpetualTaskResponse = PerpetualTaskResponse.builder().responseCode(408).responseMessage("failed").build();
      log.warn("Timed out starting task", tex);
    } catch (Exception ex) {
      perpetualTaskResponse = PerpetualTaskResponse.builder().responseCode(500).responseMessage("failed").build();
      log.error("Exception is ", ex);
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
}
