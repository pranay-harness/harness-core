package io.harness.plancreator.stages;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("stageElementWrapperConfig")
public class StageElementWrapperConfig {
  JsonNode stage;
  JsonNode parallel;
}
