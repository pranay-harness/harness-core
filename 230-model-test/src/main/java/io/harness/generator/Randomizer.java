/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.generator;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Randomizer {
  @Value
  @AllArgsConstructor
  public static class Seed {
    long value;
  }

  public static EnhancedRandom instance(Seed seed) {
    return EnhancedRandomBuilder.aNewEnhancedRandomBuilder().objectPoolSize(1).seed(seed.getValue()).build();
  }
}
