/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidArgumentsException;

import com.google.inject.Injector;
import java.util.HashMap;
import java.util.Map;

@OwnedBy(PL)
public final class GodInjector {
  private final Map<String, Injector> injectorMap;

  public GodInjector() {
    injectorMap = new HashMap<>();
  }

  public Injector put(String injectorName, Injector injector) {
    if (isEmpty(injectorName)) {
      throw new InvalidArgumentsException("Injector Name must not be empty!");
    }
    if (injector == null) {
      throw new InvalidArgumentsException("Injector must not be null!");
    }
    if (injectorMap.get(injectorName) != null) {
      throw new InvalidArgumentsException(
          String.format("Injector Map already contains mapping for injectorName %s", injectorName));
    }
    return injectorMap.put(injectorName, injector);
  }

  public Injector get(String injectorName) {
    if (injectorName == null) {
      return null;
    }
    return injectorMap.get(injectorName);
  }
}
