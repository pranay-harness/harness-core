package io.harness.licensing.migrations;

import io.harness.licensing.migrations.licenses.ModuleLicenseMigrationDetails;
import io.harness.migration.MigrationDetails;
import io.harness.migration.MigrationProvider;
import io.harness.migration.entities.NGSchema;

import com.google.common.collect.Lists;
import java.util.List;

public class LicenseManagerMigrationProvider implements MigrationProvider {
  @Override
  public String getServiceName() {
    return "licensemanager";
  }

  @Override
  public Class<? extends NGSchema> getSchemaClass() {
    return LicenseManagerSchema.class;
  }

  @Override
  public List<Class<? extends MigrationDetails>> getMigrationDetailsList() {
    return Lists.newArrayList(ModuleLicenseMigrationDetails.class);
  }
}
