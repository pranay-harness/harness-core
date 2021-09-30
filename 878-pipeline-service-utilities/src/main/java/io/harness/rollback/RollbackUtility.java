package io.harness.rollback;

import static io.harness.pms.yaml.YAMLFieldNameConstants.FAILED_CHILDREN_OUTPUT;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepResponseNotifyData;
import io.harness.steps.SectionStepSweepingOutput;
import io.harness.steps.SectionStepSweepingOutput.SectionStepSweepingOutputBuilder;
import io.harness.tasks.ResponseData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class RollbackUtility {
  public void publishRollbackInformation(Ambiance ambiance, Map<String, ResponseData> responseDataMap,
      ExecutionSweepingOutputService executionSweepingOutputService) {
    SectionStepSweepingOutputBuilder builder = SectionStepSweepingOutput.builder();
    List<String> failedNodeIds = new ArrayList<>();
    for (ResponseData responseData : responseDataMap.values()) {
      StepResponseNotifyData responseNotifyData = (StepResponseNotifyData) responseData;
      Status executionStatus = responseNotifyData.getStatus();
      if (StatusUtils.brokeStatuses().contains(executionStatus)) {
        failedNodeIds.add(responseNotifyData.getNodeUuid());
      }
    }
    List<OptionalSweepingOutput> onFailRollbackOptionalOutput =
        executionSweepingOutputService.listOutputsWithGivenNameAndSetupIds(
            ambiance, FAILED_CHILDREN_OUTPUT, failedNodeIds);
    if (onFailRollbackOptionalOutput.isEmpty() && !failedNodeIds.isEmpty()) {
      builder.failedNodeIds(failedNodeIds);
    } else {
      for (int i = 0; i < onFailRollbackOptionalOutput.size(); i++) {
        OptionalSweepingOutput optionalSweepingOutput = onFailRollbackOptionalOutput.get(i);
        String nodeUuid = failedNodeIds.get(i);
        if (optionalSweepingOutput.isFound()) {
          SectionStepSweepingOutput sectionStepSweepingOutput =
              (SectionStepSweepingOutput) optionalSweepingOutput.getOutput();
          for (String failedNode : sectionStepSweepingOutput.getFailedNodeIds()) {
            builder.failedNodeId(failedNode);
          }
        } else {
          builder.failedNodeId(nodeUuid);
        }
      }
    }
    SectionStepSweepingOutput output = builder.build();
    if (!output.getFailedNodeIds().isEmpty()) {
      executionSweepingOutputService.consume(ambiance, FAILED_CHILDREN_OUTPUT, builder.build(), null);
    }
  }
}
