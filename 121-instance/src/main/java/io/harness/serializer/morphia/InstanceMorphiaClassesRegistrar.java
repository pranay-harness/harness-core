package io.harness.serializer.morphia;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.DeploymentAccounts;
import io.harness.entities.DeploymentSummary;
import io.harness.entities.InfrastructureMapping;
import io.harness.entities.Instance;
import io.harness.entities.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfo;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

@OwnedBy(DX)
public class InstanceMorphiaClassesRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(InfrastructureMapping.class);
    set.add(Instance.class);
    set.add(InstanceSyncPerpetualTaskInfo.class);
    set.add(DeploymentSummary.class);
    set.add(DeploymentAccounts.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // Nothing to register
  }
}
