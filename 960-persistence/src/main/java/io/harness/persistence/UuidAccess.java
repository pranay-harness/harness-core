/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.persistence;

public interface UuidAccess {
  String UUID_KEY = "uuid";

  String getUuid();

  default String logKeyForId() {
    return LogKeyUtils.logKeyForId(getClass());
  }
}
