package io.harness.batch.processing.config.k8s.resource.change;

import io.harness.ccm.cluster.entities.K8sYaml;
import io.harness.perpetualtask.k8s.watch.K8sWatchEvent;
import lombok.Value;

@Value(staticConstructor = "of")
public class WorkloadEventId {
  String oldYamlRef;
  String newYamlRef;

  public static WorkloadEventId of(String accountId, K8sWatchEvent k8sWatchEvent) {
    String clusterId = k8sWatchEvent.getClusterId();
    String uid = k8sWatchEvent.getResourceRef().getUid();
    String oldYaml = k8sWatchEvent.getOldResourceYaml();
    String newYaml = k8sWatchEvent.getNewResourceYaml();
    String oldYamlRef = K8sYaml.hash(accountId, clusterId, uid, oldYaml);
    String newYamlRef = K8sYaml.hash(accountId, clusterId, uid, newYaml);
    return WorkloadEventId.of(oldYamlRef, newYamlRef);
  }
}
