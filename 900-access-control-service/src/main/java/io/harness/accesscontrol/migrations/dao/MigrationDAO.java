package io.harness.accesscontrol.migrations.dao;

import io.harness.accesscontrol.migrations.models.Migration;

public interface MigrationDAO {
  Migration save(Migration migration);
}
