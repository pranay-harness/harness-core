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
