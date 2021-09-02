package io.harness.polling.bean.artifact;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.GcrArtifactConfig;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.pms.yaml.ParameterField;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
public class GcrArtifactInfo implements ArtifactInfo {
  String connectorRef;
  String registryHostname;
  String imagePath;

  @Override
  public ArtifactSourceType getType() {
    return ArtifactSourceType.GCR;
  }

  @Override
  public ArtifactConfig toArtifactConfig() {
    return GcrArtifactConfig.builder()
        .connectorRef(ParameterField.<String>builder().value(connectorRef).build())
        .registryHostname(ParameterField.<String>builder().value(registryHostname).build())
        .imagePath(ParameterField.<String>builder().value(imagePath).build())
        .build();
  }
}
