package io.harness.cvng.beans;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class DataCollectionTaskDTO {
  String uuid;
  String accountId;
  String cvConfigId;
  DataCollectionInfo dataCollectionInfo;
  Instant startTime;
  Instant endTime;

  @Value
  @Builder
  public static class DataCollectionTaskResult {
    String dataCollectionTaskId;
    ExecutionStatus status;
    String exception;
    String stacktrace;
  }
}
