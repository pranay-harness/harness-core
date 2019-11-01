package software.wings.service.intfc;

import io.harness.beans.SweepingOutput;
import lombok.Builder;
import lombok.Value;

import javax.validation.Valid;

public interface SweepingOutputService {
  SweepingOutput save(@Valid SweepingOutput sweepingOutput);

  void ensure(@Valid SweepingOutput sweepingOutput);

  @Value
  @Builder
  class SweepingOutputInquiry {
    String appId;
    String name;
    String pipelineExecutionId;
    String workflowExecutionId;
    String phaseExecutionId;
    String stateExecutionId;
  }

  SweepingOutput find(SweepingOutputInquiry inquiry);

  void copyOutputsForAnotherWorkflowExecution(
      String appId, String fromWorkflowExecutionId, String toWorkflowExecutionId);
}
