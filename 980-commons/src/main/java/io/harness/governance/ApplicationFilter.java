/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.governance;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "filterType", include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = CustomAppFilter.class, name = "CUSTOM")
  , @JsonSubTypes.Type(value = AllAppFilter.class, name = "ALL")
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@OwnedBy(HarnessTeam.CDC)
public abstract class ApplicationFilter implements BlackoutWindowFilter {
  private BlackoutWindowFilterType filterType;
  private EnvironmentFilter envSelection;
  private ServiceFilter serviceSelection;

  @JsonCreator
  public ApplicationFilter(@JsonProperty("filterType") BlackoutWindowFilterType filterType,
      @JsonProperty("envSelection") EnvironmentFilter envSelection,
      @JsonProperty("serviceSelection") ServiceFilter serviceSelection) {
    this.envSelection = envSelection;
    this.filterType = filterType;
    this.serviceSelection = serviceSelection;
  }
}
