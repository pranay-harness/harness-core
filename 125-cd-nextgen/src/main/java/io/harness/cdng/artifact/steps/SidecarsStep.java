package io.harness.cdng.artifact.steps;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.service.steps.ServiceStepsHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.ngpipeline.artifact.bean.ArtifactOutcome;
import io.harness.ngpipeline.artifact.bean.SidecarsOutcome;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepOutcome;
import io.harness.steps.common.NGForkStep;
import io.harness.steps.fork.ForkStepParameters;
import io.harness.tasks.ResponseData;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class SidecarsStep extends NGForkStep {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.SIDECARS.getName()).setStepCategory(StepCategory.STEP).build();

  @Inject private ServiceStepsHelper serviceStepsHelper;

  @Override
  public StepResponse handleChildrenResponse(
      Ambiance ambiance, ForkStepParameters stepParameters, Map<String, ResponseData> responseDataMap) {
    List<Outcome> outcomes = serviceStepsHelper.getChildrenOutcomes(responseDataMap);
    io.harness.ngpipeline.artifact.bean.SidecarsOutcome sidecarsOutcome = new SidecarsOutcome();
    if (EmptyPredicate.isNotEmpty(outcomes)) {
      for (Outcome outcome : outcomes) {
        if (!(outcome instanceof ArtifactOutcome)) {
          continue;
        }

        ArtifactOutcome artifactOutcome = (ArtifactOutcome) outcome;
        sidecarsOutcome.put(artifactOutcome.getIdentifier(), artifactOutcome);
      }
    }

    StepResponse stepResponse = super.handleChildrenResponse(ambiance, stepParameters, responseDataMap);
    return stepResponse.withStepOutcomes(
        Collections.singleton(StepOutcome.builder().name("output").outcome(sidecarsOutcome).build()));
  }
}
