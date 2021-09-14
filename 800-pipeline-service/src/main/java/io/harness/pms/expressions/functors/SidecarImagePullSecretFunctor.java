/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.expressions.functors;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.ExpressionFunctor;
import io.harness.ngpipeline.artifact.bean.ArtifactsOutcome;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.expressions.utils.ImagePullSecretUtils;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
public class SidecarImagePullSecretFunctor implements ExpressionFunctor {
  ImagePullSecretUtils imagePullSecretUtils;
  Ambiance ambiance;
  ArtifactsOutcome artifactsOutcome;

  public Object get(String artifactIdentifier) {
    if (artifactsOutcome == null || artifactsOutcome.getSidecars() == null
        || artifactsOutcome.getSidecars().get(artifactIdentifier) == null) {
      return null;
    }
    return imagePullSecretUtils.getImagePullSecret(artifactsOutcome.getSidecars().get(artifactIdentifier), ambiance);
  }
}
