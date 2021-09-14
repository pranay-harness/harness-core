/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.version;

import com.google.inject.AbstractModule;

public class VersionModule extends AbstractModule {
  private static VersionModule instance;

  private VersionModule() {}

  public static VersionModule getInstance() {
    if (instance == null) {
      instance = new VersionModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(VersionInfoManager.class).toInstance(new VersionInfoManager());
  }
}
