package io.harness.plancreator.stages;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("stageElementWrapperConfig")
// TODO (sahil) this needs to go to yaml commons right now as we have no module marking it for PMS commons
@TargetModule(HarnessModule._884_PMS_COMMONS)
public class StageElementWrapperConfig {
  @ApiModelProperty(dataType = "io.harness.plancreator.stages.stage.StageElementConfig") JsonNode stage;
  @ApiModelProperty(dataType = "io.harness.plancreator.stages.parallel.ParallelStageElementConfig") JsonNode parallel;
}
