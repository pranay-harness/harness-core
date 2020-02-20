package io.harness.perpetualtask;

import io.harness.artifact.ArtifactCollectionPTaskServiceClient;
import io.harness.perpetualtask.ecs.EcsPerpetualTaskServiceClient;
import io.harness.perpetualtask.example.SamplePerpetualTaskServiceClient;
import io.harness.perpetualtask.k8s.watch.K8sWatchPerpetualTaskServiceClient;

public enum PerpetualTaskType {
  K8S_WATCH(K8sWatchPerpetualTaskServiceClient.class),
  ECS_CLUSTER(EcsPerpetualTaskServiceClient.class),
  SAMPLE(SamplePerpetualTaskServiceClient.class),
  ARTIFACT_COLLECTION(ArtifactCollectionPTaskServiceClient.class);

  private final Class<? extends PerpetualTaskServiceClient> perpetualTaskServiceClientClass;

  PerpetualTaskType(Class<? extends PerpetualTaskServiceClient> perpetualTaskServiceClientClass) {
    this.perpetualTaskServiceClientClass = perpetualTaskServiceClientClass;
  }

  public Class<? extends PerpetualTaskServiceClient> getPerpetualTaskServiceClientClass() {
    return perpetualTaskServiceClientClass;
  }
}
