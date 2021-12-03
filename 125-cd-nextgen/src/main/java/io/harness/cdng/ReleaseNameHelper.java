package io.harness.cdng;

import static io.harness.cdng.infra.yaml.InfrastructureKind.KUBERNETES_DIRECT;
import static io.harness.cdng.infra.yaml.InfrastructureKind.KUBERNETES_GCP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.lang.String.format;

import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sDirectInfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sGcpInfrastructureOutcome;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.yaml.validation.ExpressionUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.Pair;

@Singleton
public class ReleaseNameHelper {
  @Inject private EngineExpressionService engineExpressionService;

  public static final String RELEASE_NAME_VALIDATION_REGEX =
      "[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*";

  public static final Pattern releaseNamePattern = Pattern.compile(RELEASE_NAME_VALIDATION_REGEX);

  public String getReleaseName(Ambiance ambiance, InfrastructureOutcome infrastructure) {
    String releaseName;
    switch (infrastructure.getKind()) {
      case KUBERNETES_DIRECT:
        K8sDirectInfrastructureOutcome k8SDirectInfrastructure = (K8sDirectInfrastructureOutcome) infrastructure;
        releaseName = k8SDirectInfrastructure.getReleaseName();
        break;
      case KUBERNETES_GCP:
        K8sGcpInfrastructureOutcome k8sGcpInfrastructure = (K8sGcpInfrastructureOutcome) infrastructure;
        releaseName = k8sGcpInfrastructure.getReleaseName();
        break;
      default:
        throw new UnsupportedOperationException(format("Unknown infrastructure type: [%s]", infrastructure.getKind()));
    }
    if (EngineExpressionEvaluator.hasExpressions(releaseName)) {
      releaseName = engineExpressionService.renderExpression(ambiance, releaseName);
    }

    validateReleaseName(releaseName);
    return releaseName;
  }

  private static void validateReleaseName(String name) {
    if (isEmpty(name)) {
      throw new InvalidArgumentsException(Pair.of("releaseName", "Cannot be empty"));
    }

    if (!ExpressionUtils.matchesPattern(releaseNamePattern, name)) {
      throw new InvalidRequestException(format(
          "Invalid Release name format: %s. Release name must consist of lower case alphanumeric characters, '-' or '.'"
              + ", and must start and end with an alphanumeric character (e.g. 'example.com')",
          name));
    }
  }
}
