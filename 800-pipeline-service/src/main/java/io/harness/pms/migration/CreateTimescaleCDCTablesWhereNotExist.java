package io.harness.pms.migration;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.timescale.NGAbstractTimeScaleMigration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class CreateTimescaleCDCTablesWhereNotExist extends NGAbstractTimeScaleMigration {
  @Override
  public String getFileName() {
    return "timescale/create_change_data_capture_tables_timescale.sql";
  }
}