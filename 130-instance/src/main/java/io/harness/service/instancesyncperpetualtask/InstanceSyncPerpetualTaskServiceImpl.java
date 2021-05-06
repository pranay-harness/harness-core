package io.harness.service.instancesyncperpetualtask;

import static java.util.Collections.emptyList;

import io.harness.data.structure.EmptyPredicate;
import io.harness.dto.DeploymentSummary;
import io.harness.dto.InstanceSyncPerpetualTaskInfo;
import io.harness.dto.infrastructureMapping.InfrastructureMapping;
import io.harness.ff.FeatureFlagService;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.repository.instancesyncperpetualtask.InstanceSyncPerpetualTaskRepository;
import io.harness.service.IInstanceSyncByPerpetualTaskhandler;
import io.harness.service.InstanceHandler;
import io.harness.service.instance.InstanceService;
import io.harness.service.instancehandlerfactory.InstanceHandlerFactoryService;

import software.wings.service.impl.instance.InstanceSyncByPerpetualTaskHandler;

import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class InstanceSyncPerpetualTaskServiceImpl implements InstanceSyncPerpetualTaskService {
  @Inject private InstanceService instanceService;
  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private InstanceHandlerFactoryService instanceHandlerFactory;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private InstanceSyncPerpetualTaskRepository instanceSyncPerpetualTaskRepository;

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
      instanceSyncPerpetualTaskRepository.save(
          infrastructureMapping.getAccountId(), infrastructureMapping.getId(), perpetualTaskIds);
    }
  }

  @Override
  public void createPerpetualTasksForNewDeployment(
      InfrastructureMapping infrastructureMapping, DeploymentSummary deploymentSummary) {
    InstanceHandler handler = getInstanceHandler(infrastructureMapping);
    if (handler == null) {
      // TODO handle it gracefully with logs
      return;
    }

    List<PerpetualTaskRecord> existingTasks = getExistingPerpetualTasks(infrastructureMapping);

    List<String> newPerpetualTaskIds =
        handler.getInstanceSyncPerpetualTaskCreator().createPerpetualTasksForNewDeployment(
            deploymentSummary, existingTasks, infrastructureMapping);

    if (EmptyPredicate.isNotEmpty(newPerpetualTaskIds)) {
      instanceSyncPerpetualTaskRepository.save(
          infrastructureMapping.getAccountId(), infrastructureMapping.getId(), newPerpetualTaskIds);
    }
  }

  @Override
  public void deletePerpetualTasks(InfrastructureMapping infrastructureMapping) {
    deletePerpetualTasks(infrastructureMapping.getAccountId(), infrastructureMapping.getId());
  }

  @Override
  public void deletePerpetualTasks(String accountId, String infrastructureMappingId) {
    Optional<InstanceSyncPerpetualTaskInfo> info =
        instanceSyncPerpetualTaskRepository.getByAccountIdAndInfrastructureMappingId(
            accountId, infrastructureMappingId);
    if (!info.isPresent()) {
      return;
    }

    for (String taskId : info.get().getPerpetualTaskIds()) {
      deletePerpetualTask(accountId, infrastructureMappingId, taskId);
    }
  }

  public void resetPerpetualTask(String accountId, String perpetualTaskId) {
    perpetualTaskService.resetTask(accountId, perpetualTaskId, null);
  }

  @Override
  public void deletePerpetualTask(String accountId, String infrastructureMappingId, String perpetualTaskId) {
    perpetualTaskService.deleteTask(accountId, perpetualTaskId);

    Optional<InstanceSyncPerpetualTaskInfo> optionalInfo =
        instanceSyncPerpetualTaskRepository.getByAccountIdAndInfrastructureMappingId(
            accountId, infrastructureMappingId);
    if (!optionalInfo.isPresent()) {
      return;
    }
    InstanceSyncPerpetualTaskInfo info = optionalInfo.get();
    boolean wasFound = info.getPerpetualTaskIds().remove(perpetualTaskId);
    if (!wasFound) {
      return;
    }
    if (info.getPerpetualTaskIds().isEmpty()) {
      instanceSyncPerpetualTaskRepository.delete(infrastructureMappingId);
    } else {
      instanceSyncPerpetualTaskRepository.save(info);
    }
  }

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
    long instanceCount = 0;
    //        instanceService.getInstanceCount(infrastructureMapping.getAppId(), infrastructureMapping.getId());
    return instanceCount > 0 && !perpetualTasksAlreadyExists(infrastructureMapping);
  }

  private boolean perpetualTasksAlreadyExists(InfrastructureMapping infrastructureMapping) {
    Optional<InstanceSyncPerpetualTaskInfo> info =
        instanceSyncPerpetualTaskRepository.getByAccountIdAndInfrastructureMappingId(
            infrastructureMapping.getAccountId(), infrastructureMapping.getId());
    return info.isPresent() && !info.get().getPerpetualTaskIds().isEmpty();
  }

  private List<PerpetualTaskRecord> getExistingPerpetualTasks(InfrastructureMapping infrastructureMapping) {
    Optional<InstanceSyncPerpetualTaskInfo> info =
        instanceSyncPerpetualTaskRepository.getByAccountIdAndInfrastructureMappingId(
            infrastructureMapping.getAccountId(), infrastructureMapping.getId());
    return info
        .map(instanceSyncPerpetualTaskInfo
            -> instanceSyncPerpetualTaskInfo.getPerpetualTaskIds()
                   .stream()
                   .map(id -> perpetualTaskService.getTaskRecord(id))
                   .collect(Collectors.toList()))
        .orElse(emptyList());
  }
}
