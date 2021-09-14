/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.testing;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

public class ComponentTestsModule extends AbstractModule {
  @Override
  protected void configure() {
    MapBinder.newMapBinder(binder(), String.class, TestExecution.class);
  }
}
