package io.harness.cdng.infra.steps;

import io.harness.ambiance.Ambiance;
import io.harness.cdng.infra.beans.InfraDefinition;
import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.cdng.service.Service;
import io.harness.execution.status.Status;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.state.Step;
import io.harness.state.StepType;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponse.StepOutcome;
import io.harness.state.io.StepTransput;
import io.harness.utils.Utils;
import io.harness.validation.Validator;

import java.util.List;

public class InfrastructureStep implements Step, SyncExecutable {
  public static final StepType STEP_TYPE = StepType.builder().type("INFRASTRUCTURE").build();

  InfraMapping createInfraMappingObject(String serviceIdentifier, InfraDefinition infraDefinition) {
    InfraMapping infraMapping = infraDefinition.getInfraMapping();
    infraMapping.setServiceIdentifier(serviceIdentifier);
    return infraMapping;
  }

  @Override
  public StepResponse executeSync(
      Ambiance ambiance, StepParameters stepParameters, List<StepTransput> inputs, PassThroughData passThroughData) {
    Service service = Utils.getFirstInstance(inputs, Service.class);
    Validator.notNullCheck("Service not found", service);

    InfraDefinition infraDefinition = Utils.getFirstInstance(inputs, InfraDefinition.class);
    Validator.notNullCheck("Infrastructure Definition not found", infraDefinition);

    InfraMapping infraMapping = createInfraMappingObject(service.getIdentifier(), infraDefinition);

    return StepResponse.builder()
        .status(Status.SUCCEEDED)
        .stepOutcome(StepOutcome.builder().outcome(infraMapping).name("infrastructureMapping").build())
        .build();
  }
}
