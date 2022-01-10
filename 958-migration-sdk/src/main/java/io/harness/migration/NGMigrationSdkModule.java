/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
