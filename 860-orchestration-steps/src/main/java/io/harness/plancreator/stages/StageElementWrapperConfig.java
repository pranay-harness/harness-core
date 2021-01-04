package io.harness.plancreator.stages;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("stageElementWrapperConfig")
public class StageElementWrapperConfig {
  @ApiModelProperty(dataType = "io.harness.plancreator.stages.stage.StageElementConfig") JsonNode stage;
  @ApiModelProperty(dataType = "io.harness.plancreator.stages.parallel.ParallelStageElementConfig") JsonNode parallel;
}
