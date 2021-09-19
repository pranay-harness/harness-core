package io.harness.perpetualtask.datacollection.k8s;

import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.KubernetesChangeEventMetadata;

import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1ReplicaSet;
import java.time.Instant;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@SuperBuilder
@Slf4j
public class ChangeIntelReplicaSetHandler extends BaseChangeHandler<V1ReplicaSet> {
  @Override
  String getKind() {
    return "ReplicaSet";
  }

  @Override
  String getApiVersion() {
    return "apps/v1";
  }

  @Override
  boolean hasOwnerReference(V1ReplicaSet v1ReplicaSet) {
    if (v1ReplicaSet.getMetadata().getOwnerReferences() != null) {
      for (V1OwnerReference ownerReference : v1ReplicaSet.getMetadata().getOwnerReferences()) {
        if (Boolean.TRUE.equals(ownerReference.getController())) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void processAndSendAddEvent(V1ReplicaSet v1ReplicaSet) {
    log.info("OnAdd of ReplicaSet.");
    if (!hasOwnerReference(v1ReplicaSet)) {
      log.info("ReplicaSet doesn't have an ownerReference. Sending event Data");
      ChangeEventDTO eventDTO = buildReplicaSetChangeEvent(v1ReplicaSet);
      String newYaml = k8sHandlerUtils.yamlDump(v1ReplicaSet);
      ((KubernetesChangeEventMetadata) eventDTO.getChangeEventMetaData()).setNewYaml(newYaml);
      ((KubernetesChangeEventMetadata) eventDTO.getChangeEventMetaData())
          .setTimestamp(Instant.ofEpochMilli(
              v1ReplicaSet.getMetadata().getCreationTimestamp().toDateTime().toInstant().getMillis()));
      ((KubernetesChangeEventMetadata) eventDTO.getChangeEventMetaData())
          .setAction(KubernetesChangeEventMetadata.Action.Add);
      sendEvent(accountId, eventDTO);
    }
  }

  @Override
  void processAndSendUpdateEvent(V1ReplicaSet oldResource, V1ReplicaSet newResource, String oldYaml, String newYaml) {
    String resourceVersion = newResource.getMetadata().getResourceVersion();
    log.info("processAndSendUpdateEvent for ReplicaSet. Old ResourceVersion is {} and new is {}",
        oldResource.getMetadata().getResourceVersion(), resourceVersion);
    ChangeEventDTO eventDTO = buildReplicaSetChangeEvent(newResource);
    ((KubernetesChangeEventMetadata) eventDTO.getChangeEventMetaData()).setOldYaml(oldYaml);
    ((KubernetesChangeEventMetadata) eventDTO.getChangeEventMetaData()).setNewYaml(newYaml);
    ((KubernetesChangeEventMetadata) eventDTO.getChangeEventMetaData())
        .setAction(KubernetesChangeEventMetadata.Action.Update);
    eventDTO.setEventTime(Instant.now().toEpochMilli());
    sendEvent(accountId, eventDTO);
  }

  @Override
  void processAndSendDeletedEvent(V1ReplicaSet newResource, String oldYaml) {
    ChangeEventDTO eventDTO = buildReplicaSetChangeEvent(newResource);
    ((KubernetesChangeEventMetadata) eventDTO.getChangeEventMetaData()).setOldYaml(oldYaml);
    ((KubernetesChangeEventMetadata) eventDTO.getChangeEventMetaData())
        .setAction(KubernetesChangeEventMetadata.Action.Delete);
    sendEvent(accountId, eventDTO);
  }

  private ChangeEventDTO buildReplicaSetChangeEvent(V1ReplicaSet v1ReplicaSet) {
    String workload = v1ReplicaSet.getMetadata().getName();
    String namespace = v1ReplicaSet.getMetadata().getNamespace();
    ChangeEventDTO eventDTO = buildChangeEventDTOSkeleton();
    eventDTO.setChangeEventMetaData(KubernetesChangeEventMetadata.builder()
                                        .resourceType(KubernetesChangeEventMetadata.KubernetesResourceType.ReplicaSet)
                                        .namespace(namespace)
                                        .workload(workload)
                                        .timestamp(Instant.now())
                                        .resourceVersion(v1ReplicaSet.getMetadata().getResourceVersion())
                                        .build());
    eventDTO.setEventTime(Instant.now().toEpochMilli());
    return eventDTO;
  }
}
