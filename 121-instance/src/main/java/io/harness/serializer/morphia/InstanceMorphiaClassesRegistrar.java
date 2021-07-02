package io.harness.serializer.morphia;

import io.harness.entities.DeploymentSummary;
import io.harness.entities.InstanceSyncPerpetualTaskInfo;
import io.harness.entities.SyncStatus;
import io.harness.entities.infrastructureMapping.DirectKubernetesInfrastructureMapping;
import io.harness.entities.infrastructureMapping.InfrastructureMapping;
import io.harness.entities.instance.Instance;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class InstanceMorphiaClassesRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(DirectKubernetesInfrastructureMapping.class);
    set.add(InfrastructureMapping.class);
    set.add(Instance.class);
    set.add(InstanceSyncPerpetualTaskInfo.class);
    set.add(DeploymentSummary.class);
    set.add(SyncStatus.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // Nothing to register
  }
}
