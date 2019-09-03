package io.harness.perpetualtask;

import io.harness.perpetualtask.ecs.EcsPerpetualTaskServiceClient;
import io.harness.perpetualtask.example.SamplePerpetualTaskServiceClient;
import io.harness.perpetualtask.k8s.watch.K8sWatchPerpetualTaskServiceClient;

public enum PerpetualTaskType {
  ECS_CLUSTER(EcsPerpetualTaskServiceClient.class),
  SAMPLE(SamplePerpetualTaskServiceClient.class),
  K8S_WATCH(K8sWatchPerpetualTaskServiceClient.class);

  private final Class<? extends PerpetualTaskServiceClient> perpetualTaskServiceClientClass;

  PerpetualTaskType(Class<? extends PerpetualTaskServiceClient> perpetualTaskServiceClientClass) {
    this.perpetualTaskServiceClientClass = perpetualTaskServiceClientClass;
  }

  public Class<? extends PerpetualTaskServiceClient> getPerpetualTaskServiceClientClass() {
    return perpetualTaskServiceClientClass;
  }
}
