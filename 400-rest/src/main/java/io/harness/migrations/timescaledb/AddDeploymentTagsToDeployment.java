package io.harness.migrations.timescaledb;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

@TargetModule(HarnessModule._390_DB_MIGRATION)
public class AddDeploymentTagsToDeployment extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_deployment_tags_to_deployment.sql";
  }
}
