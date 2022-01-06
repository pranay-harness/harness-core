/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations;

import software.wings.service.impl.MigrationServiceImpl;
import software.wings.service.intfc.MigrationService;

import com.google.inject.AbstractModule;

public class MigrationModule extends AbstractModule {
  private static MigrationModule instance;

  private MigrationModule() {}

  public static MigrationModule getInstance() {
    if (instance == null) {
      instance = new MigrationModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(MigrationService.class).to(MigrationServiceImpl.class);
  }
}
