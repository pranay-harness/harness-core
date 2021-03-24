package io.harness.ng.accesscontrol.migrations.dao;

import io.harness.ng.accesscontrol.migrations.models.Migration;
import io.harness.ng.accesscontrol.migrations.repositories.MigrationRepository;

import com.google.inject.Inject;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class MigrationDAOImpl implements MigrationDAO {
  private final MigrationRepository migrationRepository;

  @Override
  public Migration save(Migration migration) {
    return migrationRepository.save(migration);
  }
}
