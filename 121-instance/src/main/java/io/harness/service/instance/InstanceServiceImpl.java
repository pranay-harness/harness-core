package io.harness.service.instance;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.dto.SyncStatus;
import io.harness.dto.instance.Instance;
import io.harness.repository.syncstatus.SyncStatusRepository;

import software.wings.beans.infrastructure.instance.ContainerDeploymentInfo;
import software.wings.beans.infrastructure.instance.ManualSyncJob;

import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class InstanceServiceImpl implements InstanceService {
  private SyncStatusRepository syncStatusRepository;

  @Override
  public Instance save(Instance instance) {
    return null;
  }

  @Override
  public List<Instance> saveOrUpdate(List<Instance> instances) {
    return null;
  }

  @Override
  public Instance get(String instanceId, boolean includeDeleted) {
    return null;
  }

  @Override
  public Instance saveOrUpdate(Instance instance) {
    return null;
  }

  @Override
  public Instance update(Instance instance, String oldInstanceId) {
    return null;
  }

  @Override
  public boolean delete(Set<String> instanceIdSet) {
    return false;
  }

  @Override
  public boolean purgeDeletedUpTo(Instant timestamp) {
    return false;
  }

  @Override
  public List<ContainerDeploymentInfo> getContainerDeploymentInfoList(String containerSvcNameNoRevision, String appId) {
    return null;
  }

  @Override
  public PageResponse<Instance> list(PageRequest<Instance> pageRequest) {
    return null;
  }

  @Override
  public List<Instance> listInstancesNotRemovedFully(Query<Instance> query) {
    return null;
  }

  @Override
  public void updateSyncSuccess(
      String appId, String serviceId, String envId, String infraMappingId, String infraMappingName, long timestamp) {}

  @Override
  public boolean handleSyncFailure(String orgId, String projectId, String serviceId, String envId,
      String infraMappingId, String infraMappingName, long timestamp, String errorMsg) {
    SyncStatus syncStatus = syncStatusRepository.getSyncStatus(orgId, projectId, serviceId, envId, infraMappingId);
    if (syncStatus != null) {
      if ((timestamp - syncStatus.getLastSuccessfullySyncedAt()) >= Duration.ofDays(7).toMillis()) {
        log.info("Deleting the instances since sync has been failing for more than a week for infraMappingId: {}",
            infraMappingId);
        syncStatusRepository.deleteById(syncStatus.getId());
        //        pruneByInfrastructureMapping(appId, infraMappingId);
        return false;
      }
    }
    return true;
  }

  @Override
  public List<SyncStatus> getSyncStatus(String appId, String serviceId, String envId) {
    return null;
  }

  @Override
  public void saveManualSyncJob(ManualSyncJob manualSyncJob) {}

  @Override
  public void deleteManualSyncJob(String appId, String manualSyncJobId) {}

  @Override
  public List<Boolean> getManualSyncJobsStatus(String accountId, Set<String> manualJobIdSet) {
    return null;
  }

  @Override
  public List<Instance> getInstancesForAppAndInframappingNotRemovedFully(String appId, String infraMappingId) {
    return null;
  }

  @Override
  public List<Instance> getInstancesForAppAndInframapping(String appId, String infraMappingId) {
    return null;
  }

  @Override
  public long getInstanceCount(String appId, String infraMappingId) {
    return 0;
  }

  @Override
  public void deleteByAccountId(String accountId) {}

  @Override
  public void pruneByApplication(String appId) {}

  @Override
  public void pruneByEnvironment(String appId, String envId) {}

  @Override
  public void pruneByInfrastructureMapping(String appId, String infrastructureMappingId) {}

  @Override
  public void pruneByService(String appId, String serviceId) {}
}
