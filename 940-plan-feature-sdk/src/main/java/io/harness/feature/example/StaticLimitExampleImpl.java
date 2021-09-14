/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.feature.example;

import io.harness.feature.interfaces.StaticLimitInterface;

import com.google.inject.Singleton;

@Singleton
public class StaticLimitExampleImpl implements StaticLimitInterface {
  @Override
  public long getCurrentValue(String accountIdentifier) {
    return 10;
  }
}
