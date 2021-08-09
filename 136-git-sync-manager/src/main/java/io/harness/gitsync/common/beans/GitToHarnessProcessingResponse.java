package io.harness.gitsync.common.beans;

import io.harness.Microservice;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitToHarnessProcessingResponse {
  GitToHarnessProcessingResponseDTO processingResponse;
  Microservice microservice;
}
