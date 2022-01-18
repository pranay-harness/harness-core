/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.audit.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.filter.FilterConstants.AUDIT_FILTER;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.Action;
import io.harness.filter.FilterType;
import io.harness.filter.dto.FilterPropertiesDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(AUDIT_FILTER)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("AuditFilterProperties")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AuditFilterPropertiesDTO extends FilterPropertiesDTO {
  List<ResourceScopeDTO> scopes;
  List<ResourceDTO> resources;

  List<ModuleType> modules;
  List<Action> actions;
  List<Environment> environments;
  List<Principal> principals;

  Long startTime;
  Long endTime;

  @Override
  public FilterType getFilterType() {
    return FilterType.AUDIT;
  }
}
