/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans.cvnglog;

public enum CVNGLogType {
  API_CALL_LOG("ApiCallLog"),
  EXECUTION_LOG("ExecutionLog");

  private String displayName;

  CVNGLogType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
