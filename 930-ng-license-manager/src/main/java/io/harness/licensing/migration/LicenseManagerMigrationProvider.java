package io.harness.licensing.migration;

import io.harness.licensing.migration.schema.LicenseManagerSchema;
import io.harness.migration.MigrationDetails;
import io.harness.migration.MigrationProvider;
import io.harness.migration.entities.NGSchema;

import java.util.List;

public class LicenseManagerMigrationProvider implements MigrationProvider {
  @Override
  public String getServiceName() {
    return "nglicensemanager";
  }

  @Override
  public Class<? extends NGSchema> getSchemaClass() {
    return LicenseManagerSchema.class;
  }

  @Override
  public List<Class<? extends MigrationDetails>> getMigrationDetailsList() {
    return null;
  }
}
