package io.harness.beans;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Data
@Value
@Builder
public class GCPArtifactUploadInfo implements ArtifactUploadInfo {
  private ArtifactUploadInfo.Type type = Type.GCP;

  GCPArtifactUploadInfo() {}

  @Override
  public ArtifactUploadInfo.Type getType() {
    return type;
  }
}
