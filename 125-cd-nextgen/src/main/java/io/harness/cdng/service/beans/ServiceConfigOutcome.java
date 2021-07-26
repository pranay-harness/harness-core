package io.harness.cdng.service.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ToBeDeleted;
import io.harness.manifest.ManifestOutcome;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("serviceConfigOutcome")
@JsonTypeName("serviceConfigOutcome")
@OwnedBy(CDC)
@ToBeDeleted
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
}
