package io.harness.ng.core.migration;

import io.harness.migration.MigrationDetails;
import io.harness.migration.MigrationProvider;
import io.harness.migration.entities.NGSchema;
import io.harness.ng.core.migration.schema.NGBeansSchema;

import java.util.ArrayList;
import java.util.List;

public class NGBeanMigrationProvider implements MigrationProvider {
  @Override
  public String getServiceName() {
    return "NGBeans";
  }

  @Override
  public Class<? extends NGSchema> getSchemaClass() {
    return NGBeansSchema.class;
  }

  @Override
  public List<Class<? extends MigrationDetails>> getMigrationDetailsList() {
    return new ArrayList<Class<? extends MigrationDetails>>() {
      { add(NGBeansTimeScaleMigrationDetails.class); }
    };
  }
}
