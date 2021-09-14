/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.plancreator.stages.parallel;

import io.harness.plancreator.stages.StageElementWrapperConfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("parallelStageElementConfig")
public class ParallelStageElementConfig {
  @NotNull List<StageElementWrapperConfig> sections;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public ParallelStageElementConfig(List<StageElementWrapperConfig> sections) {
    this.sections = sections;
  }
}
