package io.harness.cdng.service.beans;

import io.harness.cdng.artifact.bean.ArtifactOutcome;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("serviceOutcome")
@JsonTypeName("serviceOutcome")
public class ServiceOutcome implements Outcome {
  String identifier;
  String name;
  String description;
  String type;
  @Builder.Default Map<String, String> tags = new HashMap<>();

  // For expressions
  @Singular Map<String, Object> variables;
  @Singular Map<String, Map<String, Object>> artifacts;
  @Singular Map<String, Map<String, Object>> manifests;

  ArtifactsOutcome artifactsResult;
  @Singular Map<String, ManifestOutcome> manifestResults;

  @Singular Map<String, ArtifactsWrapperOutcome> artifactOverrideSets;
  @Singular Map<String, VariablesWrapperOutcome> variableOverrideSets;
  @Singular Map<String, ManifestsWrapperOutcome> manifestOverrideSets;

  StageOverridesOutcome stageOverrides;

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

  @Data
  @Builder
  @TypeAlias("serviceOutcome_stageOverridesOutcome")
  @JsonTypeName("serviceOutcome_stageOverridesOutcome")
  public static class StageOverridesOutcome implements Outcome {
    Map<String, Object> variables;
    ArtifactsOutcome artifacts;
    @Singular Map<String, ManifestOutcome> manifests;

    ParameterField<List<String>> useVariableOverrideSets;
    ParameterField<List<String>> useArtifactOverrideSets;
    ParameterField<List<String>> useManifestOverrideSets;

    @Override
    public String getType() {
      return "serviceOutcome_stageOverridesOutcome";
    }
  }

  @Data
  @Builder
  @TypeAlias("serviceOutcome_artifactsWrapperOutcome")
  @JsonTypeName("serviceOutcome_artifactsWrapperOutcome")
  public static class ArtifactsWrapperOutcome implements Outcome {
    ArtifactsOutcome artifacts;

    @Override
    public String getType() {
      return "serviceOutcome_artifactsWrapperOutcome";
    }
  }

  @Data
  @Builder
  @TypeAlias("serviceOutcome_manifestsWrapperOutcome")
  @JsonTypeName("serviceOutcome_manifestsWrapperOutcome")
  public static class ManifestsWrapperOutcome implements Outcome {
    Map<String, ManifestOutcome> manifests;

    @Override
    public String getType() {
      return "serviceOutcome_manifestsWrapperOutcome";
    }
  }

  @Data
  @Builder
  @TypeAlias("serviceOutcome_variablesWrapperOutcome")
  @JsonTypeName("serviceOutcome_variablesWrapperOutcome")
  public static class VariablesWrapperOutcome implements Outcome {
    Map<String, Object> variables;

    @Override
    public String getType() {
      return "serviceOutcome_variablesWrapperOutcome";
    }
  }
}
