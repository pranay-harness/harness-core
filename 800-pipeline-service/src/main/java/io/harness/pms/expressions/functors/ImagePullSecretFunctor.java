/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.expressions.functors;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.pms.data.PmsOutcomeService;
import io.harness.expression.ExpressionFunctor;
import io.harness.ngpipeline.artifact.bean.ArtifactsOutcome;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.expressions.utils.ImagePullSecretUtils;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.mapper.PmsOutcomeMapper;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
public class ImagePullSecretFunctor implements ExpressionFunctor {
  public static final String IMAGE_PULL_SECRET = "imagePullSecret";

  private static final String PRIMARY_ARTIFACT = "primary";
  private static final String SIDECAR_ARTIFACTS = "sidecars";

  ImagePullSecretUtils imagePullSecretUtils;
  PmsOutcomeService pmsOutcomeService;
  Ambiance ambiance;

  public Object get(String artifactIdentifier) {
    if (artifactIdentifier.equals(PRIMARY_ARTIFACT)) {
      ArtifactsOutcome artifactsOutcome = fetchArtifactsOutcome();
      if (artifactsOutcome == null || artifactsOutcome.getPrimary() == null) {
        return null;
      }
      return imagePullSecretUtils.getImagePullSecret(artifactsOutcome.getPrimary(), ambiance);
    } else if (artifactIdentifier.equals(SIDECAR_ARTIFACTS)) {
      return SidecarImagePullSecretFunctor.builder()
          .imagePullSecretUtils(imagePullSecretUtils)
          .ambiance(ambiance)
          .artifactsOutcome(fetchArtifactsOutcome())
          .build();
    } else {
      return null;
    }
  }

  private ArtifactsOutcome fetchArtifactsOutcome() {
    return (ArtifactsOutcome) PmsOutcomeMapper.convertJsonToOutcome(
        pmsOutcomeService.resolve(ambiance, RefObjectUtils.getOutcomeRefObject("artifacts")));
  }
}
