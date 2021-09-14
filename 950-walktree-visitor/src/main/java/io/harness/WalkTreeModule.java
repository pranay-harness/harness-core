/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness;

import io.harness.registrars.WalkTreeVisitorFieldRegistrar;
import io.harness.walktree.registries.VisitorRegistryModule;
import io.harness.walktree.registries.registrars.VisitableFieldRegistrar;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

public class WalkTreeModule extends AbstractModule {
  private static volatile WalkTreeModule instance;

  public static WalkTreeModule getInstance() {
    if (instance == null) {
      instance = new WalkTreeModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    install(VisitorRegistryModule.getInstance());
    MapBinder<String, VisitableFieldRegistrar> visitableFieldRegistrarMapBinder =
        MapBinder.newMapBinder(binder(), String.class, VisitableFieldRegistrar.class);
    visitableFieldRegistrarMapBinder.addBinding(WalkTreeVisitorFieldRegistrar.class.getName())
        .to(WalkTreeVisitorFieldRegistrar.class);
  }
}
