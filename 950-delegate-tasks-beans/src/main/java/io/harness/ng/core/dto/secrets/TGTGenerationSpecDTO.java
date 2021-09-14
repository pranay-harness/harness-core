/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.dto.secrets;

import io.harness.ng.core.models.TGTGenerationSpec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "tgtGenerationMethod",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = TGTKeyTabFilePathSpecDTO.class, name = "KeyTabFilePath")
  , @JsonSubTypes.Type(value = TGTPasswordSpecDTO.class, name = "Password")
})
public abstract class TGTGenerationSpecDTO {
  public abstract TGTGenerationSpec toEntity();
}
