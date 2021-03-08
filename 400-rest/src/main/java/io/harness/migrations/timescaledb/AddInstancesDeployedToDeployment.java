package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
@TargetModule(Module._390_DB_MIGRATION)
public class AddInstancesDeployedToDeployment extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_instances_deployed_to_deployment.sql";
  }
}
