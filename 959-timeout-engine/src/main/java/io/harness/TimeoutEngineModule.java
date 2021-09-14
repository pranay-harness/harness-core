/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness;

import io.harness.registrars.TimeoutEngineTimeoutRegistrar;
import io.harness.registries.TimeoutEngineRegistryModule;
import io.harness.registries.registrar.TimeoutRegistrar;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

public class TimeoutEngineModule extends AbstractModule {
  private static TimeoutEngineModule instance;

  public static TimeoutEngineModule getInstance() {
    if (instance == null) {
      instance = new TimeoutEngineModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    install(TimeoutEngineRegistryModule.getInstance());

    MapBinder<String, TimeoutRegistrar> timeoutRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, TimeoutRegistrar.class);
    timeoutRegistrarMapBinder.addBinding(TimeoutEngineTimeoutRegistrar.class.getName())
        .to(TimeoutEngineTimeoutRegistrar.class);
  }
}
