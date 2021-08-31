package io.harness.polling.bean.artifact;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.artifacts.ArtifactSourceType;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
public class EcrArtifactInfo implements ArtifactInfo {
  String connectorRef;
  String region;
  String imagePath;

  @Override
  public ArtifactSourceType getType() {
    return ArtifactSourceType.ECR;
  }
}
