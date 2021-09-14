/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.service.intfc;

import io.harness.ccm.commons.beans.recommendation.ResourceId;
import io.harness.ccm.commons.entities.k8s.K8sWorkload;
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
