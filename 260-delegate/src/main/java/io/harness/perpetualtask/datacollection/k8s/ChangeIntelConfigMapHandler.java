package io.harness.perpetualtask.datacollection.k8s;

import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.KubernetesChangeEventMetadata;

import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

@SuperBuilder
@Slf4j
public class ChangeIntelConfigMapHandler extends BaseChangeHandler<V1ConfigMap> {
  @Override
  String getKind() {
    return "ConfigMap";
  }

  @Override
  String getApiVersion() {
    return "v1";
  }

  @Override
  boolean hasOwnerReference(V1ConfigMap v1ConfigMap) {
    if (v1ConfigMap.getMetadata().getOwnerReferences() != null) {
      for (V1OwnerReference ownerReference : v1ConfigMap.getMetadata().getOwnerReferences()) {
        if (Boolean.TRUE.equals(ownerReference.getController())) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  void processAndSendAddEvent(V1ConfigMap v1ConfigMap) {
    if (!hasOwnerReference(v1ConfigMap) && v1ConfigMap.getMetadata() != null
        && v1ConfigMap.getMetadata().getCreationTimestamp().isAfter(
            Instant.now().minus(2, ChronoUnit.HOURS).toEpochMilli())) {
      log.info("ConfigMap doesn't have an ownerReference. Sending event Data");
      ChangeEventDTO eventDTO = buildChangeEvent(v1ConfigMap);
      String newYaml = k8sHandlerUtils.yamlDump(v1ConfigMap);
      ((KubernetesChangeEventMetadata) eventDTO.getMetadata()).setNewYaml(newYaml);
      DateTime dateTime = v1ConfigMap.getMetadata().getCreationTimestamp();
      if (dateTime != null) {
        ((KubernetesChangeEventMetadata) eventDTO.getMetadata())
            .setTimestamp(Instant.ofEpochMilli(dateTime.toDateTime().toInstant().getMillis()));
      }
      ((KubernetesChangeEventMetadata) eventDTO.getMetadata()).setAction(KubernetesChangeEventMetadata.Action.Add);
      sendEvent(accountId, eventDTO);
    }
  }

  @Override
  ChangeEventDTO buildChangeEvent(V1ConfigMap v1ConfigMap) {
    String workload = v1ConfigMap.getMetadata().getName();
    String namespace = v1ConfigMap.getMetadata().getNamespace();
    ChangeEventDTO eventDTO = buildChangeEventDTOSkeleton();
    eventDTO.setMetadata(KubernetesChangeEventMetadata.builder()
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
