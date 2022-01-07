/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
