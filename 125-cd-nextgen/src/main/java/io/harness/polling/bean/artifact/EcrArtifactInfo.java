package io.harness.polling.bean.artifact;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.EcrArtifactConfig;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.pms.yaml.ParameterField;

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

  @Override
  public ArtifactConfig toArtifactConfig() {
    return EcrArtifactConfig.builder()
        .connectorRef(ParameterField.<String>builder().value(connectorRef).build())
        .region(ParameterField.<String>builder().value(region).build())
        .imagePath(ParameterField.<String>builder().value(imagePath).build())
        .build();
  }
}
