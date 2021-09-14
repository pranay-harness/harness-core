/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans.activity;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.CV)
public abstract class ActivitySourceDTO {
  // TODO: keeping it in the base class but we need to separate out UI entities and entities that are updated from
  // delegate.
  String uuid;
  long createdAt;
  long lastUpdatedAt;
  @NotNull String identifier;
  @NotNull String name;
  String orgIdentifier;
  String projectIdentifier;

  public abstract ActivitySourceType getType();
  public abstract boolean isEditable();
}
