/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
