package io.harness.ng.accesscontrol.migrations.dao;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.accesscontrol.migrations.models.AccessControlMigration;

@OwnedBy(HarnessTeam.PL)
public interface AccessControlMigrationDAO {
  AccessControlMigration save(AccessControlMigration accessControlMigration);

  boolean alreadyMigrated(String accountIdentifier, String orgIdentifier, String projectIdentifier);
}
