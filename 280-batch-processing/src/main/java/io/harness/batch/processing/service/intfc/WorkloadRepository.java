package io.harness.batch.processing.service.intfc;

import io.harness.ccm.cluster.entities.K8sWorkload;
import io.harness.ccm.commons.beans.recommendation.ResourceId;
import io.harness.perpetualtask.k8s.watch.PodInfo;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface WorkloadRepository {
  void savePodWorkload(String accountId, PodInfo podInfo);
  List<K8sWorkload> getWorkload(String accountId, String clusterId, Set<String> workloadName);
  Optional<K8sWorkload> getWorkload(String accountId, String clusterId, String uid);
  Optional<K8sWorkload> getWorkload(ResourceId workloadId);
}
