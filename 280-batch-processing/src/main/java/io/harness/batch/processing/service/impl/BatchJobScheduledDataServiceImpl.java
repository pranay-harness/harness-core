package io.harness.batch.processing.service.impl;

import io.harness.batch.processing.ccm.BatchJobBucket;
import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.dao.intfc.BatchJobScheduledDataDao;
import io.harness.batch.processing.service.intfc.BatchJobScheduledDataService;
import io.harness.ccm.cluster.entities.BatchJobScheduledData;
import io.harness.ccm.health.LastReceivedPublishedMessageDao;

import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BatchJobScheduledDataServiceImpl implements BatchJobScheduledDataService {
  @Autowired private BatchJobScheduledDataDao batchJobScheduledDataDao;
  @Autowired private CloudToHarnessMappingService cloudToHarnessMappingService;
  @Autowired protected LastReceivedPublishedMessageDao lastReceivedPublishedMessageDao;

  private static final int MAX_IN_CLUSTER_DATA = 15;

  @Override
  public boolean create(BatchJobScheduledData batchJobScheduledData) {
    return batchJobScheduledDataDao.create(batchJobScheduledData);
  }

  @Override
  public Instant fetchLastBatchJobScheduledTime(String accountId, BatchJobType batchJobType) {
    Instant instant = fetchLastDependentBatchJobScheduledTime(accountId, batchJobType);
    if (null == instant) {
      if (batchJobType.getBatchJobBucket() == BatchJobBucket.OUT_OF_CLUSTER) {
        SettingAttribute ceConnector = cloudToHarnessMappingService.getFirstSettingAttributeByCategory(
            accountId, SettingAttribute.SettingCategory.CE_CONNECTOR);
        if (null != ceConnector) {
          Instant connectorCreationTime =
              Instant.ofEpochMilli(ceConnector.getCreatedAt()).truncatedTo(ChronoUnit.DAYS).minus(2, ChronoUnit.DAYS);
          if (BatchJobType.AWS_ECS_CLUSTER_SYNC == batchJobType) {
            Instant startInstant = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
            connectorCreationTime = startInstant.isAfter(connectorCreationTime) ? startInstant : connectorCreationTime;
          }

          if (BatchJobType.AWS_ECS_CLUSTER_DATA_SYNC == batchJobType) {
            Instant startInstant = Instant.now().minus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS);
            connectorCreationTime = startInstant.isAfter(connectorCreationTime) ? startInstant : connectorCreationTime;
          }
          return connectorCreationTime;
        }
      } else {
        instant = lastReceivedPublishedMessageDao.getFirstEventReceivedTime(accountId);
      }
    }

    if (null != instant && batchJobType.getBatchJobBucket() != BatchJobBucket.OUT_OF_CLUSTER
        && batchJobType != BatchJobType.INSTANCE_BILLING_AGGREGATION) {
      Instant startInstant = Instant.now().minus(MAX_IN_CLUSTER_DATA, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
      instant = startInstant.isAfter(instant) ? startInstant : instant;
    }

    if (null != instant && BatchJobType.ANOMALY_DETECTION == batchJobType) {
      Instant startInstant = Instant.now().minus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
      instant = startInstant.isAfter(instant) ? startInstant : instant;
    }

    if (null != instant && BatchJobType.AWS_ECS_CLUSTER_SYNC == batchJobType) {
      Instant startInstant = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
      instant = startInstant.isAfter(instant) ? startInstant : instant;
    }

    if (null != instant && BatchJobType.AWS_ECS_CLUSTER_DATA_SYNC == batchJobType) {
      Instant startInstant = Instant.now().minus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS);
      instant = startInstant.isAfter(instant) ? startInstant : instant;
    }

    if (null != instant && batchJobType == BatchJobType.K8S_WORKLOAD_RECOMMENDATION) {
      Instant startInstant = Instant.now().minus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
      instant = startInstant.isAfter(instant) ? startInstant : instant;
    }
    return instant;
  }

  @Override
  public Instant fetchLastDependentBatchJobScheduledTime(String accountId, BatchJobType batchJobType) {
    BatchJobScheduledData batchJobScheduledData =
        batchJobScheduledDataDao.fetchLastBatchJobScheduledData(accountId, batchJobType);
    if (null != batchJobScheduledData) {
      return batchJobScheduledData.getEndAt();
    }
    return null;
  }
}
