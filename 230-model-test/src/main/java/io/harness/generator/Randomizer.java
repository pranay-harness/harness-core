/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
