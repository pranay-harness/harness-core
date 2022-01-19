/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.filter.dto;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.filter.dto.FilterVisibility.EVERYONE;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.persistence.PersistentEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "Filter", description = "This has details of the Filter entity defined in Harness")
public class FilterDTO implements PersistentEntity {
  @Schema(description = "Name of the Filter.") @NotNull String name;
  @NotNull @EntityIdentifier @Schema(description = "Identifier of the Filter.") String identifier;
  @Schema(description = NGCommonEntityConstants.ORG_PARAM_MESSAGE) String orgIdentifier;
  @Schema(description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE) String projectIdentifier;
  @NotNull FilterPropertiesDTO filterProperties;
  @Builder.Default
  @Schema(description = "This indicates visibility of Filter, by default it is Everyone.")
  FilterVisibility filterVisibility = EVERYONE;
}
