package io.harness.perpetualtask.datacollection.k8s;

import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.KubernetesChangeEventMetadata;

import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import java.time.Instant;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@SuperBuilder
@Slf4j
public class ChangeIntelConfigMapHandler extends BaseChangeHandler<V1ConfigMap> {
  @Override
  String getKind() {
    return "ConfigMap";
  }

  @Override
  String getApiVersion() {
    return "apps/v1";
  }

  @Override
  void processAndSendAddEvent(V1ConfigMap v1ConfigMap) {
    log.info("Add configMap event received");

    boolean hasOwner = false;
    for (V1OwnerReference ownerReference : v1ConfigMap.getMetadata().getOwnerReferences()) {
      if (Boolean.TRUE.equals(ownerReference.getController())) {
        hasOwner = true;
      }
    }
    if (!hasOwner && v1ConfigMap.getMetadata() != null) {
      log.info("ConfigMap doesn't have an ownerReference. Sending event Data");
      ChangeEventDTO eventDTO = buildConfigMapChangeEvent(v1ConfigMap);
      String newYaml = k8sHandlerUtils.yamlDump(v1ConfigMap);
      ((KubernetesChangeEventMetadata) eventDTO.getChangeEventMetaData()).setNewYaml(newYaml);
      ((KubernetesChangeEventMetadata) eventDTO.getChangeEventMetaData())
          .setTimestamp(Instant.ofEpochMilli(
              v1ConfigMap.getMetadata().getCreationTimestamp().toDateTime().toInstant().getMillis()));
      ((KubernetesChangeEventMetadata) eventDTO.getChangeEventMetaData())
          .setAction(KubernetesChangeEventMetadata.Action.Add);
    }
  }

  @Override
  void processAndSendUpdateEvent(V1ConfigMap oldResource, V1ConfigMap newResource, String oldYaml, String newYaml) {
    log.info("Update config map event received.");
    String resourceVersion = newResource.getMetadata().getResourceVersion();
    log.info("processAndSendUpdateEvent for ConfigMap. Old ResourceVersion is {} and new is {}",
        oldResource.getMetadata().getResourceVersion(), resourceVersion);
    ChangeEventDTO eventDTO = buildConfigMapChangeEvent(newResource);
    ((KubernetesChangeEventMetadata) eventDTO.getChangeEventMetaData()).setOldYaml(oldYaml);
    ((KubernetesChangeEventMetadata) eventDTO.getChangeEventMetaData()).setNewYaml(newYaml);
    ((KubernetesChangeEventMetadata) eventDTO.getChangeEventMetaData())
        .setAction(KubernetesChangeEventMetadata.Action.Update);
    eventDTO.setEventTime(Instant.now().toEpochMilli());
    sendEvent(accountId, eventDTO);
  }

  private ChangeEventDTO buildConfigMapChangeEvent(V1ConfigMap v1ConfigMap) {
    String workload = v1ConfigMap.getMetadata().getName();
    String namespace = v1ConfigMap.getMetadata().getNamespace();
    ChangeEventDTO eventDTO = buildChangeEventDTOSkeleton();
    eventDTO.setChangeEventMetaData(KubernetesChangeEventMetadata.builder()
                                        .resourceType(KubernetesChangeEventMetadata.KubernetesResourceType.ConfigMap)
                                        .namespace(namespace)
                                        .workload(workload)
                                        .timestamp(Instant.now())
                                        .resourceVersion(v1ConfigMap.getMetadata().getResourceVersion())
                                        .build());
    eventDTO.setEventTime(Instant.now().toEpochMilli());
    return eventDTO;
  }
}
