package io.harness.ng.core.migration.tasks;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.timescale.NGAbstractTimeScaleMigration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class CreateTagsTimeScaleTable extends NGAbstractTimeScaleMigration {
  public String getFileName() {
    return "timescale/create_tags_table.sql";
  }
}