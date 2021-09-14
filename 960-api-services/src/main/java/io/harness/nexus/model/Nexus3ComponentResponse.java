/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
