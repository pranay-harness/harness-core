package io.harness.pms.migration;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.NGMigration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class NoopPipelineCoreMigration implements NGMigration {
  @Override
  public void migrate() {
    log.info("Executing Noop migration");
    // do nothing
  }
}
