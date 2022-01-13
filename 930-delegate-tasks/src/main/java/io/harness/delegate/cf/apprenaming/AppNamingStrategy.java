/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
