/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migration;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.beans.NGMigrationConfiguration;
import io.harness.migration.service.NGMigrationService;

import com.google.inject.Injector;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@OwnedBy(DX)
public class NGMigrationSdkInitHelper {
  public static void initialize(Injector injector, NGMigrationConfiguration config) {
    injector.getInstance(NGMigrationService.class).runMigrations(config);
  }
}
