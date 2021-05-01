package io.harness.ng.migration;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.MigrationDetails;
import io.harness.migration.MigrationProvider;
import io.harness.migration.entities.NGSchema;

import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@OwnedBy(DX)
@Singleton
public class NGCoreMigrationProvider implements MigrationProvider {
  @Override
  public String getServiceName() {
    return "ngmanager";
  }

  @Override
  public Class<? extends NGSchema> getSchemaClass() {
    return NGCoreSchema.class;
  }

  @Override
  public List<Class<? extends MigrationDetails>> getMigrationDetailsList() {
    return new ArrayList<Class<? extends MigrationDetails>>() {
      {
        add(NGCoreMongoMigrationBGDetails.class);
        add(NGCoreMigrationDetails.class);
      }
    };
  }
}