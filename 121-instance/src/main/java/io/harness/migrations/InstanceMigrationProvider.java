/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.MigrationDetails;
import io.harness.migration.MigrationProvider;
import io.harness.migration.entities.NGSchema;

import java.util.ArrayList;
import java.util.List;

@OwnedBy(DX)
public class InstanceMigrationProvider implements MigrationProvider {
  @Override
  public String getServiceName() {
    return "instance";
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
