package io.harness.ccm.cluster;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.observer.Subject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ClusterRecordServiceImpl implements ClusterRecordService {
  private final ClusterRecordDao clusterRecordDao;
  @Inject @Getter private Subject<ClusterRecordObserver> subject = new Subject<>();

  @Inject
  public ClusterRecordServiceImpl(ClusterRecordDao clusterRecordDao) {
    this.clusterRecordDao = clusterRecordDao;
  }

  @Override
  public ClusterRecord upsert(ClusterRecord clusterRecord) {
    subject.fireInform(ClusterRecordObxxxxxxxx:onUpserted, clusterRecord);
    clusterRecordDao.upsert(clusterRecord);
    logger.info(
        "Upserted the {} Cluster with id={}.", clusterRecord.getCluster().getClusterType(), clusterRecord.getUuid());
    return clusterRecord;
  }

  @Override
  public boolean delete(String accountId, String cloudProviderId) {
    subject.fireInform(ClusterRecordObxxxxxxxx:onDeleted, accountId, cloudProviderId);
    boolean success = clusterRecordDao.delete(accountId, cloudProviderId);
    if (success) {
      logger.info("Deleted all the Clusters associated with cloudProviderId={}.", cloudProviderId);
      return true;
    }
    return false;
  }
}
