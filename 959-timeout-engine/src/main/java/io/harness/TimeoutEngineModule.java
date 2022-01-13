/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
