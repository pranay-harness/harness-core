/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.governance;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.governance.EnvironmentFilter.EnvironmentFilterType;
import io.harness.yaml.BaseYaml;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "filterType", include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = CustomEnvFilter.Yaml.class, name = "CUSTOM")
  , @JsonSubTypes.Type(value = AllEnvFilter.Yaml.class, name = "ALL"),
      @JsonSubTypes.Type(value = AllProdEnvFilter.Yaml.class, name = "ALL_PROD"),
      @JsonSubTypes.Type(value = AllNonProdEnvFilter.Yaml.class, name = "ALL_NON_PROD")
})
@OwnedBy(HarnessTeam.CDC)
public abstract class EnvironmentFilterYaml extends BaseYaml {
  private EnvironmentFilterType filterType;

  public EnvironmentFilterYaml(@JsonProperty("filterType") EnvironmentFilterType filterType) {
    this.filterType = filterType;
  }
}
