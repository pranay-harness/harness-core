/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.logging;

public class MorphiaLoggerFactory implements org.mongodb.morphia.logging.LoggerFactory {
  @Override
  public org.mongodb.morphia.logging.Logger get(Class<?> c) {
    return new MorphiaLogger(c);
  }
}
