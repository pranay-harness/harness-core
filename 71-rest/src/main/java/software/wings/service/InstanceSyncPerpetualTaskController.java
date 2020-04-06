package software.wings.service;

import static software.wings.service.InstanceSyncController.InstanceSyncFlow.ITERATOR_INSTANCE_SYNC;
import static software.wings.service.InstanceSyncController.InstanceSyncFlow.NEW_DEPLOYMENT;

import software.wings.api.DeploymentSummary;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.service.impl.instance.InstanceHandler;

import java.util.List;

public interface InstanceSyncPerpetualTaskController {
  default boolean enablePerpetualTaskForAccount(String accountId) {
    return false;
  }

  default boolean createPerpetualTaskForNewDeployment(
      InfrastructureMappingType infrastructureMappingType, List<DeploymentSummary> deploymentSummaries) {
    return false;
  }

  default boolean canUpdateDb(InstanceSyncController.InstanceSyncFlow instanceSyncFlow, String accountId,
      Class<? extends InstanceHandler> callerClass) {
    return instanceSyncFlow.equals(NEW_DEPLOYMENT) || instanceSyncFlow.equals(ITERATOR_INSTANCE_SYNC);
  }

  default boolean shouldSkipIteratorInstanceSync(InfrastructureMapping infrastructureMapping) {
    return false;
  }
}
