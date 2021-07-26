package io.harness.cdng.manifest.steps;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.service.steps.ServiceStepsHelper;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.data.structure.EmptyPredicate;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.steps.common.NGForkStep;
import io.harness.steps.fork.ForkStepParameters;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class ManifestsStep extends NGForkStep {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.MANIFESTS.getName()).setStepCategory(StepCategory.STEP).build();

  @Inject private ServiceStepsHelper serviceStepsHelper;

  @Override
  public StepResponse handleChildrenResponse(
      Ambiance ambiance, ForkStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    List<Outcome> outcomes = serviceStepsHelper.getChildrenOutcomes(responseDataMap);
    ManifestsOutcome manifestsOutcome = new ManifestsOutcome();
    if (EmptyPredicate.isNotEmpty(outcomes)) {
      for (Outcome outcome : outcomes) {
        if (!(outcome instanceof ManifestOutcome)) {
          continue;
        }

        ManifestOutcome manifestOutcome = (ManifestOutcome) outcome;
        manifestsOutcome.put(manifestOutcome.getIdentifier(), manifestOutcome);
      }
    }

    StepResponse stepResponse = super.handleChildrenResponse(ambiance, stepParameters, responseDataMap);
    return stepResponse.withStepOutcomes(Collections.singleton(StepResponse.StepOutcome.builder()
                                                                   .name(OutcomeExpressionConstants.MANIFESTS)
                                                                   .outcome(manifestsOutcome)
                                                                   .group(StepOutcomeGroup.STAGE.name())
                                                                   .build()));
  }
}
