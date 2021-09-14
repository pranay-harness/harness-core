/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.service.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ToBeDeleted;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
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
@RecasterAlias("io.harness.cdng.service.beans.ServiceConfigOutcome")
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
