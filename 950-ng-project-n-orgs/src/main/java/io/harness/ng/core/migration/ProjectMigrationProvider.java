/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.migration;

import io.harness.migration.MigrationDetails;
import io.harness.migration.MigrationProvider;
import io.harness.migration.entities.NGSchema;
import io.harness.ng.core.migration.schema.ProjectSchema;

import java.util.ArrayList;
import java.util.List;

public class ProjectMigrationProvider implements MigrationProvider {
  @Override
  public String getServiceName() {
    return "projects";
  }

  @Override
  public Class<? extends NGSchema> getSchemaClass() {
    return ProjectSchema.class;
  }

  @Override
  public List<Class<? extends MigrationDetails>> getMigrationDetailsList() {
    return new ArrayList<Class<? extends MigrationDetails>>() {
      { add(ProjectMigrationDetails.class); }
      { add(ProjectTimeScaleMigrationDetails.class); }
    };
  }
}
