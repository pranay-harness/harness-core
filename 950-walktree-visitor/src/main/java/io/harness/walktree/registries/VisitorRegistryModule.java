/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.walktree.registries;

import io.harness.walktree.registries.registrars.VisitableFieldRegistrar;
import io.harness.walktree.registries.visitorfield.VisitableFieldProcessor;
import io.harness.walktree.registries.visitorfield.VisitorFieldRegistry;
import io.harness.walktree.registries.visitorfield.VisitorFieldType;
import io.harness.walktree.registries.visitorfield.VisitorFieldWrapper;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

public class VisitorRegistryModule extends AbstractModule {
  private static VisitorRegistryModule instance;

  public static synchronized VisitorRegistryModule getInstance() {
    if (instance == null) {
      instance = new VisitorRegistryModule();
    }
    return instance;
  }

  @Provides
  @Singleton
  VisitorFieldRegistry providesVisitorFieldRegistry(
      Injector injector, Map<String, VisitableFieldRegistrar> visitableFieldRegistrarMap) {
    Set<Pair<VisitorFieldType, VisitableFieldProcessor<?>>> classes = new HashSet<>();
    visitableFieldRegistrarMap.values().forEach(visitableFieldRegistrar -> visitableFieldRegistrar.register(classes));
    VisitorFieldRegistry visitorFieldRegistry = new VisitorFieldRegistry();
    injector.injectMembers(visitorFieldRegistry);
    classes.forEach(pair -> { visitorFieldRegistry.register(pair.getLeft(), pair.getRight()); });

    Set fieldTypeClasses = new HashSet<>();
    visitableFieldRegistrarMap.values().forEach(
        visitableFieldRegistrar -> { visitableFieldRegistrar.registerFieldTypes(fieldTypeClasses); });
    fieldTypeClasses.forEach(pair -> {
      Pair<Class<? extends VisitorFieldWrapper>, VisitorFieldType> fieldTypePair =
          (Pair<Class<? extends VisitorFieldWrapper>, VisitorFieldType>) pair;
      visitorFieldRegistry.registerFieldTypes(fieldTypePair.getLeft(), fieldTypePair.getRight());
    });

    return visitorFieldRegistry;
  }
}
