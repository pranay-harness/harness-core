package io.harness.perpetualtask;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.protobuf.util.Durations;

import io.harness.grpc.utils.AnyUtils;
import io.harness.grpc.utils.HTimestamps;
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
  private final PerpetualTaskParams params;
  private final PerpetualTaskContext context;
  private final PerpetualTaskExecutor perpetualTaskExecutor;
  private final PerpetualTaskServiceGrpcClient perpetualTaskServiceGrpcClient;

  PerpetualTaskLifecycleManager(PerpetualTaskId taskId, PerpetualTaskContext context,
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
      logger.warn("Timed out starting task", tex);
    } catch (Exception ex) {
      logger.error("Exception is ", ex);
    }
  }

  void stopTask() {
    try {
      perpetualTaskExecutor.cleanup(taskId, params);
    } catch (Exception ex) {
      logger.error("Error while stopping task ", ex);
    }
  }

  private Void call() throws Exception {
    Instant taskStartTime = Instant.now();
    boolean taskStarted =
        perpetualTaskExecutor.runOnce(taskId, params, HTimestamps.toInstant(context.getHeartbeatTimestamp()));
    if (taskStarted) {
      perpetualTaskServiceGrpcClient.publishHeartbeat(taskId, taskStartTime);
    }
    return null;
  }

  private String getTaskType(PerpetualTaskParams params) {
    String fullyQualifiedClassName = AnyUtils.toFqcn(params.getCustomizedParams());
    return StringUtils.substringAfterLast(fullyQualifiedClassName, ".");
  }
}
