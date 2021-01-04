package io.harness.pms.sdk;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.engine.StepTypeLookupService;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.steps.StepType;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;

public class StepTypeLookupServiceImpl implements StepTypeLookupService {
  @Inject private PmsSdkInstanceService pmsSdkInstanceService;

  @Override
  public String findNodeExecutionServiceName(NodeExecutionProto nodeExecution) {
    Map<String, List<StepType>> map = pmsSdkInstanceService.getInstanceNameToSupportedStepTypes();
    if (isEmpty(map)) {
      throw new InvalidRequestException("Step type map is empty");
    }
    StepType stepType = nodeExecution.getNode().getStepType();
    for (Map.Entry<String, List<StepType>> entry : map.entrySet()) {
      List<StepType> stepTypes = entry.getValue();
      if (isEmpty(stepTypes)) {
        continue;
      }

      if (stepTypes.stream().anyMatch(st -> st.getType().equals(stepType.getType()))) {
        return entry.getKey();
      }
    }

    throw new InvalidRequestException("Unknown step type: " + stepType.getType());
  }
}
