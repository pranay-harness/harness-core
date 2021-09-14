/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migration.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.beans.NGMigrationConfiguration;

@OwnedBy(DX)
public interface NGMigrationService {
  void runMigrations(NGMigrationConfiguration configuration);
}
