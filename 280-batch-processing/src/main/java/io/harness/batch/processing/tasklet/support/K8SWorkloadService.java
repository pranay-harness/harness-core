package io.harness.batch.processing.tasklet.support;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.harness.batch.processing.service.intfc.WorkloadRepository;
import io.harness.ccm.cluster.entities.K8sWorkload;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
