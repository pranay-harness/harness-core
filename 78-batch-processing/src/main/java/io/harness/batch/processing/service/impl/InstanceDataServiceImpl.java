package io.harness.batch.processing.service.impl;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.harness.batch.processing.billing.timeseries.data.PrunedInstanceData;
import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.dao.intfc.InstanceDataDao;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.entities.InstanceData.InstanceDataKeys;
import io.harness.batch.processing.service.intfc.InstanceDataService;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class InstanceDataServiceImpl implements InstanceDataService {
  @Autowired private InstanceDataDao instanceDataDao;

  LoadingCache<CacheKey, PrunedInstanceData> instanceDataCache =
      Caffeine.newBuilder()
          .expireAfterWrite(24, TimeUnit.HOURS)
          .build(key -> pruneInstanceData(key.accountId, key.clusterId, key.instanceId, key.occurredAt));

  @Value
  private static class CacheKey {
    private String accountId;
    private String clusterId;
    private String instanceId;
    @EqualsAndHashCode.Exclude private long occurredAt;
  }

  @Override
  public boolean create(InstanceData instanceData) {
    return instanceDataDao.create(instanceData);
  }

  @Override
  public boolean updateInstanceState(InstanceData instanceData, Instant instant, InstanceState instanceState) {
    String instantField = null;
    if (InstanceState.RUNNING == instanceState) {
      instantField = InstanceDataKeys.usageStartTime;
    } else if (InstanceState.STOPPED == instanceState) {
      instantField = InstanceDataKeys.usageStopTime;
    }

    if (null != instantField) {
      return instanceDataDao.updateInstanceState(instanceData, instant, instantField, instanceState);
    }
    return false;
  }

  @Override
  public InstanceData fetchActiveInstanceData(
      String accountId, String clusterId, String instanceId, List<InstanceState> instanceState) {
    return instanceDataDao.fetchActiveInstanceData(accountId, clusterId, instanceId, instanceState);
  }

  @Override
  public InstanceData fetchInstanceData(String accountId, String instanceId) {
    return instanceDataDao.fetchInstanceData(accountId, instanceId);
  }

  @Override
  public InstanceData fetchInstanceDataWithName(
      String accountId, String clusterId, String instanceId, Long occurredAt) {
    return instanceDataDao.fetchInstanceDataWithName(accountId, clusterId, instanceId, occurredAt);
  }

  @Override
  public List<InstanceData> fetchClusterActiveInstanceData(String accountId, String clusterId, Instant startTime) {
    List<InstanceState> instanceStates =
        new ArrayList<>(Arrays.asList(InstanceState.INITIALIZING, InstanceState.RUNNING));
    return instanceDataDao.fetchClusterActiveInstanceData(accountId, clusterId, instanceStates, startTime);
  }

  public PrunedInstanceData fetchPrunedInstanceDataWithName(
      String accountId, String clusterId, String instanceId, Long occurredAt) {
    final CacheKey cacheKey = new CacheKey(accountId, clusterId, instanceId, occurredAt);
    return instanceDataCache.get(cacheKey);
  }

  private PrunedInstanceData pruneInstanceData(String accountId, String clusterId, String instanceId, Long occurredAt) {
    InstanceData instanceData = instanceDataDao.fetchInstanceDataWithName(accountId, clusterId, instanceId, occurredAt);
    if (null != instanceData) {
      return PrunedInstanceData.builder()
          .instanceId(instanceData.getInstanceId())
          .totalResource(instanceData.getTotalResource())
          .build();
    } else {
      logger.error("Instance detail not found clusterId {} instanceId {}", clusterId, instanceId);
      return PrunedInstanceData.builder().build();
    }
  }
}
