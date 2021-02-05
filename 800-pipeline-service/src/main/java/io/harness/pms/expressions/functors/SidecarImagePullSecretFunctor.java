package io.harness.pms.expressions.functors;

import io.harness.engine.pms.data.PmsOutcomeService;
import io.harness.expression.ExpressionFunctor;
import io.harness.ngpipeline.artifact.bean.ArtifactOutcome;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.expressions.utils.ImagePullSecretUtils;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.mapper.PmsOutcomeMapper;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SidecarImagePullSecretFunctor implements ExpressionFunctor {
  ImagePullSecretUtils imagePullSecretUtils;
  PmsOutcomeService pmsOutcomeService;
  Ambiance ambiance;

  public Object get(String artifactIdentifier) {
    ArtifactOutcome artifact = (ArtifactOutcome) PmsOutcomeMapper.convertJsonToOutcome(pmsOutcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject("service.artifactsResult.sidecars." + artifactIdentifier)));
    if (artifact == null) {
      return null;
    }
    return imagePullSecretUtils.getImagePullSecret(artifact, ambiance);
  }
}
