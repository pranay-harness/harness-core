package io.harness.ccm.migration;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.NGMigration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CE)
public class NoopCENGTimescaleMigration implements NGMigration {
  @Override
  public void migrate() {
    log.info("Executing Noop TimescaleDB migration");
    // do nothing
  }
}