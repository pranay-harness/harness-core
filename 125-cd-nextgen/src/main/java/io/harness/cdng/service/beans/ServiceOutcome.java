package io.harness.cdng.service.beans;

import io.harness.cdng.artifact.bean.ArtifactOutcome;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.data.Outcome;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class ServiceOutcome implements Outcome {
  String identifier;
  String displayName;
  String description;
  String deploymentType;
  ArtifactsOutcome artifacts;
  List<ManifestAttributes> manifests;

  @Data
  @Builder
  public static class ArtifactsOutcome implements Outcome {
    private ArtifactOutcome primary;
    @Singular private Map<String, ArtifactOutcome> sidecars;
  }
}
