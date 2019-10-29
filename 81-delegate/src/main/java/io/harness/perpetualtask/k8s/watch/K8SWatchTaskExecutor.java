package io.harness.perpetualtask.k8s.watch;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.grpc.utils.AnyUtils;
import io.harness.perpetualtask.PerpetualTaskExecutor;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskParams;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@Slf4j
public class K8SWatchTaskExecutor implements PerpetualTaskExecutor {
  @Inject private K8sWatchServiceDelegate k8sWatchServiceDelegate;
  private Map<String, String> taskWatchIdMap = new ConcurrentHashMap<>();

  @Override
  public boolean runOnce(PerpetualTaskId taskId, PerpetualTaskParams params, Instant heartbeatTime) {
    K8sWatchTaskParams watchTaskParams = AnyUtils.unpack(params.getCustomizedParams(), K8sWatchTaskParams.class);
    String watchId = k8sWatchServiceDelegate.create(watchTaskParams);
    taskWatchIdMap.put(taskId.getId(), watchId);
    logger.info("Created a watch with id {}.", watchId);
    return true;
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskParams params) {
    if (null != taskWatchIdMap.get(taskId.getId())) {
      String watchId = taskWatchIdMap.get(taskId.getId());
      logger.info("Stopping the watch with id {}", watchId);
      k8sWatchServiceDelegate.delete(watchId);
      return true;
    }
    return false;
  }
}
