/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngtriggers.beans.source;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(PIPELINE)
public enum ManifestType {
  @JsonProperty("HelmChart") HELM_MANIFEST("HelmChart");

  private String value;

  ManifestType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
