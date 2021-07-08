package io.harness.migrations;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.instance.Instance;
import io.harness.migration.MigrationDetails;
import io.harness.migration.MigrationProvider;
import io.harness.migration.entities.NGSchema;

import java.util.ArrayList;
import java.util.List;

@OwnedBy(DX)
public class InstanceMigrationProvider implements MigrationProvider {
  @Override
  public String getServiceName() {
    return Instance.class.getCanonicalName();
  }

  @Override
  public Class<? extends NGSchema> getSchemaClass() {
    return NGInstanceSchema.class;
  }

  @Override
  public List<Class<? extends MigrationDetails>> getMigrationDetailsList() {
    return new ArrayList<Class<? extends MigrationDetails>>() {
      { add(InstanceStatsTimeScaleMigrationDetails.class); }
    };
  }
}
