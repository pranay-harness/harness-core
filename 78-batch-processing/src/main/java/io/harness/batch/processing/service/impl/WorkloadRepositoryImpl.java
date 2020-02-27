package io.harness.batch.processing.service.impl;

import static io.harness.ccm.cluster.entities.K8sWorkload.encodeDotsInKey;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.harness.batch.processing.service.intfc.WorkloadRepository;
import io.harness.ccm.cluster.entities.K8sWorkload;
import io.harness.ccm.cluster.entities.K8sWorkload.K8sWorkloadKeys;
import io.harness.perpetualtask.k8s.watch.Owner;
import io.harness.perpetualtask.k8s.watch.PodInfo;
import io.harness.persistence.HPersistence;
import lombok.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class WorkloadRepositoryImpl implements WorkloadRepository {
  private final HPersistence hPersistence;
  private final Cache<CacheKey, Boolean> saved = Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(1)).build();

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
}
