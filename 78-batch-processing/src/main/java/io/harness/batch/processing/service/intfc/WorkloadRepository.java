package io.harness.batch.processing.service.intfc;

import io.harness.ccm.cluster.entities.K8sWorkload;
import io.harness.perpetualtask.k8s.watch.PodInfo;

import java.util.Optional;

public interface WorkloadRepository {
  void savePodWorkload(String accountId, PodInfo podInfo);
  Optional<K8sWorkload> getWorkload(String accountId, String clusterId, String uid);
}
