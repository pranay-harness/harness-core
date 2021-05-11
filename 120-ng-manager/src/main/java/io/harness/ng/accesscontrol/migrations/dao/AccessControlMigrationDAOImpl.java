package io.harness.ng.accesscontrol.migrations.dao;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dao.AccessControlMigrationDAO;
import io.harness.models.AccessControlMigration;
import io.harness.repositories.AccessControlMigrationRepository;

import com.google.inject.Inject;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.PL)
public class AccessControlMigrationDAOImpl implements AccessControlMigrationDAO {
  private final AccessControlMigrationRepository migrationRepository;

  @Override
  public AccessControlMigration save(AccessControlMigration accessControlMigration) {
    return migrationRepository.save(accessControlMigration);
  }
}
