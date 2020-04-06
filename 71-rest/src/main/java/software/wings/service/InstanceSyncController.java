package software.wings.service;

import static io.harness.exception.WingsException.EVERYBODY;
import static io.harness.validation.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.DeploymentSummary;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.service.impl.instance.PcfInstanceHandler;
import software.wings.utils.Utils;

import java.util.List;

@Slf4j
@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InstanceSyncController {
  @Inject PCFInstanceSyncPerpetualTaskController pcfInstanceSyncPerpetualTaskController;
  @Inject DefaultInstanceSyncPerpetualTaskController defaultInstanceSyncPerpetualTaskController;

  public boolean canUpdateDb(InstanceSyncFlow instanceSyncFlow, InfrastructureMapping infrastructureMapping,
      Class<? extends PcfInstanceHandler> callerClass) {
    InstanceSyncPerpetualTaskController instanceController =
        getInstanceSyncPerpetualTaskController(infrastructureMapping);
    return instanceController.canUpdateDb(instanceSyncFlow, infrastructureMapping.getAccountId(), callerClass);
  }

  private InstanceSyncPerpetualTaskController getInstanceSyncPerpetualTaskController(
      InfrastructureMapping infrastructureMapping) {
    String deploymentType = infrastructureMapping.getInfraMappingType();
    return getInstanceController(deploymentType);
  }

  public boolean enablePerpetualTaskForAccount(String accountId, String infrastructureMappingType) {
    InstanceSyncPerpetualTaskController instanceController = getInstanceController(infrastructureMappingType);
    return instanceController.enablePerpetualTaskForAccount(accountId);
  }

  public boolean shouldSkipIteratorInstanceSync(InfrastructureMapping infrastructureMapping) {
    InstanceSyncPerpetualTaskController instanceController =
        getInstanceSyncPerpetualTaskController(infrastructureMapping);
    return instanceController.shouldSkipIteratorInstanceSync(infrastructureMapping);
  }

  public boolean createPerpetualTaskForNewDeployment(
      InfrastructureMappingType infrastructureMappingType, List<DeploymentSummary> deploymentSummaries) {
    InstanceSyncPerpetualTaskController instanceController = getInstanceController(infrastructureMappingType.getType());
    return instanceController.createPerpetualTaskForNewDeployment(infrastructureMappingType, deploymentSummaries);
  }

  private InstanceSyncPerpetualTaskController getInstanceController(String infraMapping) {
    InfrastructureMappingType infraMappingType = Utils.getEnumFromString(InfrastructureMappingType.class, infraMapping);
    notNullCheck("Infra mapping type.", infraMappingType, EVERYBODY);
    switch (infraMappingType) {
      case PCF_PCF:
        return pcfInstanceSyncPerpetualTaskController;
      default:
        return defaultInstanceSyncPerpetualTaskController;
    }
  }

  public enum InstanceSyncFlow {
    NEW_DEPLOYMENT,
    PERPETUAL_TASK,
    ITERATOR_INSTANCE_SYNC;
  }
}
