package io.harness.migration.utils;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.NGMigration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(DX)
public class NoopMigration implements NGMigration {
  @Override
  public void migrate() {
    log.info("Executing Noop migration");
    // do nothing
  }
}