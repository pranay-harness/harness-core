package io.harness.cdng.pipeline.executions.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InfraExecutionSummary {
  String identifier;
  String name;
}
