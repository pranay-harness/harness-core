package io.harness.ng.accesscontrol.migrations.dao;

import io.harness.ng.accesscontrol.migrations.models.Migration;

public interface MigrationDAO {
  Migration save(Migration migration);
}
