package io.harness.gitsync.common.beans;

import io.harness.Microservice;

import com.mongodb.lang.Nullable;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitToHarnessProcessingResponse {
  GitToHarnessProcessingResponseDTO processingResponse;
  @Nullable Microservice microservice;
}
