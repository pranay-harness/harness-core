package io.harness.executions.steps;

import io.harness.pms.sdk.core.data.Metadata;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LoggingMetadata implements Metadata {
  private String baseLoggingKey;
  private List<String> commandUnits;
}
