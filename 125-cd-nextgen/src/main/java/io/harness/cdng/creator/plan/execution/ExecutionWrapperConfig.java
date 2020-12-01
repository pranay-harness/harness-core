package io.harness.cdng.creator.plan.execution;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("executionWrapperConfig")
public class ExecutionWrapperConfig {
  JsonNode step;
  JsonNode parallel;
  JsonNode stepGroup;
}
