package io.harness.batch.processing.service.impl;

import static io.harness.ccm.cluster.entities.K8sWorkload.encodeDotsInKey;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.harness.batch.processing.config.k8s.recommendation.ResourceId;
import io.harness.batch.processing.service.intfc.WorkloadRepository;
import io.harness.ccm.cluster.entities.K8sWorkload;
import io.harness.ccm.cluster.entities.K8sWorkload.K8sWorkloadKeys;
import io.harness.perpetualtask.k8s.watch.Owner;
import io.harness.perpetualtask.k8s.watch.PodInfo;
import io.harness.persistence.HPersistence;
import lombok.Value;
import org.mongodb.morphia.query.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class WorkloadRepositoryImpl implements WorkloadRepository {
  private final HPersistence hPersistence;
  private final Cache<CacheKey, Boolean> saved = Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(20)).build();

  @Value
  private static class CacheKey {
    String clusterId;
    String uid;
  }

  @Autowired
  public WorkloadRepositoryImpl(HPersistence hPersistence) {
    this.hPersistence = hPersistence;
  }

  @Override
  public void savePodWorkload(String accountId, PodInfo podInfo) {
    Owner topLevelOwner = podInfo.getTopLevelOwner();
    if (isNotEmpty(topLevelOwner.getLabelsMap())) {
      final CacheKey cacheKey = new CacheKey(podInfo.getClusterId(), topLevelOwner.getUid());
      saved.get(cacheKey,
          key
          -> (hPersistence.upsert(hPersistence.createQuery(K8sWorkload.class)
                                      .field(K8sWorkloadKeys.clusterId)
                                      .equal(key.clusterId)
                                      .field(K8sWorkloadKeys.uid)
                                      .equal(key.uid),
                 hPersistence.createUpdateOperations(K8sWorkload.class)
                     .set(K8sWorkloadKeys.accountId, accountId)
                     .set(K8sWorkloadKeys.clusterId, podInfo.getClusterId())
                     .set(K8sWorkloadKeys.settingId, podInfo.getCloudProviderId())
                     .set(K8sWorkloadKeys.name, topLevelOwner.getName())
                     .set(K8sWorkloadKeys.namespace, podInfo.getNamespace())
                     .set(K8sWorkloadKeys.uid, topLevelOwner.getUid())
                     .set(K8sWorkloadKeys.kind, topLevelOwner.getKind())
                     .set(K8sWorkloadKeys.labels, encodeDotsInKey(topLevelOwner.getLabelsMap())),
                 HPersistence.upsertReturnNewOptions))
              != null);
    }
  }

  @Override
  public Optional<K8sWorkload> getWorkload(String accountId, String clusterId, String uid) {
    return Optional.ofNullable(hPersistence.createQuery(K8sWorkload.class)
                                   .field(K8sWorkloadKeys.accountId)
                                   .equal(accountId)
                                   .field(K8sWorkloadKeys.clusterId)
                                   .equal(clusterId)
                                   .field(K8sWorkloadKeys.uid)
                                   .equal(uid)
                                   .get());
  }

  /**
   * Fetch workload matching given parameters. If there are multiple matches (different uid), fetch the latest.
   * @param workloadId details to id the workload uniquely.
   * @return the workload.
   */
  @Override
  public Optional<K8sWorkload> getWorkload(ResourceId workloadId) {
    return Optional.ofNullable(hPersistence.createQuery(K8sWorkload.class)
                                   .field(K8sWorkloadKeys.accountId)
                                   .equal(workloadId.getAccountId())
                                   .field(K8sWorkloadKeys.clusterId)
                                   .equal(workloadId.getClusterId())
                                   .field(K8sWorkloadKeys.namespace)
                                   .equal(workloadId.getNamespace())
                                   .field(K8sWorkloadKeys.name)
                                   .equal(workloadId.getName())
                                   .field(K8sWorkloadKeys.kind)
                                   .equal(workloadId.getKind())
                                   .order(Sort.descending(K8sWorkloadKeys.lastUpdatedAt))
                                   .get());
  }
}
