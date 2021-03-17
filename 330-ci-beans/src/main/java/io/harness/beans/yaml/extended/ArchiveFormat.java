package io.harness.beans.yaml.extended;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ArchiveFormat {
  @JsonProperty("tar") TAR("tar"),
  @JsonProperty("gzip") GZIP("gzip");

  private final String displayName;

  ArchiveFormat(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }
}
