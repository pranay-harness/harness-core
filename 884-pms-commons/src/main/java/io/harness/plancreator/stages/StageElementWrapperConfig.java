/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.plancreator.stages;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("stageElementWrapperConfig")
// TODO (sahil) this needs to go to yaml commons right now as we have no module marking it for PMS commons
@OwnedBy(HarnessTeam.PIPELINE)
public class StageElementWrapperConfig {
  @ApiModelProperty(dataType = "io.harness.plancreator.stages.stage.StageElementConfig") JsonNode stage;
  @ApiModelProperty(dataType = "io.harness.plancreator.stages.parallel.ParallelStageElementConfig") JsonNode parallel;
}
