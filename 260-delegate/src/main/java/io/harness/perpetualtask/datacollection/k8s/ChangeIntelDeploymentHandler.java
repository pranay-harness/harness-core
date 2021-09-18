package io.harness.perpetualtask.datacollection.k8s;

import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.KubernetesChangeEventMetadata;
import io.harness.cvng.beans.change.KubernetesChangeEventMetadata.Action;
import io.harness.cvng.beans.change.KubernetesChangeEventMetadata.KubernetesResourceType;

import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import java.time.Instant;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@SuperBuilder
@Slf4j
public class ChangeIntelDeploymentHandler extends BaseChangeHandler<V1Deployment> {
  @Override
  String getKind() {
    return "Deployment";
  }

  @Override
  String getApiVersion() {
    return "apps/v1";
  }

  @Override
  void processAndSendAddEvent(V1Deployment v1Deployment) {
    log.info("OnAdd of DeploymentChange.");
    boolean hasOwner = false;
    for (V1OwnerReference ownerReference : v1Deployment.getMetadata().getOwnerReferences()) {
      if (Boolean.TRUE.equals(ownerReference.getController())) {
        hasOwner = true;
      }
    }
    if (!hasOwner) {
      log.info("Deployment doesn't have an ownerReference. Sending event Data");
      ChangeEventDTO eventDTO = buildDeploymentChangeEvent(v1Deployment);
      String newYaml = k8sHandlerUtils.yamlDump(v1Deployment);
      ((KubernetesChangeEventMetadata) eventDTO.getChangeEventMetaData()).setNewYaml(newYaml);
      ((KubernetesChangeEventMetadata) eventDTO.getChangeEventMetaData())
          .setTimestamp(Instant.ofEpochMilli(
              v1Deployment.getMetadata().getCreationTimestamp().toDateTime().toInstant().getMillis()));
      ((KubernetesChangeEventMetadata) eventDTO.getChangeEventMetaData()).setAction(Action.Add);
    }
  }

  @Override
  void processAndSendUpdateEvent(V1Deployment oldResource, V1Deployment newResource, String oldYaml, String newYaml) {
    String resourceVersion = newResource.getMetadata().getResourceVersion();
    log.info("processAndSendUpdateEvent for Deployment. Old ResourceVersion is {} and new is {}",
        oldResource.getMetadata().getResourceVersion(), resourceVersion);
    ChangeEventDTO eventDTO = buildDeploymentChangeEvent(newResource);
    ((KubernetesChangeEventMetadata) eventDTO.getChangeEventMetaData()).setOldYaml(oldYaml);
    ((KubernetesChangeEventMetadata) eventDTO.getChangeEventMetaData()).setNewYaml(newYaml);
    ((KubernetesChangeEventMetadata) eventDTO.getChangeEventMetaData()).setAction(Action.Update);
    eventDTO.setEventTime(Instant.now().toEpochMilli());
    sendEvent(accountId, eventDTO);
  }

  private ChangeEventDTO buildDeploymentChangeEvent(V1Deployment deployment) {
    String workload = deployment.getMetadata().getName();
    String namespace = deployment.getMetadata().getNamespace();
    ChangeEventDTO eventDTO = buildChangeEventDTOSkeleton();
    eventDTO.setChangeEventMetaData(KubernetesChangeEventMetadata.builder()
                                        .resourceType(KubernetesResourceType.Deployment)
                                        .namespace(namespace)
                                        .workload(workload)
                                        .timestamp(Instant.now())
                                        .resourceVersion(deployment.getMetadata().getResourceVersion())
                                        .build());
    eventDTO.setEventTime(Instant.now().toEpochMilli());
    return eventDTO;
  }
}
