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
  @JsonSubTypes.Type(value = CustomEnvFilter.class, name = "CUSTOM")
  , @JsonSubTypes.Type(value = AllEnvFilter.class, name = "ALL"),
      @JsonSubTypes.Type(value = AllProdEnvFilter.class, name = "ALL_PROD"),
      @JsonSubTypes.Type(value = AllNonProdEnvFilter.class, name = "ALL_NON_PROD")
})
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@OwnedBy(HarnessTeam.CDC)
public class EnvironmentFilter {
  private EnvironmentFilterType filterType;
  public enum EnvironmentFilterType { ALL_PROD, ALL_NON_PROD, ALL, CUSTOM }

  @JsonCreator
  public EnvironmentFilter(@JsonProperty("filterType") EnvironmentFilterType filterType) {
    this.filterType = filterType;
  }
}
