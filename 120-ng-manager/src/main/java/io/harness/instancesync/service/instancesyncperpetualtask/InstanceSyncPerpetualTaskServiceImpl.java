package io.harness.instancesync.service.instancesyncperpetualtask;

import static java.util.Optional.ofNullable;

import io.harness.data.structure.EmptyPredicate;
import io.harness.ff.FeatureFlagService;
import io.harness.instancesync.entity.DeploymentSummary;
import io.harness.instancesync.entity.infrastructureMapping.InfrastructureMapping;
import io.harness.instancesync.service.IInstanceSyncByPerpetualTaskhandler;
import io.harness.instancesync.service.InstanceHandler;
import io.harness.instancesync.service.instance.InstanceService;
import io.harness.instancesync.service.instancehandlerfactory.InstanceHandlerFactoryService;
import io.harness.perpetualtask.PerpetualTaskService;

import software.wings.dl.WingsPersistence;
import software.wings.service.impl.instance.InstanceSyncByPerpetualTaskHandler;
import software.wings.service.impl.instance.InstanceSyncPerpetualTaskInfo;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import org.mongodb.morphia.query.Query;

public class InstanceSyncPerpetualTaskServiceImpl implements InstanceSyncPerpetualTaskService {
  @Inject private InstanceService instanceService;
  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private InstanceHandlerFactoryService instanceHandlerFactory;
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public void createPerpetualTasks(InfrastructureMapping infrastructureMapping) {
    if (!shouldCreatePerpetualTasks(infrastructureMapping)) {
      return;
    }
    IInstanceSyncByPerpetualTaskhandler handler = getInstanceHandler(infrastructureMapping);
    if (handler == null) {
      // TODO handle it gracefully with logs
      return;
    }

    List<String> perpetualTaskIds =
        handler.getInstanceSyncPerpetualTaskCreator().createPerpetualTasks(infrastructureMapping);
    if (!perpetualTaskIds.isEmpty()) {
      save(infrastructureMapping.getAccountId(), infrastructureMapping.getId(), perpetualTaskIds);
    }
  }

  @Override
  public void createPerpetualTasksForNewDeployment(
      InfrastructureMapping infrastructureMapping, DeploymentSummary deploymentSummary) {}

  @Override
  public void deletePerpetualTasks(InfrastructureMapping infrastructureMapping) {}

  @Override
  public void deletePerpetualTasks(String accountId, String infrastructureMappingId) {}

  @Override
  public void resetPerpetualTask(String accountId, String perpetualTaskId) {}

  @Override
  public void deletePerpetualTask(String accountId, String infrastructureMappingId, String perpetualTaskId) {}

  @Override
  public boolean isInstanceSyncByPerpetualTaskEnabled(InfrastructureMapping infrastructureMapping) {
    InstanceHandler instanceHandler = getInstanceHandler(infrastructureMapping);
    if (instanceHandler == null) {
      return false;
    }

    if (instanceHandler instanceof InstanceSyncByPerpetualTaskHandler) {
      InstanceSyncByPerpetualTaskHandler handler = (InstanceSyncByPerpetualTaskHandler) instanceHandler;
      return featureFlagService.isEnabled(
          handler.getFeatureFlagToEnablePerpetualTaskForInstanceSync(), infrastructureMapping.getAccountId());
    }

    return false;
  }

  // ---------------------- PRIVATE METHODS -----------------------

  private InstanceHandler getInstanceHandler(InfrastructureMapping infrastructureMapping) {
    try {
      return instanceHandlerFactory.getInstanceHandler(infrastructureMapping);
    } catch (Exception ex) {
      return null;
    }
  }

  private boolean shouldCreatePerpetualTasks(InfrastructureMapping infrastructureMapping) {
    // TODO Fix the method acc to NG
    long instanceCount =
        instanceService.getInstanceCount(infrastructureMapping.getAppId(), infrastructureMapping.getId());
    return instanceCount > 0 && !perpetualTasksAlreadyExists(infrastructureMapping);
  }

  private boolean perpetualTasksAlreadyExists(InfrastructureMapping infrastructureMapping) {
    Optional<InstanceSyncPerpetualTaskInfo> info =
        getByAccountIdAndInfrastructureMappingId(infrastructureMapping.getAccountId(), infrastructureMapping.getId());
    return info.isPresent() && !info.get().getPerpetualTaskIds().isEmpty();
  }

  private Optional<InstanceSyncPerpetualTaskInfo> getByAccountIdAndInfrastructureMappingId(
      String accountId, String infrastructureMappingId) {
    Query<InstanceSyncPerpetualTaskInfo> query = getInfoQuery(accountId, infrastructureMappingId);
    return ofNullable(query.get());
  }

  private Query<InstanceSyncPerpetualTaskInfo> getInfoQuery(String accountId, String infrastructureMappingId) {
    return wingsPersistence.createQuery(InstanceSyncPerpetualTaskInfo.class)
        .filter(InstanceSyncPerpetualTaskInfo.InstanceSyncPerpetualTaskInfoKeys.accountId, accountId)
        .filter(InstanceSyncPerpetualTaskInfo.InstanceSyncPerpetualTaskInfoKeys.infrastructureMappingId,
            infrastructureMappingId);
  }

  private void save(String accountId, String infrastructureMappingId, List<String> perpetualTaskIds) {
    Preconditions.checkArgument(
        EmptyPredicate.isNotEmpty(perpetualTaskIds), "perpetualTaskIds must not be empty or null");
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

  private void save(InstanceSyncPerpetualTaskInfo info) {
    wingsPersistence.save(info);
  }
}
