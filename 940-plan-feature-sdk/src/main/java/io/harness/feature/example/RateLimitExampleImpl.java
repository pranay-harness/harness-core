/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.feature.example;

import io.harness.feature.interfaces.RateLimitInterface;

import com.google.inject.Singleton;

@Singleton
public class RateLimitExampleImpl implements RateLimitInterface {
  @Override
  public long getCurrentValue(String accountIdentifier, long startTime, long endTime) {
    return 20;
  }
}
