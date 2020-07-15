package io.harness.cdng.environment.steps;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.cdng.common.AmbianceHelper;
import io.harness.cdng.environment.EnvironmentService;
import io.harness.cdng.environment.beans.Environment;
import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.execution.status.Status;
import io.harness.executionplan.plancreator.beans.StepGroup;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;

public class EnvironmentStep implements Step, SyncExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type("ENVIRONMENT").build();

  @Inject private EnvironmentService environmentService;

  @Override
  public StepResponse executeSync(Ambiance ambiance, StepParameters stepParameters, StepInputPackage inputPackage,
      PassThroughData passThroughData) {
    EnvironmentStepParameters environmentStepParameters = (EnvironmentStepParameters) stepParameters;
    EnvironmentYaml environmentYaml = environmentStepParameters.getEnvironmentOverrides() != null
        ? environmentStepParameters.getEnvironment().applyOverrides(environmentStepParameters.getEnvironmentOverrides())
        : environmentStepParameters.getEnvironment();
    Environment environment = getEnvironmentObject(environmentYaml, ambiance);
    environmentService.upsert(environment);
    return StepResponse.builder()
        .status(Status.SUCCEEDED)
        .stepOutcome(StepResponse.StepOutcome.builder()
                         .name("environment")
                         .group(StepGroup.STAGE.name())
                         .outcome(environmentYaml)
                         .build())
        .build();
  }

  private Environment getEnvironmentObject(EnvironmentYaml environmentYaml, Ambiance ambiance) {
    String accountId = AmbianceHelper.getAccountId(ambiance);
    String projectId = AmbianceHelper.getProjectIdentifier(ambiance);
    String orgId = AmbianceHelper.getOrgIdentifier(ambiance);

    return Environment.builder()
        .displayName(environmentYaml.getName())
        .accountId(accountId)
        .environmentType(environmentYaml.getType())
        .identifier(environmentYaml.getIdentifier())
        .orgId(orgId)
        .projectId(projectId)
        .tags(environmentYaml.getTags())
        .build();
  }
}
