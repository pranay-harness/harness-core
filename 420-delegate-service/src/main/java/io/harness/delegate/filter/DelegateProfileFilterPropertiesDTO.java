/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.filter;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.filter.FilterConstants.DELEGATE_PROFILE_FILTER;

import io.harness.annotations.dev.OwnedBy;
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

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(DELEGATE_PROFILE_FILTER)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("DelegateProfileFilterProperties")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(DEL)
public class DelegateProfileFilterPropertiesDTO extends FilterPropertiesDTO {
  private String name;
  private String identifier;
  private String description;
  private boolean approvalRequired;
  private List<String> selectors;

  @Override
  public FilterType getFilterType() {
    return FilterType.DELEGATEPROFILE;
  }
}
