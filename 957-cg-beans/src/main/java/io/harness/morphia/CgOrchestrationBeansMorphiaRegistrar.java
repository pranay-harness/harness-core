package io.harness.morphia;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SweepingOutput;

import software.wings.beans.TerraGroupProvisioners;
import software.wings.beans.entityinterface.ApplicationAccess;
import software.wings.ngmigration.NGMigrationEntity;
import software.wings.service.intfc.customdeployment.CustomDeploymentTypeAware;

import java.util.Set;

@OwnedBy(CDP)
public class CgOrchestrationBeansMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(CustomDeploymentTypeAware.class);
    set.add(ApplicationAccess.class);
    set.add(TerraGroupProvisioners.class);
    set.add(SweepingOutput.class);
    set.add(NGMigrationEntity.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {}
}
