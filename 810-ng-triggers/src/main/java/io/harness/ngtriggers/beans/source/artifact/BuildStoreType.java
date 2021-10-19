package io.harness.ngtriggers.beans.source.artifact;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;

@OwnedBy(PIPELINE)
public enum BuildStoreType {
  @JsonProperty("Http") HTTP("Http"),
  @JsonProperty("S3") S3("S3"),
  @JsonProperty("Gcs") GCS("Gcs");

  private String value;

  BuildStoreType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
