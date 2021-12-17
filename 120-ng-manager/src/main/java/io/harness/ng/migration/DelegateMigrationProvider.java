package io.harness.ng.migration;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.MigrationDetails;
import io.harness.migration.MigrationProvider;
import io.harness.migration.entities.NGSchema;

import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@OwnedBy(DEL)
@Singleton
public class DelegateMigrationProvider implements MigrationProvider {
  @Override
  public String getServiceName() {
    return "delegate";
  }

  @Override
  public Class<? extends NGSchema> getSchemaClass() {
    return DelegateSchema.class;
  }

  @Override
  public List<Class<? extends MigrationDetails>> getMigrationDetailsList() {
    return new ArrayList<Class<? extends MigrationDetails>>() {
      { add(DelegateMigrationDetails.class); }
    };
  }
}
