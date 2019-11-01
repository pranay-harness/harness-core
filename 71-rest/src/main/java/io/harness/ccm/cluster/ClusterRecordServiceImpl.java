package io.harness.ccm.cluster;

import static java.util.Objects.isNull;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.observer.Subject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

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
    ClusterRecord prevClusterRecord = clusterRecordDao.get(clusterRecord);
    ClusterRecord upsertedClusterRecord = clusterRecordDao.upsertCluster(clusterRecord);

    if (!isNull(prevClusterRecord)) {
      logger.info("Updated the existing {} Cluster with id={}.", upsertedClusterRecord.getCluster().getClusterType(),
          upsertedClusterRecord.getUuid());
    } else {
      logger.info("Upserted a new {} Cluster with id={}.", upsertedClusterRecord.getCluster().getClusterType(),
          upsertedClusterRecord.getUuid());
      try {
        subject.fireInform(ClusterRecordObxxxxxxxx:onUpserted, clusterRecord);
      } catch (Exception e) {
        logger.error("Failed to inform the observers for the Cluster with id={}", clusterRecord.getUuid(), e);
      }
    }
    return upsertedClusterRecord;
  }

  @Override
  public List<ClusterRecord> list(String accountId, String cloudProviderId) {
    return clusterRecordDao.list(accountId, cloudProviderId);
  }

  @Override
  public ClusterRecord attachPerpetualTaskId(ClusterRecord clusterRecord, String taskId) {
    return clusterRecordDao.insertTask(clusterRecord, taskId);
  }

  @Override
  public ClusterRecord removePerpetualTaskId(ClusterRecord clusterRecord, String taskId) {
    return clusterRecordDao.removeTask(clusterRecord, taskId);
  }

  @Override
  public boolean delete(String accountId, String cloudProviderId) {
    // get the list of Clusters associated with the cloudProvider
    List<ClusterRecord> clusterRecords = clusterRecordDao.list(accountId, cloudProviderId);
    if (isNull(clusterRecords)) {
      logger.warn("Cloud Provider with id={} has no Clusters to be deleted.", cloudProviderId);
    } else {
      for (ClusterRecord clusterRecord : clusterRecords) {
        try {
          subject.fireInform(ClusterRecordObxxxxxxxx:onDeleting, clusterRecord);
        } catch (Exception e) {
          logger.error("Failed to inform the Observers for ClusterRecord with id={}", clusterRecord.getCluster(), e);
        }
      }
    }
    return clusterRecordDao.delete(accountId, cloudProviderId);
  }
}
