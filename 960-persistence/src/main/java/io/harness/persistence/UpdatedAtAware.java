/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.persistence;

public interface UpdatedAtAware extends UpdatedAtAccess {
  String LAST_UPDATED_AT_KEY = "lastUpdatedAt";

  void setLastUpdatedAt(long lastUpdatedAt);
}
