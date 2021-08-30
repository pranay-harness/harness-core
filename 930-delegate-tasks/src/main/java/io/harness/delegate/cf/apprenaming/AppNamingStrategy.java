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
