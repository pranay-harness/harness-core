package io.harness.cdng.common.step;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.ng.core.environment.beans.EnvironmentType.Production;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EnvironmentType;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(CDP)
@Singleton
public class StepHelper {
  @Inject private OutcomeService outcomeService;

  public EnvironmentType getEnvironmentType(Ambiance ambiance) {
    InfrastructureOutcome infrastructureOutcome = (InfrastructureOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME));

    if (infrastructureOutcome == null || infrastructureOutcome.getEnvironment() == null
        || infrastructureOutcome.getEnvironment().getType() == null) {
      return EnvironmentType.ALL;
    }

    return Production == infrastructureOutcome.getEnvironment().getType() ? EnvironmentType.PROD
                                                                          : EnvironmentType.NON_PROD;
  }
}
