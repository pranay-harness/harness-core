package io.harness.migrations.timescale;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.timescale.NGAbstractTimeScaleMigration;

@OwnedBy(DX)
public class CreateInstanceStatsTable extends NGAbstractTimeScaleMigration {
  @Override
  public String getFileName() {
    return "timescale/create_instance_stats_table.sql";
  }
}
