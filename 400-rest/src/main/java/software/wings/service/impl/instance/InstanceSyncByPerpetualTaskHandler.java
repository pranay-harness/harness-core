package software.wings.service.impl.instance;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.FeatureName;
import io.harness.delegate.beans.DelegateResponseData;

import software.wings.beans.InfrastructureMapping;
import software.wings.service.InstanceSyncPerpetualTaskCreator;

@OwnedBy(PL)
@TargetModule(HarnessModule._441_CG_INSTANCE_SYNC)
public interface InstanceSyncByPerpetualTaskHandler {
  FeatureName getFeatureFlagToEnablePerpetualTaskForInstanceSync();

  InstanceSyncPerpetualTaskCreator getInstanceSyncPerpetualTaskCreator();

  void processInstanceSyncResponseFromPerpetualTask(
      InfrastructureMapping infrastructureMapping, DelegateResponseData response);

  Status getStatus(InfrastructureMapping infrastructureMapping, DelegateResponseData response);
}
