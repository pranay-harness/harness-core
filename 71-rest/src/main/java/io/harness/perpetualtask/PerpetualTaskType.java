package io.harness.perpetualtask;

import io.harness.artifact.ArtifactCollectionPTaskServiceClient;
import io.harness.perpetualtask.ecs.EcsPerpetualTaskServiceClient;
import io.harness.perpetualtask.example.SamplePerpetualTaskServiceClient;
import io.harness.perpetualtask.instancesync.AwsSshPerpetualTaskServiceClient;
import io.harness.perpetualtask.instancesync.PcfInstanceSyncPerpetualTaskClient;
import io.harness.perpetualtask.k8s.watch.K8sWatchPerpetualTaskServiceClient;

public enum PerpetualTaskType {
  K8S_WATCH(K8sWatchPerpetualTaskServiceClient.class),
  ECS_CLUSTER(EcsPerpetualTaskServiceClient.class),
  SAMPLE(SamplePerpetualTaskServiceClient.class),
  ARTIFACT_COLLECTION(ArtifactCollectionPTaskServiceClient.class),
  PCF_INSTANCE_SYNC(PcfInstanceSyncPerpetualTaskClient.class),
  AWS_SSH_INSTANCE_SYNC(AwsSshPerpetualTaskServiceClient.class),
  AWS_AMI_INSTANCE_SYNC(AwsAmiInstanceSyncPerpetualTaskClient.class);

  private final Class<? extends PerpetualTaskServiceClient> perpetualTaskServiceClientClass;

  PerpetualTaskType(Class<? extends PerpetualTaskServiceClient> perpetualTaskServiceClientClass) {
    this.perpetualTaskServiceClientClass = perpetualTaskServiceClientClass;
  }

  public Class<? extends PerpetualTaskServiceClient> getPerpetualTaskServiceClientClass() {
    return perpetualTaskServiceClientClass;
  }
}
