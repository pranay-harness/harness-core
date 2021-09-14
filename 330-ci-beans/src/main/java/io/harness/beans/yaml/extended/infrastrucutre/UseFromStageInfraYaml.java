/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans.yaml.extended.infrastrucutre;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CI)
@Data
@Builder
@JsonTypeName("UseFromStage")
@TypeAlias("useFromStageInfraYaml")
public class UseFromStageInfraYaml implements Infrastructure {
  @NotNull private String useFromStage;

  @JsonIgnore
  @Override
  public Type getType() {
    return Type.USE_FROM_STAGE;
  }
}
