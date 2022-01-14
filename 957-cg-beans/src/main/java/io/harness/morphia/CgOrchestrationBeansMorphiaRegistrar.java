package io.harness.morphia;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SweepingOutput;
import io.harness.beans.terraform.TerraformPlanParam;

import software.wings.beans.Log;
import software.wings.beans.TerraGroupProvisioners;
import software.wings.beans.entityinterface.ApplicationAccess;
import software.wings.beans.entityinterface.KeywordsAware;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.ngmigration.NGMigrationEntity;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.intfc.customdeployment.CustomDeploymentTypeAware;
import software.wings.verification.CVActivityLog;

import java.util.Set;

@OwnedBy(CDP)
public class CgOrchestrationBeansMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(CustomDeploymentTypeAware.class);
    set.add(ApplicationAccess.class);
    set.add(KeywordsAware.class);
    set.add(TerraGroupProvisioners.class);
    set.add(SweepingOutput.class);
    set.add(NGMigrationEntity.class);
    set.add(ThirdPartyApiCallLog.class);
    set.add(Log.class);
    set.add(CVActivityLog.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    w.put("metrics.TimeSeriesMetricDefinition", TimeSeriesMetricDefinition.class);
    w.put("api.TerraformPlanParam", TerraformPlanParam.class);
  }
}
