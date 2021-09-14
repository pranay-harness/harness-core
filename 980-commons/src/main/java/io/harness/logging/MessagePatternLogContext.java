/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.logging;

public class MessagePatternLogContext extends AutoLogContext {
  public static final String ID = "LogMessagePattern";

  public MessagePatternLogContext(String key, OverrideBehavior behavior) {
    super(ID, key, behavior);
  }
}
