/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migration;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.entities.NGSchema;

import java.util.List;

@OwnedBy(DX)
public interface MigrationProvider {
  /**
   * @return a string value denoting the service name to which the migration belongs ex: "pipeline", "cvng" etc.
   */
  String getServiceName();

  /**
   * @return a Entity class that will extend NGSchema class
   */
  Class<? extends NGSchema> getSchemaClass();

  /**
   * @return list of all the Migrations for a service
   */
  List<Class<? extends MigrationDetails>> getMigrationDetailsList();
}
