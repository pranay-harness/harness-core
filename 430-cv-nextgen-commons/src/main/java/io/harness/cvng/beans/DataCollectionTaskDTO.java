/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DataCollectionTaskDTO {
  String uuid;
  String accountId;
  String verificationTaskId;
  DataCollectionInfo dataCollectionInfo;
  Instant startTime;
  Instant endTime;

  @Value
  @Builder
  public static class DataCollectionTaskResult {
    String dataCollectionTaskId;
    DataCollectionExecutionStatus status;
    String exception;
    String stacktrace;
  }
}
