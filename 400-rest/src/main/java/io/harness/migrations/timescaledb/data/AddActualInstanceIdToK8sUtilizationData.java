package io.harness.migrations.timescaledb.data;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.migrations.timescaledb.AbstractTimeScaleDBMigration;

@OwnedBy(HarnessTeam.CE)
@TargetModule(HarnessModule._390_DB_MIGRATION)
public class AddActualInstanceIdToK8sUtilizationData extends AbstractTimeScaleDBMigration {
  @Override
  public String getFileName() {
    return "timescaledb/add_actualinstanceid_to_k8s_util_table.sql";
  }
}
