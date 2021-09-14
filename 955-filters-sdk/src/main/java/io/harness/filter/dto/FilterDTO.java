/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.filter.dto;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.filter.dto.FilterVisibility.EVERYONE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.persistence.PersistentEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "FilterKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(DX)
public class FilterDTO implements PersistentEntity {
  @NotNull String name;
  @NotNull @EntityIdentifier String identifier;
  String orgIdentifier;
  String projectIdentifier;
  @NotNull FilterPropertiesDTO filterProperties;
  @Builder.Default FilterVisibility filterVisibility = EVERYONE;
}
