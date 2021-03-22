package io.harness.cdng.service.beans;

import io.harness.steps.shellScript.manifest.yaml.ManifestOutcome;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("serviceConfigOutcome")
@JsonTypeName("serviceConfigOutcome")
public class ServiceConfigOutcome implements Outcome {
  ServiceOutcome service;

  // For expressions
  @Singular Map<String, Object> variables;
  @Singular Map<String, Map<String, Object>> artifacts;
  @Singular Map<String, Map<String, Object>> manifests;

  // When changing the name of this variable, change ImagePullSecretFunctor.
  ServiceOutcome.ArtifactsOutcome artifactsResult;
  @Singular Map<String, ManifestOutcome> manifestResults;

  @Singular Map<String, ServiceOutcome.ArtifactsWrapperOutcome> artifactOverrideSets;
  @Singular Map<String, ServiceOutcome.VariablesWrapperOutcome> variableOverrideSets;
  @Singular Map<String, ServiceOutcome.ManifestsWrapperOutcome> manifestOverrideSets;

  ServiceOutcome.StageOverridesOutcome stageOverrides;

  @Override
  public String getType() {
    return "serviceConfigOutcome";
  }

  @Override
  public String toViewJson() {
    Map<String, Object> viewObject = new LinkedHashMap<>();
    if (artifactsResult != null) {
      viewObject.put("artifactsResult", artifactsResult);
    }
    viewObject.put("manifestResults", manifestResults);
    return RecastOrchestrationUtils.toDocumentJson(viewObject);
  }
}
