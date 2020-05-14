package software.wings.service.impl.instance;

import io.harness.delegate.beans.ResponseData;
import software.wings.beans.FeatureName;
import software.wings.beans.InfrastructureMapping;
import software.wings.service.InstanceSyncPerpetualTaskCreator;

public interface InstanceSyncByPerpetualTaskHandler {
  FeatureName getFeatureFlagToEnablePerpetualTaskForInstanceSync();

  InstanceSyncPerpetualTaskCreator getInstanceSyncPerpetualTaskCreator();

  void processInstanceSyncResponseFromPerpetualTask(InfrastructureMapping infrastructureMapping, ResponseData response);

  Status getStatus(InfrastructureMapping infrastructureMapping, ResponseData response);
}
