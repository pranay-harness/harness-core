package io.harness.accesscontrol.migrations.services;

import io.harness.accesscontrol.migrations.dao.MigrationDAO;
import io.harness.accesscontrol.migrations.models.Migration;

import com.google.inject.Inject;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class MigrationServiceImpl implements MigrationService {
  private final MigrationDAO migrationDAO;

  @Override
  public Migration save(Migration migration) {
    return migrationDAO.save(migration);
  }
}
