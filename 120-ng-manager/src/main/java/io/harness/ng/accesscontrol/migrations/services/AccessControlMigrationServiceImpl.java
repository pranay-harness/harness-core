package io.harness.ng.accesscontrol.migrations.services;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.accesscontrol.migrations.dao.AccessControlMigrationDAO;
import io.harness.ng.accesscontrol.migrations.models.AccessControlMigration;

import com.google.inject.Inject;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.PL)
public class AccessControlMigrationServiceImpl implements AccessControlMigrationService {
  private final AccessControlMigrationDAO accessControlMigrationDAO;

  @Override
  public AccessControlMigration save(AccessControlMigration accessControlMigration) {
    return accessControlMigrationDAO.save(accessControlMigration);
  }
}
