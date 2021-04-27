package io.harness.instancesync.service.instancesync;

import static io.harness.instancesync.entity.constants.InstanceSyncConstants.HARNESS_ACCOUNT_ID;
import static io.harness.validation.Validator.notNullCheck;

import static software.wings.beans.InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH;
import static software.wings.beans.InfrastructureMappingType.PHYSICAL_DATA_CENTER_WINRM;

import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.exception.WingsException;
import io.harness.instancesync.entity.DeploymentEvent;
import io.harness.instancesync.entity.DeploymentSummary;
import io.harness.instancesync.entity.InstanceSyncFlowType;
import io.harness.instancesync.entity.deploymentinfo.OnDemandRollbackInfo;
import io.harness.instancesync.entity.infrastructureMapping.InfrastructureMapping;
import io.harness.instancesync.service.InstanceHandler;
import io.harness.instancesync.service.instance.InstanceService;
import io.harness.instancesync.service.instancehandlerfactory.InstanceHandlerFactoryService;
import io.harness.instancesync.service.instancesyncperpetualtask.InstanceSyncPerpetualTaskService;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;

import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.infrastructure.instance.ManualSyncJob;
import software.wings.service.impl.instance.Status;
import software.wings.utils.Utils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InstanceSyncServiceImpl implements InstanceSyncService {
  @Inject private PersistentLocker persistentLocker;
  @Inject private InstanceHandlerFactoryService instanceHandlerFactory;
  @Inject private InstanceSyncPerpetualTaskService instanceSyncPerpetualTaskService;
  @Inject private InstanceService instanceService;
  @Inject private PerpetualTaskService perpetualTaskService;

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

  public void processInstanceSyncResponseFromPerpetualTask(String perpetualTaskId, DelegateResponseData response) {
    PerpetualTaskRecord perpetualTaskRecord = perpetualTaskService.getTaskRecord(perpetualTaskId);
    Map<String, String> clientParams = perpetualTaskRecord.getClientContext().getClientParams();

    String accountId = clientParams.get(HARNESS_ACCOUNT_ID);
    // TODO create method to fetch field by field from client params and create inframapping details object
    InfrastructureMapping infrastructureMapping = fetchInframappingDetailsFromClientContext(clientParams);

    // TODO check how to handle below case in NG
    //    if (infrastructureMapping == null) {
    //      log.info(
    //              "Handling Instance sync response. Infrastructure Mapping does not exist for Id : [{}]. Deleting
    //              Perpetual Tasks ", infrastructureMappingId);
    //      instanceSyncPerpetualTaskService.deletePerpetualTasks(accountId, infrastructureMappingId);
    //      return;
    //    }

    log.info("Handling Instance sync response. Infrastructure Mapping : [{}], Perpetual Task Id : [{}]",
        infrastructureMapping.getId(), perpetualTaskRecord.getUuid());

    try (AcquiredLock<?> lock = persistentLocker.tryToAcquireLock(software.wings.beans.InfrastructureMapping.class,
             infrastructureMapping.getId(), Duration.ofSeconds(180))) {
      if (lock == null) {
        log.warn("Couldn't acquire lock on Infrastructure Mapping. Infrastructure Mapping : [{}]",
            infrastructureMapping.getId());
        return;
      }
      handleInstanceSyncResponseFromPerpetualTask(infrastructureMapping, perpetualTaskRecord, response);
    }

    log.info("Handled Instance sync response successfully. Infrastructure Mapping : [{}], Perpetual Task Id : [{}]",
        infrastructureMapping.getId(), perpetualTaskRecord.getUuid());
  }

  // TODO check how to perform this, what should be the inputs
  public String manualSync(String appId, String infraMappingId) {
    String syncJobId = UUIDGenerator.generateUuid();
    InfrastructureMapping infrastructureMapping = infraMappingService.get(appId, infraMappingId);
    String accountId = infrastructureMapping.getAccountId();
    instanceService.saveManualSyncJob(
        ManualSyncJob.builder().uuid(syncJobId).accountId(accountId).appId(appId).build());
    executorService.submit(() -> {
      try {
        syncNow(appId, infrastructureMapping, MANUAL);
      } finally {
        instanceService.deleteManualSyncJob(appId, syncJobId);
      }
    });
    return syncJobId;
  }

  public void syncNow(String appId, InfrastructureMapping infraMapping, InstanceSyncFlowType instanceSyncFlowType) {
    if (infraMapping == null) {
      return;
    }

    String infraMappingId = infraMapping.getId();
    try (AcquiredLock lock =
             persistentLocker.tryToAcquireLock(InfrastructureMapping.class, infraMappingId, Duration.ofSeconds(180))) {
      if (lock == null) {
        log.warn("Couldn't acquire infra lock for infraMapping of appId [{}]", appId);
        return;
      }

      try {
        InstanceHandler instanceHandler = instanceHandlerFactory.getInstanceHandler(infraMapping);
        if (instanceHandler == null) {
          log.warn("Instance handler null for infraMapping of appId [{}]", appId);
          return;
        }
        log.info("Instance sync started for infraMapping");
        instanceHandler.syncInstances(appId, infraMappingId, instanceSyncFlowType);
        // TODO check if we require display name here
        instanceService.updateSyncSuccess(appId, infraMapping.getServiceId(), infraMapping.getEnvId(), infraMappingId,
            infraMapping.getDisplayName(), System.currentTimeMillis());
        log.info("Instance sync completed for infraMapping");
      } catch (Exception ex) {
        log.warn("Instance sync failed for infraMapping", ex);
        String errorMsg = getErrorMsg(ex);

        // TODO check if we require display name here
        instanceService.handleSyncFailure(appId, infraMapping.getServiceId(), infraMapping.getEnvId(), infraMappingId,
            infraMapping.getDisplayName(), System.currentTimeMillis(), errorMsg);
      }
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

  private InfrastructureMapping fetchInframappingDetailsFromClientContext(Map<String, String> clientParams) {
    return null;
  }

  private void handleInstanceSyncResponseFromPerpetualTask(InfrastructureMapping infrastructureMapping,
      PerpetualTaskRecord perpetualTaskRecord, DelegateResponseData response) {
    InstanceHandler instanceHandler = getInstanceHandler(infrastructureMapping);
    if (instanceHandler == null
        || !instanceSyncPerpetualTaskService.isInstanceSyncByPerpetualTaskEnabled(infrastructureMapping)) {
      return;
    }

    try {
      instanceHandler.processInstanceSyncResponseFromPerpetualTask(infrastructureMapping, response);
    } catch (Exception ex) {
      log.error("Error handling Instance sync response. Infrastructure Mapping : [{}], Perpetual Task Id : [{}]",
          infrastructureMapping.getId(), perpetualTaskRecord.getUuid());
      String errorMsg = getErrorMsg(ex);

      boolean stopSync = instanceService.handleSyncFailure(infrastructureMapping.getAppId(),
          infrastructureMapping.getServiceId(), infrastructureMapping.getEnvId(), infrastructureMapping.getUuid(),
          infrastructureMapping.getDisplayName(), System.currentTimeMillis(), errorMsg);

      if (stopSync) {
        log.info("Sync Failure. Deleting Perpetual Tasks. Infrastructure Mapping : [{}], Perpetual Task Id : [{}]",
            infrastructureMapping.getId(), perpetualTaskRecord.getUuid());
        instanceSyncPerpetualTaskService.deletePerpetualTasks(infrastructureMapping);
      }

      throw ex;
    } finally {
      Status status = handler.getStatus(infrastructureMapping, response);
      if (status.isSuccess()) {
        instanceService.updateSyncSuccess(infrastructureMapping.getAppId(), infrastructureMapping.getServiceId(),
            infrastructureMapping.getEnvId(), infrastructureMapping.getUuid(), infrastructureMapping.getDisplayName(),
            System.currentTimeMillis());
      }
      if (!status.isRetryable()) {
        log.info("Task Not Retryable. Deleting Perpetual Task. Infrastructure Mapping : [{}], Perpetual Task Id : [{}]",
            infrastructureMapping.getUuid(), perpetualTaskRecord.getUuid());
        instanceSyncPerpetualTaskService.deletePerpetualTask(
            infrastructureMapping.getAccountId(), infrastructureMapping.getId(), perpetualTaskRecord.getUuid());
      }
      if (!status.isSuccess()) {
        log.info("Sync Failure. Reset Perpetual Task. Infrastructure Mapping : [{}], Perpetual Task Id : [{}]",
            infrastructureMapping.getUuid(), perpetualTaskRecord.getUuid());
        instanceSyncPerpetualTaskService.resetPerpetualTask(
            infrastructureMapping.getAccountId(), perpetualTaskRecord.getUuid());
      }
    }
  }

  private InstanceHandler getInstanceHandler(InfrastructureMapping infrastructureMapping) {
    try {
      return instanceHandlerFactory.getInstanceHandler(infrastructureMapping);
    } catch (Exception ex) {
      return null;
    }
  }

  private String getErrorMsg(Exception ex) {
    String errorMsg;
    if (ex instanceof WingsException) {
      errorMsg = ex.getMessage();
    } else {
      errorMsg = ex.getMessage() != null ? ex.getMessage() : "Unknown error";
    }
    return errorMsg;
  }
}
