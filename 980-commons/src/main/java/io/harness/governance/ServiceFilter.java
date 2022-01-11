/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.governance;

import io.harness.yaml.BaseYaml;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@EqualsAndHashCode(callSuper = false)
public class ServiceFilter {
  private ServiceFilterType filterType;
  private List<String> services;

  @JsonCreator
  public ServiceFilter(
      @JsonProperty("filterType") ServiceFilterType filterType, @JsonProperty("services") List<String> services) {
    this.filterType = filterType;
    this.services = services;
  }

  public enum ServiceFilterType { ALL, CUSTOM }

  @Getter
  @Setter
  @Builder
  @EqualsAndHashCode(callSuper = false)
  public static class Yaml extends BaseYaml {
    String filterType;
    List<String> services;
  }
}
