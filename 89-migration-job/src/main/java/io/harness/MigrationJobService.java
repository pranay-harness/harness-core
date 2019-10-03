package io.harness;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.migration.MongoIndexMigrationService;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class MigrationJobService {
  @Inject MongoIndexMigrationService mongoIndexMigrationService;
  public void migrate() {
    // TODO: implement this
  }
}
