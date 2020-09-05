package software.wings.service.impl.instance;

import io.harness.delegate.beans.DelegateResponseData;
import software.wings.beans.FeatureName;
import software.wings.beans.InfrastructureMapping;
import software.wings.service.InstanceSyncPerpetualTaskCreator;

public interface InstanceSyncByPerpetualTaskHandler {
  FeatureName getFeatureFlagToEnablePerpetualTaskForInstanceSync();

  InstanceSyncPerpetualTaskCreator getInstanceSyncPerpetualTaskCreator();

  void processInstanceSyncResponseFromPerpetualTask(
      InfrastructureMapping infrastructureMapping, DelegateResponseData response);

  Status getStatus(InfrastructureMapping infrastructureMapping, DelegateResponseData response);
}
