/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness;

import com.google.inject.AbstractModule;

public class PmsCommonsModule extends AbstractModule {
  private static PmsCommonsModule instance;

  public static PmsCommonsModule getInstance() {
    if (instance == null) {
      instance = new PmsCommonsModule();
    }
    return instance;
  }

  @Override
  protected void configure() {}
}
