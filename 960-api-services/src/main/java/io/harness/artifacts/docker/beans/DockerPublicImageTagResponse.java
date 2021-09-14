/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.artifacts.docker.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;

@OwnedBy(CDC)
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerPublicImageTagResponse {
  @Getter
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Result {
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Image {
      private long size;
      private String architecture;
      private String variant;
      private String features;
      private String os;
      @JsonProperty("os_version") private String osVersion;
      @JsonProperty("os_features") private String osFeatures;
    }

    private String name;
    private String id;
    private int repository;
    private int creator;
    private boolean v2;
    @JsonProperty("last_updater") private int lastUpdater;
    @JsonProperty("full_size") private String fullSize;
    @JsonProperty("last_updated") private String lastUpdated;
    private List<Image> images;
  }

  private Integer count;
  private String next;
  private String previous;
  private List<Result> results;
}
