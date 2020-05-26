package software.wings.cdng.infra.states;

import io.harness.ambiance.Ambiance;
import io.harness.execution.status.NodeExecutionStatus;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.sync.SyncExecutable;
import io.harness.state.StepType;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepTransput;
import io.harness.utils.Utils;
import io.harness.validation.Validator;
import software.wings.cdng.infra.beans.InfraDefinition;
import software.wings.cdng.infra.beans.InfraMapping;
import software.wings.cdng.service.Service;

import java.util.List;

public class InfrastructureState implements io.harness.state.Step, SyncExecutable {
  InfraMapping createInfraMappingObject(String serviceName, InfraDefinition infraDefinition) {
    InfraMapping infraMapping = infraDefinition.getInfraMapping();
    infraMapping.setServiceName(serviceName);
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
        .status(NodeExecutionStatus.SUCCEEDED)
        .outcome("infrastructureMapping", infraMapping)
        .build();
  }

  @Override
  public StepType getType() {
    return StepType.builder().type("INFRASTRUCTURE").build();
  }
}
