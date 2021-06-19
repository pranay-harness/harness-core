package io.harness.pms.migration;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.MigrationDetails;
import io.harness.migration.MigrationProvider;
import io.harness.migration.entities.NGSchema;

import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class PipelineCoreMigrationProvider implements MigrationProvider {
  @Override
  public String getServiceName() {
    return ModuleType.PMS.getDisplayName();
  }

  @Override
  public Class<? extends NGSchema> getSchemaClass() {
    return PipelineCoreSchema.class;
  }

  @Override
  public List<Class<? extends MigrationDetails>> getMigrationDetailsList() {
    return new ArrayList<Class<? extends MigrationDetails>>() {
      { add(PipelineCoreMigrationDetails.class); }
    };
  }
}
