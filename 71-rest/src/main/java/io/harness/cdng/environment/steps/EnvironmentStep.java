package io.harness.cdng.environment.steps;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
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
    EnvironmentYaml environmentYaml = (EnvironmentYaml) stepParameters;
    renderExpressions(environmentYaml);
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
    String accountId = ambiance.getSetupAbstractions().get("accountId");
    String projectId = ambiance.getSetupAbstractions().get("projectId");
    String orgId = ambiance.getSetupAbstractions().get("orgId");

    return Environment.builder()
        .displayName(environmentYaml.getDisplayName())
        .accountId(accountId)
        .environmentType(environmentYaml.getType())
        .identifier(environmentYaml.getIdentifier())
        .orgId(orgId)
        .projectId(projectId)
        .tags(environmentYaml.getTags())
        .build();
  }

  private void renderExpressions(EnvironmentYaml environmentYaml) {
    // Todo: render  later
  }
}
