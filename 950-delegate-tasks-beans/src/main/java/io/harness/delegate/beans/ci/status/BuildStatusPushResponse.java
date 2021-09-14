/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.ci.status;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BuildStatusPushResponse implements BuildPushResponse {
  public enum Status {
    SUCCESS,
    ERROR;
  }

  Status status;
}
