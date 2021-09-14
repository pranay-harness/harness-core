/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.ci.k8s;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CIContainerStatus {
  public enum Status {
    SUCCESS,
    ERROR;
  }

  String name;
  String image;
  String startTime;
  String endTime;
  Status status;
  String errorMsg;
}
