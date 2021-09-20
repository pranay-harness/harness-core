package io.harness.pms.expressions.functors;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ngpipeline.artifact.bean.ArtifactsOutcome;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.expressions.utils.ImagePullSecretUtils;
import io.harness.pms.sdk.core.execution.expression.SdkFunctor;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.CDC)
public class ImagePullSecretFunctor implements SdkFunctor {
  public static final String IMAGE_PULL_SECRET = "imagePullSecret";

  private static final String PRIMARY_ARTIFACT = "primary";
  private static final String SIDECAR_ARTIFACTS = "sidecars";

  @Inject ImagePullSecretUtils imagePullSecretUtils;
  @Inject OutcomeService outcomeService;

  @Override
  public Object get(Ambiance ambiance, String... args) {
    String artifactIdentifier = args[0];
    if (artifactIdentifier.equals(PRIMARY_ARTIFACT)) {
      ArtifactsOutcome artifactsOutcome = fetchArtifactsOutcome(ambiance);
      if (artifactsOutcome == null || artifactsOutcome.getPrimary() == null) {
        return null;
      }
      return imagePullSecretUtils.getImagePullSecret(artifactsOutcome.getPrimary(), ambiance);
    } else if (artifactIdentifier.equals(SIDECAR_ARTIFACTS)) {
      return fetchArtifactsOutcome(ambiance).getSidecars();
    } else {
      return null;
    }
  }

  private ArtifactsOutcome fetchArtifactsOutcome(Ambiance ambiance) {
    return (ArtifactsOutcome) outcomeService.resolve(ambiance, RefObjectUtils.getOutcomeRefObject("artifacts"));
  }
}
