package io.harness.cdng.service.beans;

import io.harness.cdng.artifact.bean.ArtifactOutcome;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("serviceOutcome")
@JsonTypeName("serviceOutcome")
public class ServiceOutcome implements Outcome {
  String identifier;
  String displayName;
  String description;
  String deploymentType;
  ArtifactsOutcome artifacts;
  List<ManifestAttributes> manifests1;
  @Singular Map<String, ManifestOutcome> manifests;

  @Override
  public String getType() {
    return "serviceOutcome";
  }

  @Data
  @Builder
  @TypeAlias("serviceOutcome_artifactsOutcome")
  @JsonTypeName("serviceOutcome_artifactsOutcome")
  public static class ArtifactsOutcome implements Outcome {
    private ArtifactOutcome primary;
    @Singular private Map<String, ArtifactOutcome> sidecars;

    @Override
    public String getType() {
      return "serviceOutcome_artifactsOutcome";
    }
  }
}
