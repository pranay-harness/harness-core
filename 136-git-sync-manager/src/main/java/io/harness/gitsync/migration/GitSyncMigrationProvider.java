package io.harness.gitsync.migration;

import io.harness.migration.MigrationDetails;
import io.harness.migration.MigrationProvider;
import io.harness.migration.entities.NGSchema;

import java.util.ArrayList;
import java.util.List;

public class GitSyncMigrationProvider implements MigrationProvider {
  @Override
  public String getServiceName() {
    return "yamlGitConfig";
  }

  @Override
  public Class<? extends NGSchema> getSchemaClass() {
    return GitSyncSchema.class;
  }

  @Override
  public List<Class<? extends MigrationDetails>> getMigrationDetailsList() {
    return new ArrayList<Class<? extends MigrationDetails>>() {
      { add(GitSyncMigrationDetails.class); }
    };
  }
}
