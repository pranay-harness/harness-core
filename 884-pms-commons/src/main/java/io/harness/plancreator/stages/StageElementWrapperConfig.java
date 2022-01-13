/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
