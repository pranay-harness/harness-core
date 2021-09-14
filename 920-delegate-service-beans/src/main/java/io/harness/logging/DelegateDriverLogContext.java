/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.logging;

public class DelegateDriverLogContext extends AutoLogContext {
  public static final String ID = "driverId";

  public DelegateDriverLogContext(String driverId, OverrideBehavior behavior) {
    super(ID, driverId, behavior);
  }
}
