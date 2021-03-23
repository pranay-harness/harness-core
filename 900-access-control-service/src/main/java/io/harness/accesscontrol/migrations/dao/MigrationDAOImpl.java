package io.harness.accesscontrol.migrations.dao;

import io.harness.accesscontrol.migrations.models.Migration;
import io.harness.accesscontrol.migrations.repositories.MigrationRepository;

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
