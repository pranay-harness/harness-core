/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.governance;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@JsonTypeName("ALL")
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.CDC)
@JsonInclude(NON_NULL)
public class AllAppFilter extends ApplicationFilter {
  @JsonCreator
  @Builder
  public AllAppFilter(@JsonProperty("filterType") BlackoutWindowFilterType blackoutWindowFilterType,
      @JsonProperty("envSelection") EnvironmentFilter envSelection,
      @JsonProperty("serviceSelection") ServiceFilter serviceSelection) {
    super(blackoutWindowFilterType, envSelection, serviceSelection);
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("ALL")
  public static final class Yaml extends ApplicationFilterYaml {
    @Builder
    public Yaml(BlackoutWindowFilterType filterType, List<EnvironmentFilterYaml> envSelection,
        List<ServiceFilter.Yaml> serviceSelection) {
      super(filterType, envSelection, serviceSelection);
    }

    public Yaml() {}
  }
}
