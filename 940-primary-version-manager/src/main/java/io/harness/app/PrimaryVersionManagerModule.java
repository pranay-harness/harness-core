/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.app;

import io.harness.controller.PrimaryVersionController;
import io.harness.queue.QueueController;
import io.harness.version.VersionModule;

import com.google.inject.AbstractModule;

public class PrimaryVersionManagerModule extends AbstractModule {
  private static volatile PrimaryVersionManagerModule instance;

  private PrimaryVersionManagerModule() {}

  public static PrimaryVersionManagerModule getInstance() {
    if (instance == null) {
      instance = new PrimaryVersionManagerModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    install(VersionModule.getInstance());
    bind(QueueController.class).to(PrimaryVersionController.class);
  }
}
