package io.harness.beans;

public interface ArtifactUploadInfo {
  enum Type { GCP }

  Type getType();
}
