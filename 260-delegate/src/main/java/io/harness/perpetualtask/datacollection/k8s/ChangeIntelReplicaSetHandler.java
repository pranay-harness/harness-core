package io.harness.perpetualtask.datacollection.k8s;

import io.harness.cvng.beans.K8ActivityDataCollectionInfo;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.ChangeSourceType;
import io.harness.cvng.beans.change.KubernetesChangeEventMetadata;

import io.kubernetes.client.openapi.models.V1ReplicaSet;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChangeIntelReplicaSetHandler extends BaseChangeHandler<V1ReplicaSet> {
  public ChangeIntelReplicaSetHandler(String accountId, K8ActivityDataCollectionInfo dataCollectionInfo) {
    super(accountId, dataCollectionInfo);
  }

  @Override
  String getKind() {
    return "ReplicaSet";
  }

  @Override
  String getApiVersion() {
    return "apps/v1";
  }

  @Override
  public void onAdd(V1ReplicaSet v1ReplicaSet) {
    log.info("OnAdd of ReplicaSet.");
  }

  @Override
  void processAndSendUpdateEvent(V1ReplicaSet oldResource, V1ReplicaSet newResource) {
    log.info("processAndSendUpdateEvent for ReplicaSet");
    String oldYaml = k8sHandlerUtils.yamlDump(oldResource);
    String newYaml = k8sHandlerUtils.yamlDump(newResource);
    String workload = newResource.getMetadata().getName();
    String namespace = newResource.getMetadata().getNamespace();
    String resourceVersion = newResource.getMetadata().getResourceVersion();
    log.info("processAndSendUpdateEvent for ReplicaSet. Old ResourceVersion is {} and new is {}",
        oldResource.getMetadata().getResourceVersion(), resourceVersion);
    ChangeEventDTO eventDTO = buildChangeEventDTOSkeleton();
    eventDTO.setChangeEventMetaData(KubernetesChangeEventMetadata.builder()
                                        .resourceType(KubernetesChangeEventMetadata.KubernetesResourceType.ReplicaSet)
                                        .action(KubernetesChangeEventMetadata.Action.Update)
                                        .oldYaml(oldYaml)
                                        .newYaml(newYaml)
                                        .namespace(namespace)
                                        .workload(workload)
                                        .timestamp(Instant.now())
                                        .build());
    eventDTO.setEventTime(Instant.now().toEpochMilli());
    sendEvent(accountId, eventDTO);
  }
}
