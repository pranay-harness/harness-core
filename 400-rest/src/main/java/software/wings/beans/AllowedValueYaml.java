/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = EXTERNAL_PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = StringAllowedValue.class, name = "TEXT")
  , @JsonSubTypes.Type(value = NumberAllowedValue.class, name = "NUMBER"),
      @JsonSubTypes.Type(value = ArtifactStreamAllowedValueYaml.class, name = "ARTIFACT")
})
public interface AllowedValueYaml {}
