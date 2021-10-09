package io.harness.accesscontrol.commons.migration;

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
public class AccessControlMigrationProvider implements MigrationProvider {
  @Override
  public String getServiceName() {
    return "accesscontrol";
  }

  @Override
  public Class<? extends NGSchema> getSchemaClass() {
    return AccessControlMigrationSchema.class;
  }

  @Override
  public List<Class<? extends MigrationDetails>> getMigrationDetailsList() {
    return new ArrayList<Class<? extends MigrationDetails>>() {
      { add(AccessControlMongoBackgroundMigrationDetails.class); }
    };
  }
}