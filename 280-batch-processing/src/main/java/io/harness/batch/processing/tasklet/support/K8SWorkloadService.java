/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.tasklet.support;

import io.harness.batch.processing.service.intfc.WorkloadRepository;
import io.harness.ccm.commons.entities.k8s.K8sWorkload;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class K8SWorkloadService {
  @Autowired private WorkloadRepository workloadRepository;
  private Cache<CacheKey, Map<String, String>> workloadLabelCache =
      Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();

  @Value
  private static class CacheKey {
    private String accountId;
    private String clusterId;
    private String workloadName;
  }

  public void updateK8sWorkloadLabelCache(String accountId, String clusterId, Set<String> workloadNames) {
    List<K8sWorkload> workloads = workloadRepository.getWorkload(accountId, clusterId, workloadNames);
    workloads.forEach(workload
        -> workloadLabelCache.put(
            new CacheKey(workload.getAccountId(), workload.getClusterId(), workload.getName()), workload.getLabels()));
  }

  public Map<String, String> getK8sWorkloadLabel(String accountId, String clusterId, String workloadName) {
    return workloadLabelCache.getIfPresent(new CacheKey(accountId, clusterId, workloadName));
  }
}
