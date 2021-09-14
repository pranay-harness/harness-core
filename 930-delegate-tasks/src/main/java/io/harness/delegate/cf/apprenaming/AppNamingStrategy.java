/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.cf.apprenaming;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

public enum AppNamingStrategy {
  VERSIONING,
  APP_NAME_WITH_VERSIONING;

  public static AppNamingStrategy get(String namingStrategy) {
    if (isEmpty(namingStrategy)) {
      return VERSIONING;
    }

    for (AppNamingStrategy strategy : values()) {
      if (strategy.name().equalsIgnoreCase(namingStrategy)) {
        return strategy;
      }
    }
    return VERSIONING;
  }
}
