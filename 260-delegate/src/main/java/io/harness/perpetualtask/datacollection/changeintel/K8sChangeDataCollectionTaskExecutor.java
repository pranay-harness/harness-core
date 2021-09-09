package io.harness.perpetualtask.datacollection.changeintel;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.grpc.utils.AnyUtils;
import io.harness.perpetualtask.PerpetualTaskExecutionParams;
import io.harness.perpetualtask.PerpetualTaskExecutor;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.datacollection.K8ActivityCollectionPerpetualTaskParams;
import io.harness.perpetualtask.k8s.watch.K8sWatchServiceDelegate;

import com.google.inject.Singleton;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(HarnessTeam.CV)
public class K8sChangeDataCollectionTaskExecutor implements PerpetualTaskExecutor {
  private final Map<String, K8sWatchServiceDelegate.WatcherGroup> watchMap = new ConcurrentHashMap<>();

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    K8ActivityCollectionPerpetualTaskParams taskParams =
        AnyUtils.unpack(params.getCustomizedParams(), K8ActivityCollectionPerpetualTaskParams.class);
    log.info("Executing perpetual task for changeSourceId: {}", taskParams.getDataCollectionWorkerId());

    return null;
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
    return false;
  }
}
