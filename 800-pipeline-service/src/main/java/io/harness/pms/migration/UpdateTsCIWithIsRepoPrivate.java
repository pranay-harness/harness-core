package io.harness.pms.migration;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.timescale.NGAbstractTimeScaleMigration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CI)
public class UpdateTsCIWithIsRepoPrivate extends NGAbstractTimeScaleMigration {
  public String getFileName() {
    return "timescale/modify_pipeline_execution_summary_ci_repo_private.sql";
  }
}
