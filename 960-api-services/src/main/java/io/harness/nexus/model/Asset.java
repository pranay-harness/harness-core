/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.nexus.model;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@lombok.Data
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.CDC)
public class Asset {
  private String downloadUrl;
  private String path;
  private String id;
  private String repository;
  private String format;
  private Checksum checksum;

  @lombok.Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class Checksum {
    private String sha1;
    private String sha512;
  }
}
