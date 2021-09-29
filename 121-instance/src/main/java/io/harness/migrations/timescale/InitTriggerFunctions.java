package io.harness.migrations.timescale;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.timescale.NGAbstractTimeScaleMigration;

@OwnedBy(HarnessTeam.PL)
public class InitTriggerFunctions extends NGAbstractTimeScaleMigration {
  @Override
  public String getFileName() {
    return "timescale/trigger_functions.sql";
  }
}