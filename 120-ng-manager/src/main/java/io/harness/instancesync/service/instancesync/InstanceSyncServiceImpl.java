package io.harness.instancesync.service.instancesync;

import static io.harness.validation.Validator.notNullCheck;

import static software.wings.beans.InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH;
import static software.wings.beans.InfrastructureMappingType.PHYSICAL_DATA_CENTER_WINRM;

import io.harness.instancesync.entity.DeploymentEvent;
import io.harness.instancesync.entity.DeploymentSummary;
import io.harness.instancesync.entity.deploymentinfo.OnDemandRollbackInfo;
import io.harness.instancesync.entity.infrastructureMapping.InfrastructureMapping;
import io.harness.instancesync.service.InstanceHandler;
import io.harness.instancesync.service.instancehandlerfactory.InstanceHandlerFactoryService;
import io.harness.instancesync.service.instancesyncperpetualtask.InstanceSyncPerpetualTaskService;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;

import software.wings.beans.InfrastructureMappingType;
import software.wings.utils.Utils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InstanceSyncServiceImpl implements InstanceSyncService {
  @Inject private PersistentLocker persistentLocker;
  @Inject private InstanceHandlerFactoryService instanceHandlerFactory;
  @Inject private InstanceSyncPerpetualTaskService instanceSyncPerpetualTaskService;

  public void processDeploymentEvent(DeploymentEvent deploymentEvent) {
    try {
      DeploymentSummary deploymentSummary = deploymentEvent.getDeploymentSummary();

      // TODO save deployment summary if required

      processDeploymentSummary(
          deploymentSummary, deploymentEvent.isRollback(), deploymentEvent.getOnDemandRollbackInfo());
    } catch (Exception ex) {
      log.error("Error while processing deployment event {}. Skipping the deployment event", deploymentEvent.getId());
    }
  }

  // ------------------------- PRIVATE METHODS ---------------------------

  private void processDeploymentSummary(
      DeploymentSummary deploymentSummary, boolean isRollback, OnDemandRollbackInfo onDemandRollbackInfo) {
    String infraMappingId = deploymentSummary.getInfrastructureMapping().getId();
    try (AcquiredLock lock = persistentLocker.waitToAcquireLock(
             InfrastructureMapping.class, infraMappingId, Duration.ofSeconds(200), Duration.ofSeconds(220))) {
      //            log.info("Handling deployment event for infraMappingId [{}] of appId [{}]", infraMappingId, appId);

      InfrastructureMapping infraMapping = deploymentSummary.getInfrastructureMapping();
      notNullCheck("Infra mapping is null for the given id: " + infraMappingId, infraMapping);

      InfrastructureMappingType infrastructureMappingType =
          Utils.getEnumFromString(InfrastructureMappingType.class, infraMapping.getInfraMappingType());
      Preconditions.checkNotNull(infrastructureMappingType, "InfrastructureMappingType should not be null");
      if (isSupported(infrastructureMappingType)) {
        InstanceHandler instanceHandler = instanceHandlerFactory.getInstanceHandler(infraMapping);
        instanceHandler.handleNewDeployment(deploymentSummary, isRollback, onDemandRollbackInfo);
        createPerpetualTaskForNewDeploymentIfEnabled(infraMapping, deploymentSummary);
        log.info("Handled deployment event for infraMappingId [{}] successfully", infraMappingId);
      } else {
        log.info("Skipping deployment event for infraMappingId [{}]", infraMappingId);
      }
    } catch (Exception ex) {
      // We have to catch all kinds of runtime exceptions, log it and move on, otherwise the queue impl keeps retrying
      // forever in case of exception
      log.warn("Exception while handling deployment event for infraMappingId [{}]", infraMappingId, ex);
    }
  }

  private boolean isSupported(InfrastructureMappingType infrastructureMappingType) {
    return PHYSICAL_DATA_CENTER_SSH != infrastructureMappingType
        && PHYSICAL_DATA_CENTER_WINRM != infrastructureMappingType;
  }

  void createPerpetualTaskForNewDeploymentIfEnabled(
      InfrastructureMapping infrastructureMapping, DeploymentSummary deploymentSummary) {
    if (instanceSyncPerpetualTaskService.isInstanceSyncByPerpetualTaskEnabled(infrastructureMapping)) {
      log.info("Creating Perpetual tasks for new deployment for account: [{}] and infrastructure mapping [{}]",
          infrastructureMapping.getAccountId(), infrastructureMapping.getId());
      instanceSyncPerpetualTaskService.createPerpetualTasksForNewDeployment(infrastructureMapping, deploymentSummary);
    }
  }
}
