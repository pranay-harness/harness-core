package io.harness.resourcegroup.migrations;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.MigrationDetails;
import io.harness.migration.MigrationProvider;
import io.harness.migration.entities.NGSchema;

import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@OwnedBy(PL)
@Singleton
public class ResourceGroupMigrationProvider implements MigrationProvider {
  @Override
  public String getServiceName() {
    return "resourcegroup";
  }

  @Override
  public Class<? extends NGSchema> getSchemaClass() {
    return ResourceGroupMigrationSchema.class;
  }

  @Override
  public List<Class<? extends MigrationDetails>> getMigrationDetailsList() {
    return new ArrayList<Class<? extends MigrationDetails>>() {
      { add(ResourceGroupBackgroundMigrationDetails.class); }
    };
  }
}
