/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migration;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.service.NGMigrationService;
import io.harness.migration.service.impl.NGMigrationServiceImpl;

import com.google.inject.AbstractModule;
import java.util.concurrent.atomic.AtomicReference;

@OwnedBy(DX)
public class NGMigrationSdkModule extends AbstractModule {
  private static final AtomicReference<NGMigrationSdkModule> instanceRef = new AtomicReference();

  public NGMigrationSdkModule() {}

  @Override
  protected void configure() {
    bind(NGMigrationService.class).to(NGMigrationServiceImpl.class);
  }

  public static NGMigrationSdkModule getInstance() {
    if (instanceRef.get() == null) {
      instanceRef.compareAndSet(null, new NGMigrationSdkModule());
    }
    return instanceRef.get();
  }
}
