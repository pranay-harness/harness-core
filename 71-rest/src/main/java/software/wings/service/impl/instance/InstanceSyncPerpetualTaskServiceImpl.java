package software.wings.service.impl.instance;

import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import software.wings.api.DeploymentSummary;
import software.wings.beans.InfrastructureMapping;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.instance.InstanceSyncPerpetualTaskInfo.InstanceSyncPerpetualTaskInfoKeys;
import software.wings.service.intfc.instance.InstanceService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class InstanceSyncPerpetualTaskServiceImpl implements InstanceSyncPerpetualTaskService {
  @Inject private InstanceService instanceService;
  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private InstanceHandlerFactory instanceHandlerFactory;

  @Override
  public void createPerpetualTasks(InfrastructureMapping infrastructureMapping) {
    InstanceSyncByPerpetualTaskHandler handler = getInstanceHandler(infrastructureMapping);

    if (!shouldCreatePerpetualTasks(infrastructureMapping)) {
      return;
    }

    List<String> perpetualTaskIds =
        handler.getInstanceSyncPerpetualTaskCreator().createPerpetualTasks(infrastructureMapping);
    if (!perpetualTaskIds.isEmpty()) {
      save(infrastructureMapping.getAccountId(), infrastructureMapping.getUuid(), perpetualTaskIds);
    }
  }

  private InstanceSyncByPerpetualTaskHandler getInstanceHandler(InfrastructureMapping infrastructureMapping) {
    return (InstanceSyncByPerpetualTaskHandler) instanceHandlerFactory.getInstanceHandler(infrastructureMapping);
  }

  private boolean shouldCreatePerpetualTasks(InfrastructureMapping infrastructureMapping) {
    long instanceCount =
        instanceService.getInstanceCount(infrastructureMapping.getAppId(), infrastructureMapping.getUuid());
    return instanceCount > 0 && !perpetualTasksAlreadyExists(infrastructureMapping);
  }

  private boolean perpetualTasksAlreadyExists(InfrastructureMapping infrastructureMapping) {
    Optional<InstanceSyncPerpetualTaskInfo> info =
        getByAccountIdAndInfrastructureMappingId(infrastructureMapping.getAccountId(), infrastructureMapping.getUuid());
    return info.isPresent() && !info.get().getPerpetualTaskIds().isEmpty();
  }

  @Override
  public void createPerpetualTasksForNewDeployment(
      InfrastructureMapping infrastructureMapping, List<DeploymentSummary> deploymentSummaries) {
    InstanceSyncByPerpetualTaskHandler handler = getInstanceHandler(infrastructureMapping);

    List<PerpetualTaskRecord> existingTasks = getExistingPerpetualTasks(infrastructureMapping);

    List<String> newPerpetualTaskIds =
        handler.getInstanceSyncPerpetualTaskCreator().createPerpetualTasksForNewDeployment(
            deploymentSummaries, existingTasks, infrastructureMapping);

    save(infrastructureMapping.getAccountId(), infrastructureMapping.getUuid(), newPerpetualTaskIds);
  }

  private List<PerpetualTaskRecord> getExistingPerpetualTasks(InfrastructureMapping infrastructureMapping) {
    Optional<InstanceSyncPerpetualTaskInfo> info =
        getByAccountIdAndInfrastructureMappingId(infrastructureMapping.getAccountId(), infrastructureMapping.getUuid());
    return info
        .map(instanceSyncPerpetualTaskInfo
            -> instanceSyncPerpetualTaskInfo.getPerpetualTaskIds()
                   .stream()
                   .map(id -> perpetualTaskService.getTaskRecord(id))
                   .collect(Collectors.toList()))
        .orElse(emptyList());
  }

  @Override
  public void deletePerpetualTasks(InfrastructureMapping infrastructureMapping) {
    deletePerpetualTasks(infrastructureMapping.getAccountId(), infrastructureMapping.getUuid());
  }

  @Override
  public void deletePerpetualTasks(String accountId, String infrastructureMappingId) {
    Optional<InstanceSyncPerpetualTaskInfo> info =
        getByAccountIdAndInfrastructureMappingId(accountId, infrastructureMappingId);
    if (!info.isPresent()) {
      return;
    }

    for (String taskId : info.get().getPerpetualTaskIds()) {
      deletePerpetualTask(accountId, infrastructureMappingId, taskId);
    }
  }

  @Override
  public void resetPerpetualTask(String accountId, String perpetualTaskId) {
    perpetualTaskService.resetTask(accountId, perpetualTaskId);
  }

  private Optional<InstanceSyncPerpetualTaskInfo> getByAccountIdAndInfrastructureMappingId(
      String accountId, String infrastructureMappingId) {
    Query<InstanceSyncPerpetualTaskInfo> query = getInfoQuery(accountId, infrastructureMappingId);
    return ofNullable(query.get());
  }

  private Query<InstanceSyncPerpetualTaskInfo> getInfoQuery(String accountId, String infrastructureMappingId) {
    return wingsPersistence.createQuery(InstanceSyncPerpetualTaskInfo.class)
        .filter(InstanceSyncPerpetualTaskInfoKeys.accountId, accountId)
        .filter(InstanceSyncPerpetualTaskInfoKeys.infrastructureMappingId, infrastructureMappingId);
  }

  private void save(InstanceSyncPerpetualTaskInfo info) {
    wingsPersistence.save(info);
  }

  private void delete(String infrastructureMappingId) {
    Query<InstanceSyncPerpetualTaskInfo> deleteQuery =
        wingsPersistence.createQuery(InstanceSyncPerpetualTaskInfo.class, excludeAuthority)
            .filter(InstanceSyncPerpetualTaskInfoKeys.infrastructureMappingId, infrastructureMappingId);

    wingsPersistence.delete(deleteQuery);
  }

  @Override
  public void deletePerpetualTask(String accountId, String infrastructureMappingId, String perpetualTaskId) {
    perpetualTaskService.deleteTask(accountId, perpetualTaskId);

    Optional<InstanceSyncPerpetualTaskInfo> optionalInfo =
        getByAccountIdAndInfrastructureMappingId(accountId, infrastructureMappingId);
    if (!optionalInfo.isPresent()) {
      return;
    }
    InstanceSyncPerpetualTaskInfo info = optionalInfo.get();
    boolean wasFound = info.getPerpetualTaskIds().remove(perpetualTaskId);
    if (!wasFound) {
      return;
    }
    if (info.getPerpetualTaskIds().isEmpty()) {
      delete(infrastructureMappingId);
    } else {
      save(info);
    }
  }

  private void save(String accountId, String infrastructureMappingId, List<String> perpetualTaskIds) {
    Optional<InstanceSyncPerpetualTaskInfo> infoOptional =
        getByAccountIdAndInfrastructureMappingId(accountId, infrastructureMappingId);
    if (!infoOptional.isPresent()) {
      save(InstanceSyncPerpetualTaskInfo.builder()
               .accountId(accountId)
               .infrastructureMappingId(infrastructureMappingId)
               .perpetualTaskIds(perpetualTaskIds)
               .build());
    } else {
      InstanceSyncPerpetualTaskInfo info = infoOptional.get();
      info.getPerpetualTaskIds().addAll(perpetualTaskIds);
      save(info);
    }
  }
}
