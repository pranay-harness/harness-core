package io.harness.ng.accesscontrol.migrations.services;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.ng.accesscontrol.migrations.models.AccessControlMigration;

@OwnedBy(HarnessTeam.PL)
public interface AccessControlMigrationService {
  AccessControlMigration save(AccessControlMigration accessControlMigration);

  boolean isMigrated(Scope scope);

  void migrate(String accountId);
}
