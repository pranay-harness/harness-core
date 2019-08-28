package io.harness.perpetualtask.k8s.watch;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import io.harness.perpetualtask.AbstractPerpetualTask;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskParams;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class K8sWatchTask extends AbstractPerpetualTask {
  private String watchId;
  private K8sWatchTaskParams watchTaskParams;
  private K8sWatchServiceDelegate k8sWatchServiceDelegate;

  @Inject
  public K8sWatchTask(@Assisted PerpetualTaskId taskId, @Assisted PerpetualTaskParams params,
      K8sWatchServiceDelegate k8sWatchServiceDelegate) throws Exception {
    super(taskId);
    K8sWatchTaskParams watchTaskParams = params.getCustomizedParams().unpack(K8sWatchTaskParams.class);
    this.watchTaskParams = watchTaskParams;
    this.k8sWatchServiceDelegate = k8sWatchServiceDelegate;
  }

  @Override
  public Void call() throws Exception {
    watchId = k8sWatchServiceDelegate.create(watchTaskParams);
    logger.info("Created a watch with id {}.", watchId);
    return null;
  }

  @Override
  public void stop() {
    logger.info("Stopping the watch with id {}", watchId);
    k8sWatchServiceDelegate.delete(watchId);
  }
}
