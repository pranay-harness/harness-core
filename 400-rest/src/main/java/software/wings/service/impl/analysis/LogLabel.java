/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

public enum LogLabel {
  NOISE,
  CLEAN,
  INFRA,
  THIRD_PARTY,
  IMPORTANT,
  BACKGROUND,
  JAVA_THROWABLE,
  ERROR,
  EXCEPTION,
  RUNTIME,
  HTTP,
  UPSTREAM,
  DOWNSTREAM,
  DATABASE,
  NETWORK,
  APM,
  LOGS,
  JVM,
  WARN
}
