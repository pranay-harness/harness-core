/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.dto.secrets;

import io.harness.beans.DecryptableEntity;
import io.harness.ng.core.models.TGTGenerationSpec;
import io.harness.ng.core.models.TGTKeyTabFilePathSpec;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("KeyTabFilePath")
public class TGTKeyTabFilePathSpecDTO extends TGTGenerationSpecDTO implements DecryptableEntity {
  private String keyPath;

  @Override
  public TGTGenerationSpec toEntity() {
    return TGTKeyTabFilePathSpec.builder().keyPath(getKeyPath()).build();
  }
}
