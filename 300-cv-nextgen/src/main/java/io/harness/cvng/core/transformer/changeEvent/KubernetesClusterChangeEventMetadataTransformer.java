package io.harness.cvng.core.transformer.changeEvent;

import io.harness.cvng.activity.entities.KubernetesClusterActivity;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.KubernetesChangeEventMetadata;

import java.time.Instant;

public class KubernetesClusterChangeEventMetadataTransformer
    extends ChangeEventMetaDataTransformer<KubernetesClusterActivity, KubernetesChangeEventMetadata> {
  @Override
  public KubernetesClusterActivity getEntity(ChangeEventDTO changeEventDTO) {
    KubernetesChangeEventMetadata metadata = (KubernetesChangeEventMetadata) changeEventDTO.getMetadata();
    return KubernetesClusterActivity.builder()
        .accountId(changeEventDTO.getAccountId())
        .activityName("Kubernetes " + metadata.getResourceType() + " event.")
        .orgIdentifier(changeEventDTO.getOrgIdentifier())
        .projectIdentifier(changeEventDTO.getProjectIdentifier())
        .serviceIdentifier(changeEventDTO.getServiceIdentifier())
        .environmentIdentifier(changeEventDTO.getEnvIdentifier())
        .eventTime(Instant.ofEpochMilli(changeEventDTO.getEventTime()))
        .changeSourceIdentifier(changeEventDTO.getChangeSourceIdentifier())
        .type(changeEventDTO.getType().getActivityType())
        .oldYaml(metadata.getOldYaml())
        .newYaml(metadata.getNewYaml())
        .resourceType(metadata.getResourceType())
        .action(metadata.getAction())
        .kind(metadata.getKind())
        .reason(metadata.getReason())
        .namespace(metadata.getNamespace())
        .workload(metadata.getWorkload())
        .activityStartTime(metadata.getTimestamp())
        .build();
  }

  @Override
  protected KubernetesChangeEventMetadata getMetadata(KubernetesClusterActivity activity) {
    return KubernetesChangeEventMetadata.builder()
        .oldYaml(activity.getOldYaml())
        .newYaml(activity.getNewYaml())
        .resourceType(activity.getResourceType())
        .action(activity.getAction())
        .kind(activity.getKind())
        .reason(activity.getReason())
        .namespace(activity.getNamespace())
        .workload(activity.getWorkload())
        .timestamp(activity.getEventTime())
        .resourceVersion(activity.getResourceVersion())
        .build();
  }
}
