/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.nexus.model;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@OwnedBy(CDC)
@lombok.Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Nexus3ComponentResponse {
  private List<Component> items;
  private String continuationToken;

  @lombok.Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Component {
    private String id;
    private String repository;
    private String format;
    private String group;
    private String name;
    private String version;
    private List<Asset> assets;
  }
}
