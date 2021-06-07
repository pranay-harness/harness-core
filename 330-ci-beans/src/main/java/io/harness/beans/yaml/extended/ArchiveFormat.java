package io.harness.beans.yaml.extended;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ArchiveFormat {
  @JsonProperty("tar") TAR("tar"),
  @JsonProperty("gzip") GZIP("gzip");
  private final String displayName;

  @JsonCreator
  public static ArchiveFormat getArchiveFormat(@JsonProperty("archiveFormat") String displayName) {
    for (ArchiveFormat archiveFormat : ArchiveFormat.values()) {
      if (archiveFormat.displayName.equalsIgnoreCase(displayName)) {
        return archiveFormat;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + displayName);
  }

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

  public static ArchiveFormat fromString(final String s) {
    return ArchiveFormat.getArchiveFormat(s);
  }
}
