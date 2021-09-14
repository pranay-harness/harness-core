/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.resourcegroup.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, visible = true, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = StaticResourceSelector.class, name = "StaticResourceSelector")
  , @JsonSubTypes.Type(value = DynamicResourceSelector.class, name = "DynamicResourceSelector")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public interface ResourceSelector {}
