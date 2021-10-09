package io.harness.ng.migration;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.NGMigration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(DX)
public class NoopNGCoreMigration implements NGMigration {
  @Override
  public void migrate() {
    log.info("Executing Noop migration");
    // do nothing
  }
}