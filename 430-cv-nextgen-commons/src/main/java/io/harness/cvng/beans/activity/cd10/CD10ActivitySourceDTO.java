/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans.activity.cd10;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.activity.ActivitySourceDTO;
import io.harness.cvng.beans.activity.ActivitySourceType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Collections;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonTypeName("HARNESS_CD10")
@OwnedBy(HarnessTeam.CV)
public class CD10ActivitySourceDTO extends ActivitySourceDTO {
  Set<CD10EnvMappingDTO> envMappings;
  Set<CD10ServiceMappingDTO> serviceMappings;

  public Set<CD10ServiceMappingDTO> getServiceMappings() {
    if (serviceMappings == null) {
      return Collections.emptySet();
    }
    return serviceMappings;
  }

  public Set<CD10EnvMappingDTO> getEnvMappings() {
    if (envMappings == null) {
      return Collections.emptySet();
    }
    return envMappings;
  }
  @Override
  public ActivitySourceType getType() {
    return ActivitySourceType.HARNESS_CD10;
  }

  @Override
  public boolean isEditable() {
    return true;
  }
}
