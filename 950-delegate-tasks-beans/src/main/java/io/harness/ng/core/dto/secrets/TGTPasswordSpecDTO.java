/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.dto.secrets;

import io.harness.beans.DecryptableEntity;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.harness.ng.core.models.TGTGenerationSpec;
import io.harness.ng.core.models.TGTPasswordSpec;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("Password")
public class TGTPasswordSpecDTO extends TGTGenerationSpecDTO implements DecryptableEntity {
  @ApiModelProperty(dataType = "string") @SecretReference private SecretRefData password;

  @Override
  public TGTGenerationSpec toEntity() {
    return TGTPasswordSpec.builder().password(getPassword()).build();
  }
}
