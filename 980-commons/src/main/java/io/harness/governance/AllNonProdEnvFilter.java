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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@JsonTypeName("ALL_NON_PROD")
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.CDC)
public class AllNonProdEnvFilter extends EnvironmentFilter {
  @Builder
  @JsonCreator
  public AllNonProdEnvFilter(@JsonProperty("filterType") EnvironmentFilterType filterType) {
    super(filterType);
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("ALL_NON_PROD")
  public static final class Yaml extends EnvironmentFilterYaml {
    @Builder
    public Yaml(@JsonProperty("filterType") EnvironmentFilterType environmentFilterType) {
      super(environmentFilterType);
    }

    public Yaml() {}
  }
}
