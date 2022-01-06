package io.harness.batch.processing.tasklet.support;

import io.harness.batch.processing.service.intfc.WorkloadRepository;
import io.harness.ccm.commons.entities.k8s.K8sWorkload;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import lombok.EqualsAndHashCode;
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
  @EqualsAndHashCode
  public static class CacheKey {
    private String accountId;
    private String clusterId;
    private String namespace;
    @Nullable private String workloadName;
  }

  public void updateK8sWorkloadLabelCache(CacheKey key, Set<String> workloadNames) {
    List<K8sWorkload> workloads =
        workloadRepository.getWorkload(key.getAccountId(), key.getClusterId(), key.getNamespace(), workloadNames);

    // reverse looping because, lastUpdatedAt is in descending order, so we want to overwrite old data with latest one
    // in case of duplicates
    for (int i = workloads.size() - 1; i >= 0; i--) {
      workloadLabelCache.put(new CacheKey(workloads.get(i).getAccountId(), workloads.get(i).getClusterId(),
                                 workloads.get(i).getNamespace(), workloads.get(i).getName()),
          workloads.get(i).getLabels());
    }
  }

  public Map<String, String> getK8sWorkloadLabel(
      String accountId, String clusterId, String namespace, String workloadName) {
    return workloadLabelCache.getIfPresent(new CacheKey(accountId, clusterId, namespace, workloadName));
  }
}
