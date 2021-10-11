package io.harness.pms.instrumentaion;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.failure.FailureData;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.execution.utils.EngineExceptionUtils;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class PipelineInstrumentationUtils {
  public String getIdentityFromAmbiance(Ambiance ambiance) {
    if (!ambiance.getMetadata().getTriggerInfo().getTriggeredBy().getExtraInfoMap().get("email").isEmpty()) {
      return ambiance.getMetadata().getTriggerInfo().getTriggeredBy().getExtraInfoMap().get("email");
    }
    return ambiance.getMetadata().getTriggerInfo().getTriggeredBy().getIdentifier();
  }

  public Collection<io.harness.exception.FailureType> getFailureTypesFromNodeExecution(NodeExecution nodeExecution) {
    List<FailureType> failureTypes = new ArrayList<>();
    for (FailureData failureData : nodeExecution.getFailureInfo().getFailureDataList()) {
      failureTypes.addAll(failureData.getFailureTypesList());
    }
    return EngineExceptionUtils.transformToWingsFailureTypes(failureTypes);
  }

  public List<String> getErrorMessagesFromNodeExecution(NodeExecution nodeExecution) {
    return nodeExecution.getFailureInfo()
        .getFailureDataList()
        .stream()
        .map(o -> o.getMessage())
        .collect(Collectors.toList());
  }

  public List<String> getErrorMessagesFromPipelineExecutionSummary(
      PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity) {
    return pipelineExecutionSummaryEntity.getFailureInfo()
        .getResponseMessages()
        .stream()
        .map(o -> o.getMessage())
        .collect(Collectors.toList());
  }
}
