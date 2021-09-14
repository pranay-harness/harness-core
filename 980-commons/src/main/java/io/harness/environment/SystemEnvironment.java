/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.environment;

import com.google.inject.Singleton;

@Singleton
public class SystemEnvironment {
  public String get(String name) {
    return System.getenv(name);
  }
}
