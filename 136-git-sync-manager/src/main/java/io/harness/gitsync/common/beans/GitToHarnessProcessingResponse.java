package io.harness.gitsync.common.beans;

import io.harness.Microservice;
import io.harness.gitsync.ProcessingResponse;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitToHarnessProcessingResponse {
  ProcessingResponse processingResponse;
  Microservice microservice;
}
