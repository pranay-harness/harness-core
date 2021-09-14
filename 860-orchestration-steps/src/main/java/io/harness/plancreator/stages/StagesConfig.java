/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.plancreator.stages;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("stagesConfig")
public class StagesConfig {
  List<StageElementWrapperConfig> stages;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public StagesConfig(List<StageElementWrapperConfig> stages) {
    this.stages = stages;
  }
}
