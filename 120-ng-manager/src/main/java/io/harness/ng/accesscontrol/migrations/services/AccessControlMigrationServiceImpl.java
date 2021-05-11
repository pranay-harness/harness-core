package io.harness.ng.accesscontrol.migrations.services;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dao.AccessControlMigrationDAO;
import io.harness.models.AccessControlMigration;
import io.harness.services.AccessControlMigrationService;

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
