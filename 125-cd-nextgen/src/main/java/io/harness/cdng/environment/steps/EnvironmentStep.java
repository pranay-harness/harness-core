package io.harness.cdng.environment.steps;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.cdng.environment.EnvironmentMapper;
import io.harness.cdng.environment.EnvironmentOutcome;
import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.cdng.pipeline.steps.NGStepTypes;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.common.AmbianceHelper;
import io.harness.execution.status.Status;
import io.harness.executionplan.plancreator.beans.StepOutcomeGroup;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepResponse;

public class EnvironmentStep implements Step, SyncExecutable<EnvironmentStepParameters> {
  public static final StepType STEP_TYPE = StepType.builder().type(NGStepTypes.ENVIRONMENT).build();
  @Inject private EnvironmentService environmentService;

  @Override
  public StepResponse executeSync(Ambiance ambiance, EnvironmentStepParameters environmentStepParameters,
      StepInputPackage inputPackage, PassThroughData passThroughData) {
    EnvironmentYaml environmentYaml = environmentStepParameters.getEnvironmentOverrides() != null
        ? environmentStepParameters.getEnvironment().applyOverrides(environmentStepParameters.getEnvironmentOverrides())
        : environmentStepParameters.getEnvironment();
    Environment environment = getEnvironmentObject(environmentYaml, ambiance);
    environmentService.upsert(environment);
    EnvironmentOutcome environmentOutcome = EnvironmentMapper.toOutcome(environmentYaml);
    return StepResponse.builder()
        .status(Status.SUCCEEDED)
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name(OutcomeExpressionConstants.ENVIRONMENT)
                         .group(StepOutcomeGroup.STAGE.name())
                         .outcome(environmentOutcome)
                         .build())
        .build();
  }

  private Environment getEnvironmentObject(EnvironmentYaml environmentYaml, Ambiance ambiance) {
    String accountId = AmbianceHelper.getAccountId(ambiance);
    String projectIdentifier = AmbianceHelper.getProjectIdentifier(ambiance);
    String orgIdentifier = AmbianceHelper.getOrgIdentifier(ambiance);

    return Environment.builder()
        .name(environmentYaml.getName().getValue())
        .accountId(accountId)
        .type(environmentYaml.getType())
        .identifier(environmentYaml.getIdentifier().getValue())
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .build();
  }
}
