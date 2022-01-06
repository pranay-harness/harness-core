/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.governance;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.yaml.BaseYaml;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, property = "filterType", include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AllAppFilter.Yaml.class, name = "ALL")
  , @JsonSubTypes.Type(value = CustomAppFilter.Yaml.class, name = "CUSTOM")
})
@OwnedBy(HarnessTeam.CDC)
public abstract class ApplicationFilterYaml extends BaseYaml {
  private List<EnvironmentFilterYaml> envSelection;
  private List<ServiceFilter.Yaml> serviceSelection;
  private BlackoutWindowFilterType filterType;

  public ApplicationFilterYaml(@JsonProperty("filterType") BlackoutWindowFilterType filterType,
      @JsonProperty("envSelection") List<EnvironmentFilterYaml> envSelection,
      @JsonProperty("serviceSelection") List<ServiceFilter.Yaml> serviceSelection) {
    this.filterType = filterType;
    this.envSelection = envSelection;
    this.serviceSelection = serviceSelection;
  }
}
